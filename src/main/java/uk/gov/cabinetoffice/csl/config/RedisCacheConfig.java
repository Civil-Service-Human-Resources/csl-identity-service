package uk.gov.cabinetoffice.csl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.util.HashMap;
import java.util.Map;

import static java.time.Duration.ofSeconds;
import static org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig;

@Configuration
public class RedisCacheConfig {

    @Value("${civilServantRegistry.allowListDomains.cache.ttlSeconds}")
    private int allowListDomainsCacheTTlSeconds;

    @Value("${civilServantRegistry.organisations.cache.ttlSeconds}")
    private int organisationsCacheTTlSeconds;

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {

        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        RedisCacheConfiguration defaultCacheConfig = defaultCacheConfig().disableCachingNullValues();
        configMap.put("allowListDomains", defaultCacheConfig.entryTtl(ofSeconds(allowListDomainsCacheTTlSeconds)));
        configMap.put("organisations", defaultCacheConfig.entryTtl(ofSeconds(organisationsCacheTTlSeconds)));

        return (builder) -> builder
                .withInitialCacheConfigurations(configMap);
    }
}
