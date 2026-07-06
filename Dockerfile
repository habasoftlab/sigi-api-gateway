FROM eclipse-temurin:11-jre-focal

WORKDIR /app

# Descargamos los parches de JAXB directo al contenedor usando curl
RUN apt-get update && apt-get install -y curl && \
    curl -sO https://repo1.maven.org/maven2/javax/xml/bind/jaxb-api/2.3.1/jaxb-api-2.3.1.jar && \
    curl -sO https://repo1.maven.org/maven2/org/glassfish/jaxb/jaxb-runtime/2.3.1/jaxb-runtime-2.3.1.jar && \
    curl -sO https://repo1.maven.org/maven2/com/sun/xml/bind/jaxb-impl/2.3.1/jaxb-impl-2.3.1.jar && \
    curl -sO https://repo1.maven.org/maven2/com/sun/xml/bind/jaxb-core/2.3.1/jaxb-core-2.3.1.jar && \
    curl -sO https://repo1.maven.org/maven2/javax/activation/javax.activation-api/1.2.0/javax.activation-api-1.2.0.jar

# Copiamos tu microservicio
COPY target/api-gateway-1.jar app.jar

EXPOSE 8088

# Arrancamos inyectando de forma explícita las librerías descargadas al Classpath de Java
ENTRYPOINT ["java", "-cp", "app.jar:jaxb-api-2.3.1.jar:jaxb-runtime-2.3.1.jar:jaxb-impl-2.3.1.jar:jaxb-core-2.3.1.jar:javax.activation-api-1.2.0.jar", "org.springframework.boot.loader.JarLauncher"]