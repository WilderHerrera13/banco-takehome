FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./pom.xml
COPY shared ./shared
COPY ms1-person-client ./ms1-person-client
COPY ms2-account-movement ./ms2-account-movement
RUN mvn -q -pl shared,ms1-person-client -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /build/ms1-person-client/target/ms1-person-client-1.0.0.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
