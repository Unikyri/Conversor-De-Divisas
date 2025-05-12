package com.conversor.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP para realizar solicitudes a APIs externas utilizando HttpClient moderno
 */
public class ClienteHttpRequest {
    
    private final HttpClient cliente;
    
    public ClienteHttpRequest() {
        this.cliente = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * Realiza una solicitud GET a la URL especificada
     * @param urlStr URL a la cual realizar la solicitud
     * @param apiKey Clave de API si es necesaria (puede ser null)
     * @param headerName Nombre del encabezado para la API key
     * @return Respuesta del servidor en formato String
     * @throws IOException Si ocurre un error de conexión
     * @throws InterruptedException Si la solicitud es interrumpida
     */
    public String get(String urlStr, String apiKey, String headerName) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30));
        
        if (apiKey != null && headerName != null) {
            requestBuilder.header(headerName, apiKey);
        }
        
        HttpRequest request = requestBuilder.build();
        
        HttpResponse<String> response = cliente.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Error HTTP: " + response.statusCode());
        }
        
        return response.body();
    }
    
    /**
     * Realiza una solicitud GET asíncrona a la URL especificada
     * @param urlStr URL a la cual realizar la solicitud
     * @param apiKey Clave de API si es necesaria (puede ser null)
     * @param headerName Nombre del encabezado para la API key
     * @return CompletableFuture con la respuesta del servidor
     */
    public java.util.concurrent.CompletableFuture<String> getAsync(String urlStr, String apiKey, String headerName) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30));
        
        if (apiKey != null && headerName != null) {
            requestBuilder.header(headerName, apiKey);
        }
        
        HttpRequest request = requestBuilder.build();
        
        return cliente.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Error HTTP: " + response.statusCode());
                    }
                    return response.body();
                });
    }
} 