#FROM openjdk:21-jdk-slim
#
#WORKDIR /app
#
## Install netcat for wait-for-it.sh
#RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*
#
#COPY . .
#RUN chmod +x mvnw wait-for-it.sh
#RUN ./mvnw clean package -DskipTests
#
#EXPOSE 8080
#ENTRYPOINT ["./wait-for-it.sh", "db:3306", "--", "java", "-jar", "target/carry_guide_admin-0.0.1-SNAPSHOT.jar"]
# backend/Dockerfile
# Use OpenJDK base image
# backend/Dockerfile
FROM eclipse-temurin:21-jdk-alpine

RUN apk update && apk add maven netcat-openbsd

WORKDIR /app

COPY . .

WORKDIR /app/backend
RUN mvn clean package -DskipTests

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/carry_guide_admin-0.0.1-SNAPSHOT.jar"]

