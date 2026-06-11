package com.ubs.wma.aat.rampuppack.config;

import com.ubs.wma.aat.rampuppack.domain.BatchStatus;
import com.ubs.wma.aat.rampuppack.domain.EmailStatus;
import io.r2dbc.postgresql.codec.Json;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * R2DBC data-mapping configuration:
 * <ul>
 *   <li>{@link EnableR2dbcAuditing} populates {@code @CreatedDate}/{@code @LastModifiedDate} and
 *       {@code @CreatedBy}/{@code @LastModifiedBy} on save (auditor: see {@code AuditingConfig});</li>
 *   <li>enum converters map {@link EmailStatus}/{@link BatchStatus} to/from their {@code VARCHAR}
 *       column values (the Postgres driver would otherwise try a native enum type);</li>
 *   <li>JSON converters map {@code Map<String, String>} merge fields to/from {@code JSONB} via the
 *       driver's {@link Json} codec and Jackson 3.</li>
 * </ul>
 * Defining a {@link R2dbcCustomConversions} bean makes Spring Boot back off its default one.
 */
@Configuration(proxyBeanMethods = false)
@EnableR2dbcAuditing
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        return R2dbcCustomConversions.of(
                PostgresDialect.INSTANCE,
                new EmailStatusReadingConverter(),
                new EmailStatusWritingConverter(),
                new BatchStatusReadingConverter(),
                new BatchStatusWritingConverter(),
                new MergeFieldsReadingConverter(jsonMapper),
                new MergeFieldsWritingConverter(jsonMapper));
    }

    @ReadingConverter
    static final class EmailStatusReadingConverter implements Converter<String, EmailStatus> {
        @Override
        public EmailStatus convert(String source) {
            return EmailStatus.valueOf(source);
        }
    }

    @WritingConverter
    static final class EmailStatusWritingConverter implements Converter<EmailStatus, String> {
        @Override
        public String convert(EmailStatus source) {
            return source.name();
        }
    }

    @ReadingConverter
    static final class BatchStatusReadingConverter implements Converter<String, BatchStatus> {
        @Override
        public BatchStatus convert(String source) {
            return BatchStatus.valueOf(source);
        }
    }

    @WritingConverter
    static final class BatchStatusWritingConverter implements Converter<BatchStatus, String> {
        @Override
        public String convert(BatchStatus source) {
            return source.name();
        }
    }

    @ReadingConverter
    static final class MergeFieldsReadingConverter implements Converter<Json, Map<String, String>> {
        private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

        private final JsonMapper jsonMapper;

        MergeFieldsReadingConverter(JsonMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
        }

        @Override
        public Map<String, String> convert(Json source) {
            return jsonMapper.readValue(source.asString(), MAP_TYPE);
        }
    }

    @WritingConverter
    static final class MergeFieldsWritingConverter implements Converter<Map<String, String>, Json> {
        private final JsonMapper jsonMapper;

        MergeFieldsWritingConverter(JsonMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
        }

        @Override
        public Json convert(Map<String, String> source) {
            return Json.of(jsonMapper.writeValueAsString(source));
        }
    }
}
