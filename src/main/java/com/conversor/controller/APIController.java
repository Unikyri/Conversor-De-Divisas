package com.conversor.controller;

import com.conversor.model.HistorialConversion;
import com.conversor.service.ConversionService;
import com.conversor.service.GraficosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para la API REST de conversiones
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class APIController {

    private final ConversionService conversionService;
    private final GraficosService graficosService;
    
    /**
     * Obtiene la lista de monedas disponibles
     */
    @GetMapping("/monedas")
    public ResponseEntity<Map<String, String>> obtenerMonedas() {
        try {
            Map<String, String> monedas = conversionService.obtenerMonedas();
            return ResponseEntity.ok(monedas);
        } catch (Exception e) {
            log.error("Error al obtener monedas", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Realiza una conversión entre monedas fiduciarias
     */
    @GetMapping("/convertir")
    public ResponseEntity<Map<String, Object>> convertirMoneda(
            @RequestParam String monedaOrigen,
            @RequestParam String monedaDestino,
            @RequestParam double cantidad) {
        
        try {
            double resultado = conversionService.convertirMoneda(monedaOrigen, monedaDestino, cantidad);
            
            Map<String, Object> response = new HashMap<>();
            response.put("monedaOrigen", monedaOrigen);
            response.put("monedaDestino", monedaDestino);
            response.put("cantidadOrigen", cantidad);
            response.put("cantidadDestino", resultado);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al convertir monedas", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Realiza una conversión entre criptomonedas y monedas fiduciarias
     */
    @GetMapping("/convertir-cripto")
    public ResponseEntity<Map<String, Object>> convertirCripto(
            @RequestParam String criptomoneda,
            @RequestParam String monedaFiat,
            @RequestParam double cantidad) {
        
        try {
            double resultado = conversionService.convertirCripto(criptomoneda, monedaFiat, cantidad);
            
            Map<String, Object> response = new HashMap<>();
            response.put("criptomoneda", criptomoneda);
            response.put("monedaFiat", monedaFiat);
            response.put("cantidadOrigen", cantidad);
            response.put("cantidadDestino", resultado);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al convertir criptomonedas", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtiene el historial de conversiones recientes
     */
    @GetMapping("/historial")
    public ResponseEntity<List<HistorialConversion>> obtenerHistorial() {
        return ResponseEntity.ok(conversionService.obtenerUltimasConversiones());
    }
    
    /**
     * Obtiene el historial de conversiones por tipo
     */
    @GetMapping("/historial/{tipo}")
    public ResponseEntity<List<HistorialConversion>> obtenerHistorialPorTipo(@PathVariable String tipo) {
        return ResponseEntity.ok(conversionService.obtenerHistorialPorTipo(tipo));
    }
    
    /**
     * Obtiene datos históricos de tasas de cambio para el gráfico
     */
    @GetMapping("/graf/historial-tasas")
    public ResponseEntity<Map<String, Object>> obtenerHistorialTasas(
            @RequestParam String monedaOrigen, 
            @RequestParam String monedaDestino) {
        
        try {
            Map<String, Object> result = graficosService.obtenerDatosHistoricosTasas(monedaOrigen, monedaDestino);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error al obtener historial de tasas", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtiene datos para el gráfico de distribución de monedas
     */
    @GetMapping("/graf/distribucion-monedas")
    public ResponseEntity<Map<String, Object>> obtenerDistribucionMonedas() {
        try {
            Map<String, Object> result = graficosService.obtenerDatosDistribucionMonedas();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error al obtener distribución de monedas", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtiene datos para el gráfico de distribución por tipos de conversión
     */
    @GetMapping("/graf/distribucion-tipos")
    public ResponseEntity<Map<String, Object>> obtenerDistribucionTipos() {
        try {
            Map<String, Object> result = graficosService.obtenerDatosDistribucionTipos();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error al obtener distribución de tipos", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 