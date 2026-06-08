FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 复制整个项目（最简单，最稳定，多模块必备）
COPY . .

# 全量编译：父工程 → common → gateway（自动按顺序）
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]