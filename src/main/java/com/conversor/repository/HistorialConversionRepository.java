package com.conversor.repository;

import com.conversor.model.HistorialConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialConversionRepository extends JpaRepository<HistorialConversion, Long> {
    
    /**
     * Encuentra las últimas 10 conversiones ordenadas por fecha descendente
     * @return Lista de las 10 conversiones más recientes
     */
    List<HistorialConversion> findTop10ByOrderByFechaHoraDesc();
    
    /**
     * Encuentra todas las conversiones de un tipo específico ordenadas por fecha descendente
     * @param tipoConversion Tipo de conversión ("MONEDA" o "CRIPTO")
     * @return Lista de conversiones del tipo especificado
     */
    List<HistorialConversion> findByTipoConversionOrderByFechaHoraDesc(String tipoConversion);
    
    /**
     * Encuentra todas las conversiones entre una moneda de origen y una de destino específicas
     * @param monedaOrigen Código de la moneda de origen
     * @param monedaDestino Código de la moneda de destino
     * @return Lista de conversiones entre las monedas especificadas
     */
    List<HistorialConversion> findByMonedaOrigenAndMonedaDestinoOrderByFechaHoraDesc(String monedaOrigen, String monedaDestino);
    
    /**
     * Cuenta cuántas conversiones hay de un tipo específico
     * @param tipoConversion Tipo de conversión ("MONEDA" o "CRIPTO")
     * @return Número de conversiones del tipo especificado
     */
    long countByTipoConversion(String tipoConversion);
} 