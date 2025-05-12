# Conversor de Monedas y Criptomonedas

Aplicación web para conversión de monedas y criptomonedas desarrollada como parte del desafío de Alura.

## Descripción

Esta aplicación permite realizar conversiones entre diferentes divisas y criptomonedas utilizando tasas de cambio actualizadas mediante APIs externas. Incluye un historial de todas las conversiones realizadas, visualización mediante gráficos y una interfaz web intuitiva con modo oscuro/claro.

## Funcionalidades

- Conversión entre monedas fiduciarias (USD, EUR, GBP, etc.)
- Conversión entre criptomonedas y monedas fiduciarias
- Historial de conversiones con marcas de tiempo
- Gráficos interactivos para visualizar datos de conversiones
- Modo oscuro/claro para mejor experiencia visual
- API REST para acceso programático
- Interfaz web responsive con Thymeleaf y Bootstrap

## APIs utilizadas

- ExchangeRate-API: Para conversión entre monedas fiduciarias
- CoinMarketCap: Para información y conversión de criptomonedas

## Tecnologías

- Java 11
- Spring Boot 2.7
- Spring Data JPA
- H2 Database
- Thymeleaf
- Bootstrap 5
- Chart.js
- Lombok
- Gson

## Requisitos

- Java 11 o superior
- Maven 3.6 o superior
- Claves API para ExchangeRate-API y CoinMarketCap

## Configuración

1. Clona este repositorio
2. Crea un archivo `.env` basado en `env.example`
3. Añade tus propias claves API de ExchangeRate-API y CoinMarketCap:
   ```
   API_EXCHANGERATE_KEY=tu_clave_api_exchangerate
   API_COINMARKETCAP_KEY=tu_clave_api_coinmarketcap
   ```

## Ejecución

### Usando Java/Maven

Para ejecutar la aplicación en modo desarrollo:

```bash
mvn spring-boot:run
```

Para generar el archivo JAR y ejecutarlo:

```bash
mvn clean package
java -jar target/conversor-monedas-1.0-SNAPSHOT.jar
```

### Usando Docker

Para construir y ejecutar usando Docker Compose:

```bash
docker-compose up -d --build
```

La aplicación estará disponible en: http://localhost:9080

## API REST

La aplicación expone los siguientes endpoints REST:

- `GET /api/monedas`: Lista todas las monedas disponibles
- `GET /api/convertir?monedaOrigen=USD&monedaDestino=EUR&cantidad=100`: Convierte entre monedas fiduciarias
- `GET /api/convertir-cripto?criptomoneda=BTC&monedaFiat=USD&cantidad=1`: Convierte entre criptomonedas y monedas fiduciarias
- `GET /api/historial`: Obtiene el historial completo de conversiones
- `GET /api/historial/{tipo}`: Obtiene el historial filtrado por tipo (MONEDA o CRIPTO)
- `GET /api/graf/historial-tasas?monedaOrigen=USD&monedaDestino=EUR`: Obtiene datos históricos de tasas de cambio
- `GET /api/graf/distribucion-monedas`: Obtiene datos para el gráfico de distribución de monedas
- `GET /api/graf/distribucion-tipos`: Obtiene datos para el gráfico de distribución por tipos de conversión

## Despliegue en producción

### Usando Docker en DigitalOcean

1. Crea una cuenta en DigitalOcean
2. Crea un Droplet con Docker preinstalado
3. Clona este repositorio en el Droplet
4. Configura el archivo `.env` con tus claves API
5. Ejecuta `docker-compose up -d`

La aplicación estará disponible en la IP del Droplet en el puerto 9080.

### Consideraciones de seguridad

- **Nunca** incluyas claves API directamente en el código
- Usa siempre variables de entorno para credenciales y configuraciones sensibles
- Usa un archivo `.env` local para desarrollo y configuraciones de entorno en producción
- Considera usar un proxy inverso como Nginx para HTTPS en producción

## Estructura del proyecto

La aplicación sigue los principios SOLID y está estructurada en:

- `model`: Entidades y enumeraciones del dominio
- `repository`: Interfaces de acceso a datos
- `service`: Lógica de negocio
- `controller`: Controladores REST y web
- `http`: Clientes HTTP para APIs externas

## Licencia

Este proyecto está licenciado bajo MIT License. 