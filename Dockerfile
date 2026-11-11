# 仓库根目录构建入口：Railway 默认从根目录构建，而 Java 项目在 multi-agent-vocab/ 子目录
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY multi-agent-vocab/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY multi-agent-vocab/src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
