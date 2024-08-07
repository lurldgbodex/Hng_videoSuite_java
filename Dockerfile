FROM eclipse-temurin:17-jre-alpine as build
LABEL authors="seguser"
WORKDIR /workspace/app

RUN mkdir -p target/extracted

ADD target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination target/extracted

FROM eclipse-temurin:17-jre-alpine
WORKDIR /workspace/app
VOLUME /tmp
ARG EXTRACTED=/workspace/app/target/extracted
COPY --from=build ${EXTRACTED}/dependencies/ ./
COPY --from=build ${EXTRACTED}/spring-boot-loader/ ./
COPY --from=build ${EXTRACTED}/snapshot-dependencies/ ./
COPY --from=build ${EXTRACTED}/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]