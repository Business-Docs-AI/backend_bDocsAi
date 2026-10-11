FROM eclipse-temurin:25-jre

WORKDIR /app

COPY .tmp-docker-jar/app.jar app.jar

EXPOSE 8080


# A rede padrão do Docker Compose aqui não roteia IPv6 — sem isso, a JVM resolve hosts
# externos (ex.: api.anthropic.com) só para o endereço AAAA e a conexão trava/falha com
# UnresolvedAddressException.
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
