package com.conversor.service;

import com.conversor.model.HistorialConversion;
import com.conversor.model.TipoConversion;
import com.conversor.repository.HistorialConversionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para generar datos para gráficos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GraficosService {

    private final HistorialConversionRepository historialRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Random RANDOM = new Random();

    /**
     * Obtiene datos históricos de tasas de cambio para un par de monedas
     * @param monedaOrigen Moneda de origen
     * @param monedaDestino Moneda de destino
     * @return Mapa con datos para el gráfico
     */
    public Map<String, Object> obtenerDatosHistoricosTasas(String monedaOrigen, String monedaDestino) {
        log.debug("Obteniendo datos históricos de tasas para {} -> {}", monedaOrigen, monedaDestino);
        
        // Obtener historial de conversiones entre las monedas especificadas
        List<HistorialConversion> historial = historialRepository
                .findByMonedaOrigenAndMonedaDestinoOrderByFechaHoraDesc(monedaOrigen, monedaDestino);
        
        log.debug("Conversiones directas encontradas: {}", historial.size());
        
        // Si no hay suficientes datos directos, buscar también conversiones inversas
        if (historial.size() < 7) {
            List<HistorialConversion> historialInverso = historialRepository
                    .findByMonedaOrigenAndMonedaDestinoOrderByFechaHoraDesc(monedaDestino, monedaOrigen);
            
            log.debug("Conversiones inversas encontradas: {}", historialInverso.size());
            
            // Convertir las conversiones inversas a directas
            List<HistorialConversion> historialInversoConvertido = historialInverso.stream()
                    .map(h -> {
                        double tasaInvertida = 1.0 / h.getTasaCambio();
                        return HistorialConversion.builder()
                                .id(h.getId())
                                .monedaOrigen(monedaOrigen)
                                .monedaDestino(monedaDestino)
                                .cantidadOrigen(h.getCantidadDestino())
                                .cantidadDestino(h.getCantidadOrigen())
                                .tasaCambio(tasaInvertida)
                                .fechaHora(h.getFechaHora())
                                .tipoConversion(h.getTipoConversion())
                                .build();
                    })
                    .collect(Collectors.toList());
            
            // Combinar ambas listas
            historial.addAll(historialInversoConvertido);
            
            // Ordenar por fecha (más reciente primero)
            historial.sort((a, b) -> b.getFechaHora().compareTo(a.getFechaHora()));
            
            log.debug("Total de conversiones después de combinar: {}", historial.size());
        }
        
        // Crear mapa para evitar duplicados por fecha (tomando el más reciente)
        Map<String, HistorialConversion> historialPorFecha = new HashMap<>();
        for (HistorialConversion conversion : historial) {
            String fechaKey = conversion.getFechaHora().toLocalDate().toString();
            if (!historialPorFecha.containsKey(fechaKey)) {
                historialPorFecha.put(fechaKey, conversion);
            }
        }
        
        log.debug("Días únicos en el historial: {}", historialPorFecha.size());
        
        // Generar datos faltantes para completar 7 días
        List<HistorialConversion> historialCompleto = completarDatosFaltantes(
                new ArrayList<>(historialPorFecha.values()), 
                monedaOrigen, 
                monedaDestino
        );
        
        log.debug("Total de días después de completar datos faltantes: {}", historialCompleto.size());
        
        // Ordenar cronológicamente
        historialCompleto.sort(Comparator.comparing(HistorialConversion::getFechaHora));
        
        // Crear las etiquetas (fechas) y datos (tasas) para el gráfico
        List<String> labels = new ArrayList<>();
        List<Double> tasas = new ArrayList<>();
        
        for (HistorialConversion conversion : historialCompleto) {
            labels.add(conversion.getFechaHora().format(FORMATTER));
            tasas.add(conversion.getTasaCambio());
            log.debug("Punto de datos: {} - {}", conversion.getFechaHora().format(FORMATTER), conversion.getTasaCambio());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("tasas", tasas);
        result.put("monedaOrigen", monedaOrigen);
        result.put("monedaDestino", monedaDestino);
        
        log.debug("Se generaron datos de gráfico con {} puntos", labels.size());
        return result;
    }
    
    /**
     * Completa los datos faltantes para tener al menos 7 días de historial
     * @param historicoDisponible Lista de conversiones históricas disponibles
     * @param monedaOrigen Moneda de origen
     * @param monedaDestino Moneda de destino
     * @return Lista completa con al menos 7 días de historial
     */
    private List<HistorialConversion> completarDatosFaltantes(
            List<HistorialConversion> historicoDisponible, 
            String monedaOrigen, 
            String monedaDestino) {
        
        // Si no hay datos históricos, crear datos simulados completamente
        if (historicoDisponible.isEmpty()) {
            return generarDatosSimulados(monedaOrigen, monedaDestino, 7);
        }
        
        // Ordenar por fecha (más antiguo primero)
        historicoDisponible.sort(Comparator.comparing(HistorialConversion::getFechaHora));
        
        // Obtener el rango de fechas
        LocalDateTime fechaInicio = historicoDisponible.get(0).getFechaHora().toLocalDate().atStartOfDay();
        LocalDateTime fechaFin = LocalDateTime.now().toLocalDate().atStartOfDay();
        
        // Si tenemos suficientes datos (7 días o más), retornar los datos existentes
        if (historicoDisponible.size() >= 7) {
            return historicoDisponible;
        }
        
        // Crear un mapa de fechas existentes para evitar duplicados
        Map<String, HistorialConversion> fechasExistentes = new HashMap<>();
        for (HistorialConversion conversion : historicoDisponible) {
            String fechaKey = conversion.getFechaHora().toLocalDate().toString();
            fechasExistentes.put(fechaKey, conversion);
        }
        
        // Completar con datos simulados para los días faltantes
        List<HistorialConversion> resultado = new ArrayList<>(historicoDisponible);
        
        // Determinar la tasa base para simulaciones (usamos la última disponible o un valor por defecto)
        double tasaBase = historicoDisponible.isEmpty() ? 1.0 : historicoDisponible.get(historicoDisponible.size() - 1).getTasaCambio();
        
        // Llenar datos faltantes hacia atrás desde fecha de inicio
        LocalDateTime fechaActual = fechaInicio.minusDays(1);
        int diasAgregados = 0;
        
        while (resultado.size() < 7 && diasAgregados < 6) {
            String fechaKey = fechaActual.toLocalDate().toString();
            if (!fechasExistentes.containsKey(fechaKey)) {
                // Simular una pequeña variación en la tasa (±2%)
                double variacion = 0.96 + (RANDOM.nextDouble() * 0.08); // Entre 0.96 y 1.04
                double nuevaTasa = tasaBase * variacion;
                
                HistorialConversion nuevoHistorial = HistorialConversion.builder()
                        .monedaOrigen(monedaOrigen)
                        .monedaDestino(monedaDestino)
                        .cantidadOrigen(1.0)
                        .cantidadDestino(nuevaTasa)
                        .tasaCambio(nuevaTasa)
                        .fechaHora(fechaActual)
                        .tipoConversion(TipoConversion.MONEDA.name())
                        .build();
                
                resultado.add(nuevoHistorial);
                fechasExistentes.put(fechaKey, nuevoHistorial);
                diasAgregados++;
            }
            fechaActual = fechaActual.minusDays(1);
        }
        
        // Llenar datos faltantes hacia adelante desde fecha de fin
        fechaActual = fechaFin;
        diasAgregados = 0;
        
        while (resultado.size() < 7 && diasAgregados < 6) {
            String fechaKey = fechaActual.toLocalDate().toString();
            if (!fechasExistentes.containsKey(fechaKey)) {
                // Simular una pequeña variación en la tasa (±2%)
                double variacion = 0.96 + (RANDOM.nextDouble() * 0.08); // Entre 0.96 y 1.04
                double nuevaTasa = tasaBase * variacion;
                
                HistorialConversion nuevoHistorial = HistorialConversion.builder()
                        .monedaOrigen(monedaOrigen)
                        .monedaDestino(monedaDestino)
                        .cantidadOrigen(1.0)
                        .cantidadDestino(nuevaTasa)
                        .tasaCambio(nuevaTasa)
                        .fechaHora(fechaActual.plusHours(12)) // Al mediodía
                        .tipoConversion(TipoConversion.MONEDA.name())
                        .build();
                
                resultado.add(nuevoHistorial);
                fechasExistentes.put(fechaKey, nuevoHistorial);
                diasAgregados++;
            }
            fechaActual = fechaActual.minusDays(1);
        }
        
        return resultado;
    }
    
    /**
     * Genera datos simulados para un rango de días
     * @param monedaOrigen Moneda de origen
     * @param monedaDestino Moneda de destino
     * @param dias Número de días a simular
     * @return Lista de conversiones simuladas
     */
    private List<HistorialConversion> generarDatosSimulados(String monedaOrigen, String monedaDestino, int dias) {
        List<HistorialConversion> resultado = new ArrayList<>();
        
        // Generar una tasa base razonable según las monedas
        double tasaBase = 1.0;
        if ("USD".equals(monedaOrigen) && "EUR".equals(monedaDestino)) {
            tasaBase = 0.93;
        } else if ("EUR".equals(monedaOrigen) && "USD".equals(monedaDestino)) {
            tasaBase = 1.08;
        } else if ("BTC".equals(monedaOrigen)) {
            tasaBase = 43000.0; // BTC a USD aproximado
        }
        
        LocalDateTime fechaFin = LocalDateTime.now();
        
        for (int i = dias - 1; i >= 0; i--) {
            LocalDateTime fecha = fechaFin.minusDays(i).withHour(12).withMinute(0).withSecond(0);
            
            // Simular una pequeña variación en la tasa (±3%)
            double variacion = 0.97 + (RANDOM.nextDouble() * 0.06); // Entre 0.97 y 1.03
            double tasa = tasaBase * variacion;
            
            HistorialConversion simulado = HistorialConversion.builder()
                    .monedaOrigen(monedaOrigen)
                    .monedaDestino(monedaDestino)
                    .cantidadOrigen(1.0)
                    .cantidadDestino(tasa)
                    .tasaCambio(tasa)
                    .fechaHora(fecha)
                    .tipoConversion("MONEDA".equals(monedaOrigen) ? TipoConversion.MONEDA.name() : TipoConversion.CRIPTO.name())
                    .build();
            
            resultado.add(simulado);
        }
        
        return resultado;
    }
    
    /**
     * Obtiene datos para el gráfico de distribución de monedas
     * @return Mapa con datos para el gráfico
     */
    public Map<String, Object> obtenerDatosDistribucionMonedas() {
        log.debug("Obteniendo distribución de conversiones por monedas");
        
        // Obtener todas las conversiones
        List<HistorialConversion> historial = historialRepository.findAll();
        
        // Contar ocurrencias de cada moneda de origen
        Map<String, Long> distribucionOrigenTemp = historial.stream()
                .collect(Collectors.groupingBy(HistorialConversion::getMonedaOrigen, Collectors.counting()));
        
        // Ordenar por cantidad de conversiones y tomar las 10 más frecuentes
        Map<String, Long> distribucionOrigen = distribucionOrigenTemp.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, 
                        Map.Entry::getValue, 
                        (e1, e2) -> e1, 
                        LinkedHashMap::new
                ));
        
        // Crear listas para el gráfico
        List<String> labels = new ArrayList<>(distribucionOrigen.keySet());
        List<Long> data = new ArrayList<>(distribucionOrigen.values());
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        
        log.debug("Se generaron datos de distribución con {} monedas", labels.size());
        return result;
    }
    
    /**
     * Obtiene datos para el gráfico de distribución por tipos de conversión
     * @return Mapa con datos para el gráfico
     */
    public Map<String, Object> obtenerDatosDistribucionTipos() {
        log.debug("Obteniendo distribución de conversiones por tipo");
        
        // Contar conversiones por tipo
        long conversionesMoneda = historialRepository.countByTipoConversion(TipoConversion.MONEDA.name());
        long conversionesCripto = historialRepository.countByTipoConversion(TipoConversion.CRIPTO.name());
        
        // Crear listas para el gráfico
        List<String> labels = Arrays.asList("Monedas", "Criptomonedas");
        List<Long> data = Arrays.asList(conversionesMoneda, conversionesCripto);
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        
        log.debug("Se generaron datos de distribución por tipo: Monedas={}, Criptomonedas={}", 
                conversionesMoneda, conversionesCripto);
        return result;
    }
} 