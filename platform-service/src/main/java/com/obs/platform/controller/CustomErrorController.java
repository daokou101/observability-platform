package com.obs.platform.controller;

import com.obs.platform.common.api.Result;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Result<Void>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = HttpStatus.resolve(statusCode != null ? statusCode : 500);

        if (status == HttpStatus.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.failed(40005, "接口不存在"));
        }

        return ResponseEntity.status(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failed(statusCode != null ? statusCode : 500, "请求失败"));
    }
}
