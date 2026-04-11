package xyz.jasenon.lab.cache.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import xyz.jasenon.lab.cache.exception.CacheException;

import java.io.*;
import java.lang.reflect.Type;

/**
 * 序列化工具类
 *
 * @author Jasenon
 */
@Slf4j
public class SerializationUtil {

    private final ObjectMapper objectMapper;
    private final String serializer;

    public SerializationUtil(ObjectMapper objectMapper, String serializer) {
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
        this.serializer = serializer != null ? serializer : "jackson";
    }

    /**
     * 创建默认 ObjectMapper
     */
    private ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    /**
     * 序列化对象为字节数组
     *
     * @param value 对象
     * @return 字节数组
     */
    public byte[] serialize(Object value) {
        if (value == null) {
            return new byte[0];
        }

        try {
            if ("jackson".equalsIgnoreCase(serializer)) {
                return objectMapper.writeValueAsBytes(value);
            } else {
                return jdkSerialize(value);
            }
        } catch (Exception e) {
            log.error("序列化失败: {}", e.getMessage(), e);
            throw new CacheException("序列化失败", e);
        }
    }

    /**
     * 序列化对象为字符串
     *
     * @param value 对象
     * @return JSON 字符串
     */
    public String serializeToString(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return (String) value;
        }

        try {
            if ("jackson".equalsIgnoreCase(serializer)) {
                return objectMapper.writeValueAsString(value);
            } else {
                return new String(jdkSerialize(value));
            }
        } catch (Exception e) {
            log.error("序列化失败: {}", e.getMessage(), e);
            throw new CacheException("序列化失败", e);
        }
    }

    /**
     * 反序列化字节数组为对象
     *
     * @param data  字节数组
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 对象
     */
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            if ("jackson".equalsIgnoreCase(serializer)) {
                return objectMapper.readValue(data, clazz);
            } else {
                return jdkDeserialize(data);
            }
        } catch (Exception e) {
            log.error("反序列化失败: {}", e.getMessage(), e);
            throw new CacheException("反序列化失败", e);
        }
    }

    /**
     * 反序列化字符串为对象（通用类型）
     *
     * @param json JSON 字符串
     * @param <T>  类型参数
     * @return 对象
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return (T) objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.error("反序列化失败: {}", e.getMessage(), e);
            throw new CacheException("反序列化失败", e);
        }
    }

    public <T> T deserialize(String json, TypeReference<T> ref){
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json,ref);
        } catch (Exception e) {
            log.error("反序列化失败: {}", e.getMessage(), e);
            throw new CacheException("反序列化失败", e);
        }
    }

    /**
     * 反序列化字符串为对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 对象
     */
    public <T> T deserialize(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        if (clazz == String.class) {
            return clazz.cast(json);
        }

        try {
            if ("jackson".equalsIgnoreCase(serializer)) {
                return objectMapper.readValue(json, clazz);
            } else {
                return jdkDeserialize(json.getBytes());
            }
        } catch (Exception e) {
            log.error("反序列化失败: {}", e.getMessage(), e);
            throw new CacheException("反序列化失败", e);
        }
    }

    /**
     * 反序列化为集合类型
     *
     * @param json         JSON 字符串
     * @param elementClass 元素类型
     * @param <T>          元素类型参数
     * @return 集合
     */
    public <T> T deserializeCollection(String json, Class<?> collectionClass, Class<?> elementClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            JavaType javaType = objectMapper.getTypeFactory()
                    .constructParametricType(collectionClass, elementClass);
            return objectMapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("反序列化失败: {}", e.getMessage(), e);
            throw new CacheException("反序列化失败", e);
        }
    }

    /**
     * 反序列化为 Map 类型
     *
     * @param json      JSON 字符串
     * @param keyClass  键类型
     * @param valueClass 值类型
     * @param <K>       键类型参数
     * @param <V>       值类型参数
     * @return Map
     */
    public <K, V> java.util.Map<K, V> deserializeMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        try {
            JavaType javaType = objectMapper.getTypeFactory()
                    .constructMapType(java.util.Map.class, keyClass, valueClass);
            return objectMapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error("反序列化失败: {}", e.getMessage(), e);
            throw new CacheException("反序列化失败", e);
        }
    }

    /**
     * JDK 序列化
     */
    private byte[] jdkSerialize(Object value) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(value);
            return baos.toByteArray();
        }
    }

    /**
     * JDK 反序列化
     */
    @SuppressWarnings("unchecked")
    private <T> T jdkDeserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        }
    }

    /**
     * 获取 ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
