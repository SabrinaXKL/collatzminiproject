# Use a lightweight Java runtime
FROM openjdk:11-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the assembly JAR into the container
COPY target/scala-3.3.7/collatzminiproject-assembly-0.2.jar app.jar

# Command to run the app
CMD ["java", "-jar", "app.jar"]