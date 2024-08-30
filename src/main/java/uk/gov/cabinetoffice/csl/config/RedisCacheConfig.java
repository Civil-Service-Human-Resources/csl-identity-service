package uk.gov.cabinetoffice.csl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return (builder) -> builder
                .withCacheConfiguration("allowDomains",
                        defaultCacheConfig()
                                .disableCachingNullValues()
                                .entryTtl(ofSeconds(allowListDomainsCacheTTlSeconds))
                                .prefixCacheNameWith("csl-identity_"))
                .withCacheConfiguration("organisations",
                        defaultCacheConfig()
                                .disableCachingNullValues()
                                .entryTtl(ofSeconds(organisationsCacheTTlSeconds))
                                .prefixCacheNameWith("csl-identity_"));
    }
}
