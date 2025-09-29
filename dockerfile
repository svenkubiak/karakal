FROM openjdk:25-jdk
WORKDIR /app
COPY target/karakal.jar karakal.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/karakal.jar"]