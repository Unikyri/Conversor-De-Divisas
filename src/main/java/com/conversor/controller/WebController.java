package com.conversor.controller;

import com.conversor.model.HistorialConversion;
import com.conversor.service.ConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final ConversionService conversionService;
    
    @GetMapping("/")
    public String index(Model model) {
        try {
            Map<String, String> monedas = conversionService.obtenerMonedas();
            List<HistorialConversion> historial = conversionService.obtenerUltimasConversiones();
            
            model.addAttribute("monedas", monedas);
            model.addAttribute("historial", historial);
        } catch (Exception e) {
            log.error("Error al cargar la página principal", e);
            model.addAttribute("error", "Error al cargar datos: " + e.getMessage());
        }
        
        return "index";
    }
    
    @PostMapping("/convertir")
    public String convertir(
            @RequestParam String monedaOrigen,
            @RequestParam String monedaDestino,
            @RequestParam double cantidad,
            Model model) {
        
        try {
            double resultado = conversionService.convertirMoneda(monedaOrigen, monedaDestino, cantidad);
            Map<String, String> monedas = conversionService.obtenerMonedas();
            List<HistorialConversion> historial = conversionService.obtenerUltimasConversiones();
            
            model.addAttribute("monedas", monedas);
            model.addAttribute("historial", historial);
            model.addAttribute("resultadoConversion", String.format("%.2f %s = %.2f %s", 
                    cantidad, monedaOrigen, resultado, monedaDestino));
            
            // Mantener los valores seleccionados
            model.addAttribute("monedaOrigenSeleccionada", monedaOrigen);
            model.addAttribute("monedaDestinoSeleccionada", monedaDestino);
            model.addAttribute("cantidadSeleccionada", cantidad);
            model.addAttribute("activarTabMonedas", true);
        } catch (Exception e) {
            log.error("Error al realizar conversión", e);
            model.addAttribute("error", "Error al convertir: " + e.getMessage());
        }
        
        return "index";
    }
    
    @PostMapping("/convertir-cripto")
    public String convertirCripto(
            @RequestParam String criptomoneda,
            @RequestParam String monedaFiat,
            @RequestParam double cantidad,
            Model model) {
        
        try {
            double resultado = conversionService.convertirCripto(criptomoneda, monedaFiat, cantidad);
            Map<String, String> monedas = conversionService.obtenerMonedas();
            List<HistorialConversion> historial = conversionService.obtenerUltimasConversiones();
            
            model.addAttribute("monedas", monedas);
            model.addAttribute("historial", historial);
            model.addAttribute("resultadoConversion", String.format("%.8f %s = %.2f %s", 
                    cantidad, criptomoneda, resultado, monedaFiat));
            
            // Mantener los valores seleccionados
            model.addAttribute("criptomonedaSeleccionada", criptomoneda);
            model.addAttribute("monedaFiatSeleccionada", monedaFiat);
            model.addAttribute("cantidadCriptoSeleccionada", cantidad);
            model.addAttribute("activarTabCripto", true);
        } catch (Exception e) {
            log.error("Error al realizar conversión de criptomonedas", e);
            model.addAttribute("error", "Error al convertir criptomonedas: " + e.getMessage());
        }
        
        return "index";
    }
    
    @GetMapping("/historial")
    public String historial(Model model) {
        List<HistorialConversion> historial = conversionService.obtenerUltimasConversiones();
        model.addAttribute("historial", historial);
        return "historial";
    }
} 