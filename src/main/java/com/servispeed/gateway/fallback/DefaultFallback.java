package com.servispeed.gateway.fallback;

import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class DefaultFallback implements FallbackProvider {

    @Override
    public String getRoute() {
        return "*"; // Aplica para absolutamente todas las rutas configuradas
    }

    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
        return new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() throws IOException {
                return HttpStatus.SERVICE_UNAVAILABLE; // Código 503
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return HttpStatus.SERVICE_UNAVAILABLE.value();
            }

            @Override
            public String getStatusText() throws IOException {
                return HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase();
            }

            @Override
            public void close() {
                // No se requieren recursos para cerrar en este fallback estático
            }

            @Override
            public InputStream getBody() throws IOException {
                String mensajeError = String.format(
                    "{\"error\": \"Servicio temporalmente no disponible\", \"route\": \"%s\", \"detalles\": \"Comprueba que el microservicio esté encendido.\"}", 
                    route
                );
                return new ByteArrayInputStream(mensajeError.getBytes());
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }
}