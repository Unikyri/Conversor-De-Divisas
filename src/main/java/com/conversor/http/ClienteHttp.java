package com.conversor.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Cliente HTTP para realizar solicitudes a APIs externas
 */
public class ClienteHttp {
    
    /**
     * Realiza una solicitud GET a la URL especificada
     * @param urlStr URL a la cual realizar la solicitud
     * @param apiKey Clave de API si es necesaria (puede ser null)
     * @param headerName Nombre del encabezado para la API key
     * @return Respuesta del servidor en formato String
     * @throws IOException Si ocurre un error de conexión
     */
    public String get(String urlStr, String apiKey, String headerName) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        
        conexion.setRequestMethod("GET");
        conexion.setRequestProperty("Accept", "application/json");
        
        if (apiKey != null && headerName != null) {
            conexion.setRequestProperty(headerName, apiKey);
        }
        
        if (conexion.getResponseCode() != 200) {
            throw new IOException("Error HTTP: " + conexion.getResponseCode() + " - " + conexion.getResponseMessage());
        }
        
        BufferedReader br = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        StringBuilder respuesta = new StringBuilder();
        String linea;
        
        while ((linea = br.readLine()) != null) {
            respuesta.append(linea);
        }
        
        br.close();
        conexion.disconnect();
        
        return respuesta.toString();
    }
} 