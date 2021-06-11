FROM openjdk:8-jdk-alpine
ARG JAR_FILE=target/*-fat.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8088
ENTRYPOINT ["java","-jar","/app.jar"]