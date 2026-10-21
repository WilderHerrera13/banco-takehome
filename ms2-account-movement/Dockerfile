FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./pom.xml
COPY shared ./shared
COPY ms2-account-movement ./ms2-account-movement
COPY ms1-person-client ./ms1-person-client
RUN mvn -q -pl shared,ms2-account-movement -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /build/ms2-account-movement/target/ms2-account-movement-1.0.0.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
