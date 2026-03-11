# Stage 1: cache Maven dependencies (solo POMs)
FROM eclipse-temurin:17-jdk-alpine AS deps
WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY ecommerce-jsonapi-model/pom.xml ecommerce-jsonapi-model/
COPY products-service/pom.xml products-service/
COPY inventory-service/pom.xml inventory-service/
RUN ./mvnw dependency:go-offline -B -pl inventory-service -am -q

# Stage 2: build (reusa .m2 del stage deps)
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build
COPY --from=deps /root/.m2 /root/.m2
COPY . .
RUN ./mvnw package -B -pl inventory-service -am -DskipTests -Djacoco.skip=true -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/inventory-service/target/inventory-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
