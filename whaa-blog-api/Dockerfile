FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY whaa-blog-common/pom.xml whaa-blog-common/pom.xml
COPY whaa-blog-domain/pom.xml whaa-blog-domain/pom.xml
COPY whaa-blog-infrastructure/pom.xml whaa-blog-infrastructure/pom.xml
COPY whaa-blog-application/pom.xml whaa-blog-application/pom.xml
COPY whaa-blog-api/pom.xml whaa-blog-api/pom.xml

COPY whaa-blog-common/src whaa-blog-common/src
COPY whaa-blog-domain/src whaa-blog-domain/src
COPY whaa-blog-infrastructure/src whaa-blog-infrastructure/src
COPY whaa-blog-application/src whaa-blog-application/src
COPY whaa-blog-api/src whaa-blog-api/src

RUN mvn -B -pl whaa-blog-api -am package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /workspace/whaa-blog-api/target/whaa-blog-api-*.jar /app/app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
