FROM eclipse-temurin:11-jdk AS build

WORKDIR /app

# Copiar archivos de Maven
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Construir la aplicación sin ejecutar pruebas
RUN ./mvnw package -DskipTests

# Usar una imagen más ligera para producción
FROM eclipse-temurin:11-jre

WORKDIR /app

# Copiar el archivo JAR construido
COPY --from=build /app/target/*.jar app.jar

# Variables de entorno
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=9080

# Exponer el puerto
EXPOSE ${PORT}

# Comando para iniciar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"] 