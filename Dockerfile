# Cambia la línea de FROM por esta (Java 11 sobre una base ligera de Ubuntu)
FROM eclipse-temurin:11-jre-focal

# El resto del archivo se queda exactamente igual:
WORKDIR /app

COPY target/api-gateway-1.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]