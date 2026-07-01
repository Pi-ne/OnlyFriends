# Gateway 默认 Dockerfile（目标目录填 backend 时使用）

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY onlyfriends-gateway/pom.xml onlyfriends-gateway/
COPY onlyfriends-gateway/src onlyfriends-gateway/src

RUN mvn -pl onlyfriends-gateway package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/onlyfriends-gateway/target/onlyfriends-gateway-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
