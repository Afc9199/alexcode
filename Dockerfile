# 使用官方 OpenJDK 21 镜像作为基础镜像
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制构建好的 JAR 文件
# 注意：需要先运行 mvn clean package 生成 JAR 文件
COPY target/employee-management-0.0.1-SNAPSHOT.jar app.jar

# 创建上传文件目录
RUN mkdir -p /app/uploads/kpi-evidence /app/uploads/leave-documents /app/uploads/resumes

# 暴露端口（Cloud Run 会通过 PORT 环境变量设置，默认 8080）
EXPOSE 8080

# 设置 JVM 参数优化 Cloud Run 环境
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# 启动应用
# Cloud Run 会设置 PORT 环境变量，Spring Boot 会自动读取
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]



