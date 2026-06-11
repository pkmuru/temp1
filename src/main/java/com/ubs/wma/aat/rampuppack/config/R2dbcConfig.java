package com.ubs.wma.aat.rampuppack.config;

import com.ubs.wma.aat.rampuppack.domain.RampUpPackStatus;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

/**
 * R2DBC data-mapping configuration:
 * <ul>
 *   <li>{@link EnableR2dbcAuditing} populates {@code @CreatedDate}/{@code @LastModifiedDate} on save;</li>
 *   <li>custom converters map {@link RampUpPackStatus} to/from its {@code VARCHAR} column value.</li>
 * </ul>
 * Defining a {@link R2dbcCustomConversions} bean makes Spring Boot back off its default one.
 */
@Configuration(proxyBeanMethods = false)
@EnableR2dbcAuditing
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(
                PostgresDialect.INSTANCE,
                new StatusReadingConverter(),
                new StatusWritingConverter());
    }

    @ReadingConverter
    static final class StatusReadingConverter implements Converter<String, RampUpPackStatus> {
        @Override
        public RampUpPackStatus convert(String source) {
            return RampUpPackStatus.valueOf(source);
        }
    }

    @WritingConverter
    static final class StatusWritingConverter implements Converter<RampUpPackStatus, String> {
        @Override
        public String convert(RampUpPackStatus source) {
            return source.name();
        }
    }
}
