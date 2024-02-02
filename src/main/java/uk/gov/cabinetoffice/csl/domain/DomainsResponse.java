package uk.gov.cabinetoffice.csl.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DomainsResponse {

    private List<Domain> domains;

    @JsonProperty("_embedded")
    public void setDomains(Map<String, List<Domain>> embeddedDomains) {
        this.domains = embeddedDomains.get("domains");
    }
}
