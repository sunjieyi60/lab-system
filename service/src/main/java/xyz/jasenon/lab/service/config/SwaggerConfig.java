package xyz.jasenon.lab.service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.info.Contact;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Lab System Service API",
                version = "v1",
                description = "实验室管理系统服务端接口",
                contact = @Contact(name = "Jasenon"),
                license = @License(name = "Apache-2.0")
        )
)
@Configuration
public class SwaggerConfig {
}