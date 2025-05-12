package com.conversor.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Cliente para la API de CoinMarketCap
 */
@Slf4j
@Component
public class ClienteCoinMarketCap {
    
    private final String apiKey;
    private static final String URL_BASE = "https://pro-api.coinmarketcap.com/v1/";
    private static final String HEADER_NAME = "X-CMC_PRO_API_KEY";
    private final ClienteHttpRequest clienteHttp;
    
    public ClienteCoinMarketCap(@Value("${api.coinmarketcap.key:}") String apiKey) {
        this.apiKey = apiKey;
        this.clienteHttp = new ClienteHttpRequest();
        
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            log.warn("La API key de CoinMarketCap no está configurada. Las solicitudes a la API fallarán.");
        }
    }
    
    /**
     * Obtiene la lista de criptomonedas
     * @param limite Límite de resultados (max 5000)
     * @return JsonObject con la lista de criptomonedas
     * @throws IOException Si ocurre un error de conexión
     */
    public JsonObject listarCriptomonedas(int limite) throws IOException {
        String url = URL_BASE + "cryptocurrency/listings/latest?limit=" + limite;
        try {
            log.debug("Realizando solicitud a: {}", url);
            String respuesta = clienteHttp.get(url, apiKey, HEADER_NAME);
            return JsonParser.parseString(respuesta).getAsJsonObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("La solicitud fue interrumpida", e);
        }
    }
    
    /**
     * Obtiene la lista de criptomonedas de forma asíncrona
     * @param limite Límite de resultados (max 5000)
     * @return CompletableFuture<JsonObject> con la lista de criptomonedas
     */
    public CompletableFuture<JsonObject> listarCriptomonedasAsync(int limite) {
        String url = URL_BASE + "cryptocurrency/listings/latest?limit=" + limite;
        return clienteHttp.getAsync(url, apiKey, HEADER_NAME)
                .thenApply(respuesta -> JsonParser.parseString(respuesta).getAsJsonObject());
    }
    
    /**
     * Obtiene información detallada de una criptomoneda por su símbolo
     * @param simbolo Símbolo de la criptomoneda (ej. BTC, ETH)
     * @return JsonObject con la información de la criptomoneda
     * @throws IOException Si ocurre un error de conexión
     */
    public JsonObject obtenerInfoCriptomoneda(String simbolo) throws IOException {
        String url = URL_BASE + "cryptocurrency/quotes/latest?symbol=" + simbolo;
        try {
            log.debug("Realizando solicitud a: {}", url);
            String respuesta = clienteHttp.get(url, apiKey, HEADER_NAME);
            return JsonParser.parseString(respuesta).getAsJsonObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("La solicitud fue interrumpida", e);
        }
    }
    
    /**
     * Obtiene información detallada de una criptomoneda por su símbolo de forma asíncrona
     * @param simbolo Símbolo de la criptomoneda (ej. BTC, ETH)
     * @return CompletableFuture<JsonObject> con la información de la criptomoneda
     */
    public CompletableFuture<JsonObject> obtenerInfoCriptomonedaAsync(String simbolo) {
        String url = URL_BASE + "cryptocurrency/quotes/latest?symbol=" + simbolo;
        return clienteHttp.getAsync(url, apiKey, HEADER_NAME)
                .thenApply(respuesta -> JsonParser.parseString(respuesta).getAsJsonObject());
    }
    
    /**
     * Obtiene la tasa de conversión entre una criptomoneda y una moneda fiduciaria
     * @param simboloCripto Símbolo de la criptomoneda (ej. BTC, ETH)
     * @param simboloFiat Símbolo de la moneda fiduciaria (ej. USD, EUR)
     * @return JsonObject con la información de conversión
     * @throws IOException Si ocurre un error de conexión
     */
    public JsonObject obtenerTasaConversion(String simboloCripto, String simboloFiat) throws IOException {
        // Según la documentación de CoinMarketCap necesitamos usar el endpoint correcto
        // para la versión 1 de la API
        String url = URL_BASE + "cryptocurrency/quotes/latest?symbol=" + simboloCripto + "&convert=" + simboloFiat;
        try {
            log.debug("Realizando solicitud a: {}", url);
            String respuesta = clienteHttp.get(url, apiKey, HEADER_NAME);
            log.debug("Respuesta recibida: {}", respuesta);
            return JsonParser.parseString(respuesta).getAsJsonObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("La solicitud fue interrumpida", e);
        }
    }
    
    /**
     * Obtiene la tasa de conversión entre una criptomoneda y una moneda fiduciaria de forma asíncrona
     * @param simboloCripto Símbolo de la criptomoneda (ej. BTC, ETH)
     * @param simboloFiat Símbolo de la moneda fiduciaria (ej. USD, EUR)
     * @return CompletableFuture<JsonObject> con la información de conversión
     */
    public CompletableFuture<JsonObject> obtenerTasaConversionAsync(String simboloCripto, String simboloFiat) {
        String url = URL_BASE + "cryptocurrency/quotes/latest?symbol=" + simboloCripto + "&convert=" + simboloFiat;
        return clienteHttp.getAsync(url, apiKey, HEADER_NAME)
                .thenApply(respuesta -> JsonParser.parseString(respuesta).getAsJsonObject());
    }
} 