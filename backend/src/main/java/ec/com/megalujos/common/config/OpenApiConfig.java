package ec.com.megalujos.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("MEGALUJOS API — Documentación del Sistema")
                        .version("1.0.0")
                        .description("API REST para el sistema integral MEGALUJOS: Catálogo automotriz, Inventario, POS, Caja, Servicios y Facturación Electrónica SRI.")
                        .contact(new Contact().name("Megalujos Soporte Técnico").email("soporte@megalujos.com.ec"))
                        .license(new License().name("Propietario MEGALUJOS").url("https://megalujos.com.ec")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
