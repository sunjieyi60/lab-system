package xyz.jasenon.lab.mqtt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * RestTemplate 使用与当前应用一致的 ObjectMapper（含 JavaTimeModule 等），
 * 避免上报 /log/alarm 时 JSON 序列化格式与 service 端反序列化不一致导致 400。
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper) {
        RestTemplate rest = new RestTemplate();
        List<org.springframework.http.converter.HttpMessageConverter<?>> converters = new ArrayList<>(rest.getMessageConverters());
        // 用配置了 JavaTimeModule 的 ObjectMapper 替换默认的 Jackson 转换器
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter custom = new MappingJackson2HttpMessageConverter();
                custom.setObjectMapper(objectMapper);
                converters.set(i, custom);
                break;
            }
        }
        rest.setMessageConverters(converters);
        return rest;
    }
}
