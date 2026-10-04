# ==========================================
# Etapa 1: Build de la aplicación con Maven
# ==========================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copiar wrapper y pom primero para cachear dependencias
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copiar el código fuente y compilar el JAR ejecutable
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Etapa 2: Runtime ligero y seguro
# ==========================================
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Crear usuario y grupo de sistema sin privilegios de root
RUN addgroup -S sepisac && adduser -S sepisac -G sepisac

# Copiar el JAR empaquetado desde la etapa builder
COPY --from=builder /app/target/sepisac-backend-*.jar app.jar

# Configurar permisos
RUN chown -R sepisac:sepisac /app
USER sepisac

# Puerto del servicio
EXPOSE 8080

# Parámetros JVM optimizados para contenedores y servidores de recursos moderados (Lightsail 2GB)
ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseContainerSupport -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
