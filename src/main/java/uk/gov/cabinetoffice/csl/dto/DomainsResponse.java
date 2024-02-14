package uk.gov.cabinetoffice.csl.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import uk.gov.cabinetoffice.csl.dto.Domain;

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
