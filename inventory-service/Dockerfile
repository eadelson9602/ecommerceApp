FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build
COPY . .
RUN ./mvnw package -B -pl inventory-service -am -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/inventory-service/target/inventory-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
