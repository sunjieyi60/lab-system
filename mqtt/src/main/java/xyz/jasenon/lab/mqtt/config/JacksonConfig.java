package xyz.jasenon.lab.mqtt.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            JavaTimeModule module = new JavaTimeModule();

            // 使用自定义的 null-safe 序列化器
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            ) {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider)
                        throws IOException {
                    if (value == null) {
                        gen.writeNull();
                    } else {
                        super.serialize(value, gen, provider);
                    }
                }
            });

            module.addDeserializer(LocalDateTime.class,
                    new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            module.addSerializer(LocalDate.class, new LocalDateSerializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
            ) {
                @Override
                public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider)
                        throws IOException {
                    if (value == null) {
                        gen.writeNull();
                    } else {
                        super.serialize(value, gen, provider);
                    }
                }
            });

            module.addDeserializer(LocalDate.class,
                    new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );

            module.addSerializer(LocalTime.class, new LocalTimeSerializer(
                    DateTimeFormatter.ofPattern("HH:mm:ss")
            ) {
                @Override
                public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider provider)
                        throws IOException {
                    if (value == null) {
                        gen.writeNull();
                    } else {
                        super.serialize(value, gen, provider);
                    }
                }
            });

            module.addDeserializer(LocalTime.class,
                    new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss"))
            );

            builder.modules(module);
            // 确保 null 值被序列化为 null，而不是报错
            builder.serializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);
        };
    }

}
