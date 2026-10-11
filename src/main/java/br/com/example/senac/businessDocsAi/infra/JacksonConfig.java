package br.com.example.senac.businessDocsAi.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara o {@link ObjectMapper} explicitamente porque, neste projeto, o bean
 * autoconfigurado do Jackson não fica disponível a tempo para os componentes de segurança
 * (que o Spring Security inicializa cedo no bootstrap) nem para os serviços que o injetam
 * diretamente — {@code findAndRegisterModules()} replica o mesmo auto-registro de módulos
 * (ex.: suporte a LocalDateTime) que o Spring Boot faria por padrão.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
