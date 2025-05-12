package com.conversor.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Cliente para la API de ExchangeRate
 */
@Slf4j
@Component
public class ClienteExchangeRate {
    
    private final String apiKey;
    private static final String URL_BASE = "https://v6.exchangerate-api.com/v6/";
    private final ClienteHttpRequest clienteHttp;
    
    public ClienteExchangeRate(@Value("${api.exchangerate.key:}") String apiKey) {
        this.apiKey = apiKey;
        this.clienteHttp = new ClienteHttpRequest();
        
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            log.warn("La API key de ExchangeRate-API no está configurada. Las solicitudes a la API fallarán.");
        }
    }
    
    /**
     * Obtiene las tasas de cambio para una moneda base
     * @param monedaBase Código de la moneda base (ej. USD, EUR)
     * @return JsonObject con las tasas de cambio
     * @throws IOException Si ocurre un error de conexión
     */
    public JsonObject obtenerTasas(String monedaBase) throws IOException {
        String url = URL_BASE + apiKey + "/latest/" + monedaBase;
        try {
            String respuesta = clienteHttp.get(url, null, null);
            return JsonParser.parseString(respuesta).getAsJsonObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("La solicitud fue interrumpida", e);
        }
    }
    
    /**
     * Obtiene las tasas de cambio para una moneda base de forma asíncrona
     * @param monedaBase Código de la moneda base (ej. USD, EUR)
     * @return CompletableFuture<JsonObject> con las tasas de cambio
     */
    public CompletableFuture<JsonObject> obtenerTasasAsync(String monedaBase) {
        String url = URL_BASE + apiKey + "/latest/" + monedaBase;
        return clienteHttp.getAsync(url, null, null)
                .thenApply(respuesta -> JsonParser.parseString(respuesta).getAsJsonObject());
    }
    
    /**
     * Convierte un monto de una moneda a otra
     * @param monedaOrigen Código de la moneda de origen
     * @param monedaDestino Código de la moneda de destino
     * @param monto Monto a convertir
     * @return JsonObject con el resultado de la conversión
     * @throws IOException Si ocurre un error de conexión
     */
    public JsonObject convertir(String monedaOrigen, String monedaDestino, double monto) throws IOException {
        try {
            // Intenta primero con el endpoint pair
            String url = URL_BASE + apiKey + "/pair/" + monedaOrigen + "/" + monedaDestino;
            
            // Si hay un monto, agregarlo a la URL
            if (monto > 0) {
                url = url + "/" + monto;
            }
            
            log.debug("Intentando conversión con endpoint pair: {}", url);
            
            try {
                String respuesta = clienteHttp.get(url, null, null);
                return JsonParser.parseString(respuesta).getAsJsonObject();
            } catch (IOException e) {
                // Si falla, usar el método alternativo con latest
                log.debug("Error al usar endpoint pair: {}. Intentando con endpoint latest", e.getMessage());
                return convertirConLatest(monedaOrigen, monedaDestino, monto);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("La solicitud fue interrumpida", e);
        }
    }
    
    /**
     * Método alternativo para convertir usando el endpoint latest
     */
    private JsonObject convertirConLatest(String monedaOrigen, String monedaDestino, double monto) throws IOException, InterruptedException {
        String url = URL_BASE + apiKey + "/latest/" + monedaOrigen;
        log.debug("Realizando conversión alternativa con endpoint latest: {}", url);
        
        String respuesta = clienteHttp.get(url, null, null);
        JsonObject json = JsonParser.parseString(respuesta).getAsJsonObject();
        
        // Crear manualmente un objeto de respuesta similar al del endpoint pair
        JsonObject resultado = new JsonObject();
        resultado.addProperty("result", "success");
        resultado.addProperty("documentation", "https://www.exchangerate-api.com/docs");
        resultado.addProperty("terms_of_use", "https://www.exchangerate-api.com/terms");
        
        if (json.has("time_last_update_unix")) {
            resultado.addProperty("time_last_update_unix", json.get("time_last_update_unix").getAsLong());
        }
        if (json.has("time_last_update_utc")) {
            resultado.addProperty("time_last_update_utc", json.get("time_last_update_utc").getAsString());
        }
        if (json.has("time_next_update_unix")) {
            resultado.addProperty("time_next_update_unix", json.get("time_next_update_unix").getAsLong());
        }
        if (json.has("time_next_update_utc")) {
            resultado.addProperty("time_next_update_utc", json.get("time_next_update_utc").getAsString());
        }
        
        resultado.addProperty("base_code", monedaOrigen);
        resultado.addProperty("target_code", monedaDestino);
        
        // Obtener la tasa de conversión del objeto conversion_rates
        double tasaConversion = json.getAsJsonObject("conversion_rates").get(monedaDestino).getAsDouble();
        resultado.addProperty("conversion_rate", tasaConversion);
        
        // Calcular y agregar el resultado de la conversión
        double resultadoConversion = monto * tasaConversion;
        resultado.addProperty("conversion_result", resultadoConversion);
        
        return resultado;
    }
    
    /**
     * Convierte un monto de una moneda a otra de forma asíncrona
     * @param monedaOrigen Código de la moneda de origen
     * @param monedaDestino Código de la moneda de destino
     * @param monto Monto a convertir
     * @return CompletableFuture<JsonObject> con el resultado de la conversión
     */
    public CompletableFuture<JsonObject> convertirAsync(String monedaOrigen, String monedaDestino, double monto) {
        // Primero intentamos con el endpoint pair
        String url = URL_BASE + apiKey + "/pair/" + monedaOrigen + "/" + monedaDestino;
        
        // Si hay un monto, agregarlo a la URL
        if (monto > 0) {
            url = url + "/" + monto;
        }
        
        final String finalUrl = url;
        log.debug("Intentando conversión asíncrona con endpoint pair: {}", finalUrl);
        
        return clienteHttp.getAsync(finalUrl, null, null)
                .thenApply(respuesta -> JsonParser.parseString(respuesta).getAsJsonObject())
                .exceptionally(ex -> {
                    log.debug("Error al usar endpoint pair asíncrono: {}. Intentando con endpoint latest", ex.getMessage());
                    try {
                        // Si falla, usar el método alternativo con latest
                        return convertirConLatest(monedaOrigen, monedaDestino, monto);
                    } catch (Exception e) {
                        throw new RuntimeException("Error al realizar conversión alternativa: " + e.getMessage(), e);
                    }
                });
    }
    
    /**
     * Obtiene la lista de monedas soportadas
     * @return JsonObject con las monedas disponibles
     * @throws IOException Si ocurre un error de conexión
     */
    public JsonObject obtenerMonedas() throws IOException {
        String url = URL_BASE + apiKey + "/codes";
        try {
            String respuesta = clienteHttp.get(url, null, null);
            return JsonParser.parseString(respuesta).getAsJsonObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("La solicitud fue interrumpida", e);
        }
    }
    
    /**
     * Obtiene la lista de monedas soportadas de forma asíncrona
     * @return CompletableFuture<JsonObject> con las monedas disponibles
     */
    public CompletableFuture<JsonObject> obtenerMonedasAsync() {
        String url = URL_BASE + apiKey + "/codes";
        return clienteHttp.getAsync(url, null, null)
                .thenApply(respuesta -> JsonParser.parseString(respuesta).getAsJsonObject());
    }
} 