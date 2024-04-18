package uk.gov.cabinetoffice.csl.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Value("${clock.zoneId}")
    private String clockZoneId;

    @Bean
    public Clock getClock() {
        if(StringUtils.isBlank(clockZoneId)) {
            return Clock.systemDefaultZone();
        }
        return Clock.system(ZoneId.of(clockZoneId.trim()));
    }
}
