package br.ufrn.dimap.siglab.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SIGLab API")
                        .version("1.0.0")
                        .description("Research Group/Lab management API")
                        .contact(new Contact()
                                .name("DIMAP / UFRN")
                                .url("https://dimap.ufrn.br")));
    }
}
