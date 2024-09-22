FROM maven:3.8.6-eclipse-temurin-17 as builder
WORKDIR /opt/app
COPY mvnw pom.xml ./
COPY ./src ./src
RUN mvn clean install -DskipTests


FROM eclipse-temurin:17-jre-jammy
WORKDIR /opt/app
EXPOSE 8092
COPY --from=builder /opt/app/target/*.jar /opt/app/papa_blinov.jar
ENTRYPOINT ["java", "-jar", "/opt/app/papa_blinov.jar", "--server.port=8092"]