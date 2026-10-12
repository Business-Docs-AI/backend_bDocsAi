# ---------- Etapa 1: build ----------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copia primeiro só o necessário para resolver as dependências, aproveitando o cache de
# camadas do Docker: essa camada só é invalidada quando build.gradle/settings.gradle/wrapper
# mudam, não a cada alteração em src/.
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle settings.gradle ./

# O checkout no host pode trazer CRLF (ex.: autocrlf no Windows) e sem permissão de
# execução — o shebang do gradlew quebra num container Linux se isso não for corrigido.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

RUN ./gradlew --no-daemon dependencies

COPY src src

# O plugin do Spring Boot só gera o "-plain.jar" (sem Main-Class/dependências) quando a
# task `jar` está habilitada; removemos por garantia para nunca copiar o jar errado.
RUN ./gradlew --no-daemon bootJar -x test \
    && rm -f build/libs/*-plain.jar

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# A rede padrão do Docker Compose aqui não roteia IPv6 — sem isso, a JVM resolve hosts
# externos (ex.: api.anthropic.com) só para o endereço AAAA e a conexão trava/falha com
# UnresolvedAddressException.
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
