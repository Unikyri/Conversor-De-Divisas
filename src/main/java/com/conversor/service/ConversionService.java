package com.conversor.service;

import com.conversor.http.ClienteExchangeRate;
import com.conversor.http.ClienteCoinMarketCap;
import com.conversor.model.HistorialConversion;
import com.conversor.model.TipoConversion;
import com.conversor.repository.HistorialConversionRepository;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversionService {

    private final HistorialConversionRepository historialRepository;
    private final ClienteExchangeRate clienteExchangeRate;
    private final ClienteCoinMarketCap clienteCoinMarketCap;
    
    private Map<String, String> monedasCache = new HashMap<>();
    
    /**
     * Obtiene la lista de monedas disponibles para conversión
     * @return Mapa con código y nombre de las monedas (ordenado alfabéticamente por nombre)
     * @throws IOException En caso de error en la comunicación con la API
     * @throws ExecutionException Si hay un error en la ejecución asíncrona
     * @throws InterruptedException Si la operación es interrumpida
     */
    public Map<String, String> obtenerMonedas() throws IOException, ExecutionException, InterruptedException {
        if (monedasCache.isEmpty()) {
            cargarMonedasDesdeAPI();
        }
        
        return ordenarMonedasAlfabeticamente(monedasCache);
    }
    
    /**
     * Carga la lista de monedas desde la API y las almacena en caché
     */
    private void cargarMonedasDesdeAPI() throws ExecutionException, InterruptedException {
        log.debug("Obteniendo lista de monedas desde la API...");
        CompletableFuture<JsonObject> future = clienteExchangeRate.obtenerMonedasAsync();
        JsonObject json = future.get();
        
        if (json.has("supported_codes")) {
            log.debug("Procesando lista de monedas recibida");
            json.getAsJsonArray("supported_codes").forEach(element -> {
                String codigo = element.getAsJsonArray().get(0).getAsString();
                String nombre = element.getAsJsonArray().get(1).getAsString();
                monedasCache.put(codigo, nombre);
            });
            log.debug("Se obtuvieron {} monedas", monedasCache.size());
        } else {
            log.warn("La respuesta de la API no contiene el campo 'supported_codes'");
        }
    }
    
    /**
     * Ordena el mapa de monedas por nombre alfabéticamente
     */
    private Map<String, String> ordenarMonedasAlfabeticamente(Map<String, String> monedas) {
        Map<String, String> monedasOrdenadas = new TreeMap<>((k1, k2) -> {
            return monedas.get(k1).compareToIgnoreCase(monedas.get(k2));
        });
        monedasOrdenadas.putAll(monedas);
        return monedasOrdenadas;
    }
    
    /**
     * Convierte una cantidad de una moneda a otra
     * @param monedaOrigen Código de la moneda de origen
     * @param monedaDestino Código de la moneda de destino
     * @param cantidad Cantidad a convertir
     * @return Resultado de la conversión
     * @throws IOException En caso de error en la comunicación con la API
     * @throws ExecutionException Si hay un error en la ejecución asíncrona
     * @throws InterruptedException Si la operación es interrumpida
     */
    public double convertirMoneda(String monedaOrigen, String monedaDestino, double cantidad) 
            throws IOException, ExecutionException, InterruptedException {
        log.debug("Iniciando conversión de {} {} a {}", cantidad, monedaOrigen, monedaDestino);
        
        CompletableFuture<JsonObject> future = clienteExchangeRate.convertirAsync(monedaOrigen, monedaDestino, cantidad);
        try {
            JsonObject json = future.get();
            log.debug("Respuesta recibida: {}", json);
            
            if (json.has("conversion_result")) {
                double resultado = json.get("conversion_result").getAsDouble();
                double tasaCambio = json.get("conversion_rate").getAsDouble();
                
                log.debug("Conversión exitosa: {} {} = {} {} (tasa: {})",
                        cantidad, monedaOrigen, resultado, monedaDestino, tasaCambio);
                
                // Registrar en el historial
                registrarConversion(monedaOrigen, monedaDestino, cantidad, resultado, tasaCambio, TipoConversion.MONEDA);
                
                return resultado;
            } else {
                log.error("La respuesta no contiene el campo 'conversion_result': {}", json);
                throw new IOException("Error en la conversión: respuesta incompleta");
            }
        } catch (ExecutionException e) {
            log.error("Error al ejecutar la conversión asíncrona", e);
            throw e;
        }
    }
    
    /**
     * Convierte una criptomoneda a una moneda fiduciaria
     * @param criptomoneda Símbolo de la criptomoneda (ej. BTC)
     * @param monedaFiat Símbolo de la moneda fiduciaria (ej. USD)
     * @param cantidad Cantidad a convertir
     * @return Resultado de la conversión
     * @throws IOException En caso de error en la comunicación con la API
     */
    public double convertirCripto(String criptomoneda, String monedaFiat, double cantidad) throws IOException {
        try {
            log.debug("Iniciando conversión de {} {} a {}", cantidad, criptomoneda, monedaFiat);
            
            JsonObject json = clienteCoinMarketCap.obtenerTasaConversion(criptomoneda, monedaFiat);
            log.debug("Respuesta de CoinMarketCap: {}", json);
            
            // Estructura del JSON de la respuesta para cryptocurrency/quotes/latest
            double tasaConversion = extraerTasaConversion(json, criptomoneda, monedaFiat);
            
            double resultado = cantidad * tasaConversion;
            
            log.debug("Conversión exitosa: {} {} = {} {} (tasa: {})",
                    cantidad, criptomoneda, resultado, monedaFiat, tasaConversion);
            
            // Registrar en el historial
            registrarConversion(criptomoneda, monedaFiat, cantidad, resultado, tasaConversion, TipoConversion.CRIPTO);
            
            return resultado;
        } catch (Exception e) {
            log.error("Error al procesar la respuesta de CoinMarketCap: {}", e.getMessage(), e);
            throw new IOException("Error en la conversión de criptomoneda: " + e.getMessage());
        }
    }
    
    /**
     * Extrae la tasa de conversión de la respuesta JSON de CoinMarketCap
     */
    private double extraerTasaConversion(JsonObject json, String criptomoneda, String monedaFiat) {
        return json.getAsJsonObject("data")
            .getAsJsonObject(criptomoneda)
            .getAsJsonObject("quote")
            .getAsJsonObject(monedaFiat)
            .get("price").getAsDouble();
    }
    
    /**
     * Registra una conversión en el historial
     */
    private HistorialConversion registrarConversion(
            String monedaOrigen, 
            String monedaDestino, 
            double cantidadOrigen, 
            double cantidadDestino, 
            double tasaCambio, 
            TipoConversion tipoConversion) {
        
        HistorialConversion historial = HistorialConversion.builder()
                .monedaOrigen(monedaOrigen)
                .monedaDestino(monedaDestino)
                .cantidadOrigen(cantidadOrigen)
                .cantidadDestino(cantidadDestino)
                .tasaCambio(tasaCambio)
                .fechaHora(LocalDateTime.now())
                .tipoConversion(tipoConversion.name())
                .build();
        
        historialRepository.save(historial);
        log.debug("Conversión guardada en historial con ID: {}", historial.getId());
        
        return historial;
    }
    
    /**
     * Obtiene el historial de las últimas 10 conversiones
     * @return Lista con las últimas 10 conversiones
     */
    public List<HistorialConversion> obtenerUltimasConversiones() {
        log.debug("Obteniendo últimas 10 conversiones del historial");
        List<HistorialConversion> historial = historialRepository.findTop10ByOrderByFechaHoraDesc();
        log.debug("Se encontraron {} conversiones recientes", historial.size());
        return historial;
    }
    
    /**
     * Obtiene el historial de todas las conversiones
     * @return Lista con todas las conversiones
     */
    public List<HistorialConversion> obtenerTodasLasConversiones() {
        log.debug("Obteniendo todas las conversiones del historial");
        List<HistorialConversion> historial = historialRepository.findAll();
        log.debug("Se encontraron {} conversiones en total", historial.size());
        return historial;
    }
    
    /**
     * Obtiene el historial de conversiones por tipo (MONEDA o CRIPTO)
     * @param tipo Tipo de conversión ("MONEDA" o "CRIPTO")
     * @return Lista con el historial de conversiones del tipo especificado
     */
    public List<HistorialConversion> obtenerHistorialPorTipo(String tipo) {
        log.debug("Obteniendo historial de conversiones de tipo: {}", tipo);
        List<HistorialConversion> historial = historialRepository.findByTipoConversionOrderByFechaHoraDesc(tipo);
        log.debug("Se encontraron {} conversiones de tipo {}", historial.size(), tipo);
        return historial;
    }
    
    /**
     * Obtiene el historial de conversiones entre dos monedas específicas
     * @param monedaOrigen Moneda de origen
     * @param monedaDestino Moneda de destino
     * @return Lista con el historial de conversiones entre las monedas especificadas
     */
    public List<HistorialConversion> obtenerHistorialPorMonedas(String monedaOrigen, String monedaDestino) {
        log.debug("Obteniendo historial de conversiones entre {} y {}", monedaOrigen, monedaDestino);
        
        // Buscar conversiones directas (origen -> destino)
        List<HistorialConversion> historialDirecto = historialRepository
                .findByMonedaOrigenAndMonedaDestinoOrderByFechaHoraDesc(monedaOrigen, monedaDestino);
        
        // Si hay suficientes datos directos, no es necesario buscar inversos
        if (historialDirecto.size() >= 7) {
            log.debug("Se encontraron {} conversiones directas", historialDirecto.size());
            return historialDirecto;
        }
        
        // Buscar también conversiones inversas (destino -> origen) para complementar
        List<HistorialConversion> historialInverso = historialRepository
                .findByMonedaOrigenAndMonedaDestinoOrderByFechaHoraDesc(monedaDestino, monedaOrigen);
        
        // Convertir las conversiones inversas a directas
        List<HistorialConversion> historialInversoConvertido = convertirConversionesInversas(
                historialInverso, monedaOrigen, monedaDestino);
        
        // Combinar ambas listas
        historialDirecto.addAll(historialInversoConvertido);
        
        // Ordenar por fecha (más reciente primero)
        historialDirecto.sort((a, b) -> b.getFechaHora().compareTo(a.getFechaHora()));
        
        log.debug("Se encontraron {} conversiones en total (directas + inversas convertidas)", 
                historialDirecto.size());
                
        return historialDirecto;
    }
    
    /**
     * Convierte conversiones inversas a directas (invirtiendo la tasa de cambio)
     */
    private List<HistorialConversion> convertirConversionesInversas(
            List<HistorialConversion> historialInverso, 
            String monedaOrigenDeseada, 
            String monedaDestinoDeseada) {
        
        return historialInverso.stream()
                .map(h -> {
                    // Invertir la tasa de cambio (1/tasa) para que sea equivalente
                    double tasaInvertida = 1.0 / h.getTasaCambio();
                    
                    return HistorialConversion.builder()
                            .id(h.getId())
                            .monedaOrigen(monedaOrigenDeseada)
                            .monedaDestino(monedaDestinoDeseada)
                            .cantidadOrigen(h.getCantidadDestino())
                            .cantidadDestino(h.getCantidadOrigen())
                            .tasaCambio(tasaInvertida)
                            .fechaHora(h.getFechaHora())
                            .tipoConversion(h.getTipoConversion())
                            .build();
                })
                .collect(Collectors.toList());
    }
} 