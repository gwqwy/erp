# 构建镜像（在仓库根目录执行）：
#   docker build -f deploy/Dockerfile -t tiangong/server:dev .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY tiangong-erp /build
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/tiangong-server/target/tiangong-server-*.jar app.jar
EXPOSE 48080
ENTRYPOINT ["java", "-jar", "app.jar"]
