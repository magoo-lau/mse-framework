package vip.mseapp.web.config;

import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 统一序列化规则（Boot 4 / Jackson 3，JsonMapper 定制）：
 * <ul>
 *   <li>Long → String：雪花 ID 超过 JS Number 安全整数（2^53），防止前端精度丢失；</li>
 *   <li>LocalDateTime → yyyy-MM-dd HH:mm:ss。</li>
 * </ul>
 */
public class DefaultJacksonCustomizer implements JsonMapperBuilderCustomizer {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    @Override
    public void customize(JsonMapper.Builder builder) {
        DateTimeFormatter dateTime = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter date = DateTimeFormatter.ofPattern(DATE_PATTERN);
        // Boot 4 / Jackson 3 移除了 builder.serializerByType，按类型注册序列化器统一走 SimpleModule
        builder.addModule(new SimpleModule("mse-web-json")
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(Long.TYPE, ToStringSerializer.instance)
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTime))
                .addSerializer(LocalDate.class, new LocalDateSerializer(date))
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTime))
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(date)));
    }
}
