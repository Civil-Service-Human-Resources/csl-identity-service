package uk.gov.cabinetoffice.csl.config.FrontendAssets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FrontendAssetConfig {

    @Value("${templates.assetCdn}")
    private String assetCdn;

    @Bean(name = "assetCdnService")
    public AssetCdnService assetCdnService() {
        return () -> assetCdn;
    }
}
