FROM openjdk:11-oracle
#RUN addgroup -S spring && adduser -S spring -G spring
#RUN mkdir target && chown -R spring:spring target/
#USER spring:spring
ARG JAR_FILE=target/archivo-consistency-mod-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
COPY application.yml application.yml
ENTRYPOINT ["java","-jar","/app.jar"]