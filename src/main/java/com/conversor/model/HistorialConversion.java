package com.conversor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_conversiones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String monedaOrigen;

    @Column(nullable = false)
    private String monedaDestino;

    @Column(nullable = false)
    private Double cantidadOrigen;

    @Column(nullable = false)
    private Double cantidadDestino;

    @Column(nullable = false)
    private Double tasaCambio;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(length = 50)
    private String tipoConversion; // MONEDA o CRIPTO
} 