package com.obs.platform.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Service
public class StressTestService {

    private final ConcurrentHashMap<String, TestRunner> activeTests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> completedReports = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Object> history = new ConcurrentLinkedQueue<>();
    private static final int HISTORY_MAX = 20;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String start(Map<String, Object> config) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String url = (String) config.getOrDefault("url", "http://localhost:8080/api/demo/hello");
        String method = (String) config.getOrDefault("method", "GET");
        int qps = ((Number) config.getOrDefault("qps", 10)).intValue();
        int durationSeconds = ((Number) config.getOrDefault("durationSeconds", 30)).intValue();

        TestRunner runner = new TestRunner(taskId, url, method, qps, durationSeconds);
        activeTests.put(taskId, runner);
        scheduler.submit(runner);
        log.info("[StressTest] 启动压测 taskId={}, url={}, qps={}, duration={}s", taskId, url, qps, durationSeconds);
        return taskId;
    }

    public void stop(String taskId) {
        TestRunner runner = activeTests.get(taskId);
        if (runner != null) {
            runner.stop();
            log.info("[StressTest] 手动停止压测 taskId={}", taskId);
        }
    }

    public SseEmitter subscribe(String taskId) {
        TestRunner runner = activeTests.get(taskId);
        if (runner != null) {
            SseEmitter emitter = new SseEmitter(600_000L);
            runner.addEmitter(emitter);
            return emitter;
        }
        // 已完成的任务：发送完成信号后立即结束
        SseEmitter done = new SseEmitter(30000L);
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("running", false);
            msg.put("finished", true);
            done.send(SseEmitter.event().name("message").data(msg));
        } catch (IOException ignored) {}
        done.complete();
        return done;
    }

    public List<Object> getHistory() {
        return new ArrayList<>(history);
    }

    @PreDestroy
    public void cleanup() {
        activeTests.values().forEach(TestRunner::stop);
    }

    class TestRunner implements Runnable {
        private final String taskId;
        private final String url;
        private final String method;
        private final int targetQps;
        private final int durationSeconds;
        private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

        private volatile boolean running = true;

        // 每秒统计（每次 report 后重置）
        private final AtomicLong secondCount = new AtomicLong();
        private final AtomicLong secondSuccess = new AtomicLong();
        private final AtomicLong secondError = new AtomicLong();
        private final List<Long> secondDurations = Collections.synchronizedList(new ArrayList<>());

        // 累计
        private final AtomicLong totalRequests = new AtomicLong();
        private final AtomicLong totalSuccess = new AtomicLong();
        private final AtomicLong totalErrors = new AtomicLong();

        TestRunner(String taskId, String url, String method, int targetQps, int durationSeconds) {
            this.taskId = taskId;
            this.url = url;
            this.method = method;
            this.targetQps = targetQps;
            this.durationSeconds = durationSeconds;
        }

        void addEmitter(SseEmitter emitter) {
            emitters.add(emitter);
            emitter.onCompletion(() -> emitters.remove(emitter));
            emitter.onTimeout(() -> emitters.remove(emitter));
            emitter.onError(e -> emitters.remove(emitter));
        }

        @Override
        public void run() {
            long endTime = System.currentTimeMillis() + durationSeconds * 1000L;
            int concurrency = Math.min(targetQps, 50); // 最多 50 并发

            // 每秒报告一次
            ScheduledExecutorService reporter = Executors.newScheduledThreadPool(1);
            reporter.scheduleAtFixedRate(this::report, 1, 1, TimeUnit.SECONDS);

            // 启动并发请求线程
            List<Thread> workers = new ArrayList<>();
            for (int i = 0; i < concurrency; i++) {
                Thread worker = new Thread(() -> {
                    while (running && System.currentTimeMillis() < endTime) {
                        long start = System.nanoTime();
                        try {
                            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                                    .uri(URI.create(url))
                                    .timeout(Duration.ofSeconds(30));
                            if ("GET".equalsIgnoreCase(method)) {
                                reqBuilder.GET();
                            } else {
                                reqBuilder.POST(HttpRequest.BodyPublishers.noBody());
                            }
                            HttpResponse<String> resp = httpClient.send(reqBuilder.build(),
                                    HttpResponse.BodyHandlers.ofString());

                            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                            secondCount.incrementAndGet();
                            secondDurations.add(elapsedMs);

                            if (resp.statusCode() >= 400) {
                                secondError.incrementAndGet();
                            } else {
                                secondSuccess.incrementAndGet();
                            }
                        } catch (Exception e) {
                            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                            secondCount.incrementAndGet();
                            secondError.incrementAndGet();
                            secondDurations.add(elapsedMs);
                        }

                        // 节流
                        if (concurrency > 0) {
                            long perThreadNs = 1_000_000_000L / (targetQps / concurrency);
                            long took = System.nanoTime() - start;
                            long wait = perThreadNs - took;
                            if (wait > 0) {
                                LockSupport.parkNanos(wait);
                            }
                        }
                    }
                }, "stress-worker-" + taskId + "-" + i);
                worker.setDaemon(true);
                worker.start();
                workers.add(worker);
            }

            // 等待所有工作线程结束
            for (Thread w : workers) {
                try { w.join(); } catch (InterruptedException ignored) { break; }
            }
            running = false;
            reporter.shutdown();

            // 先存结果，再发最终报告（避免前端查历史时数据未写入）
            storeResult();
            report();
            activeTests.remove(taskId);
            log.info("[StressTest] 压测结束 taskId={}, 总请求={}", taskId, totalRequests.get());
        }

        void stop() {
            running = false;
        }

        void report() {
            long count = secondCount.getAndSet(0);
            long success = secondSuccess.getAndSet(0);
            long errors = secondError.getAndSet(0);

            List<Long> durations;
            synchronized (secondDurations) {
                durations = new ArrayList<>(secondDurations);
                secondDurations.clear();
            }

            if (count == 0) return;

            totalRequests.addAndGet(count);
            totalSuccess.addAndGet(success);
            totalErrors.addAndGet(errors);

            durations.sort(Long::compareTo);
            double p50 = durations.get((int) (durations.size() * 0.5));
            double p95 = durations.get((int) (durations.size() * 0.95));
            double p99 = durations.get((int) (durations.size() * 0.99));
            double avg = durations.stream().mapToLong(Long::longValue).average().orElse(0);

            long total = totalRequests.get();
            long totalSuc = totalSuccess.get();
            long totalErr = totalErrors.get();

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("taskId", taskId);
            snapshot.put("running", running);
            snapshot.put("secondRequests", count);
            snapshot.put("secondSuccess", success);
            snapshot.put("secondErrors", errors);
            snapshot.put("currentQps", count);
            snapshot.put("p50", String.format("%.1f", p50));
            snapshot.put("p95", String.format("%.1f", p95));
            snapshot.put("p99", String.format("%.1f", p99));
            snapshot.put("avgDuration", String.format("%.1f", avg));
            snapshot.put("totalRequests", total);
            snapshot.put("totalSuccess", totalSuc);
            snapshot.put("totalErrors", totalErr);
            snapshot.put("timestamp", System.currentTimeMillis());

            Iterator<SseEmitter> iter = emitters.iterator();
            while (iter.hasNext()) {
                SseEmitter emitter = iter.next();
                try {
                    emitter.send(snapshot);
                } catch (IOException e) {
                    iter.remove();
                }
            }
        }

        void storeResult() {
            long total = totalRequests.get();
            long suc = totalSuccess.get();
            long err = totalErrors.get();
            if (total == 0) return;

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("taskId", taskId);
            report.put("url", url);
            report.put("method", method);
            report.put("targetQps", targetQps);
            report.put("durationSeconds", durationSeconds);
            report.put("totalRequests", total);
            report.put("successCount", suc);
            report.put("errorCount", err);
            report.put("errorRate", String.format("%.1f%%", err * 100.0 / total));
            report.put("timestamp", System.currentTimeMillis());

            history.add(report);
            completedReports.put(taskId, report);
            if (history.size() > HISTORY_MAX) history.poll();
        }
    }
}
