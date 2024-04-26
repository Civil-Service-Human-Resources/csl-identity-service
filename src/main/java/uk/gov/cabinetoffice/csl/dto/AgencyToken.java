package uk.gov.cabinetoffice.csl.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgencyToken implements Serializable {
    private String uid;
    private Long capacity;
    private Long capacityUsed;
    private String domain;
    private String token;
    private String org;
    private List<Domain> agencyDomains = emptyList();

    public AgencyToken(Long capacityUsed) {
        this.capacityUsed = capacityUsed;
    }

    public AgencyToken(String domain, String token, String org) {
        this.domain = domain;
        this.token = token;
        this.org = org;
    }

    @JsonIgnore
    public boolean hasData() {
        return isNotBlank(this.getDomain())
                && isNotBlank(this.getToken())
                && isNotBlank(this.getOrg());
    }

    public List<Domain> getAgencyDomains() {
        if (agencyDomains == null) {
            agencyDomains = emptyList();
        }
        return agencyDomains;
    }

    public boolean isDomainAssignedToAgencyToken(String domain) {
        return this.getAgencyDomains()
                .stream()
                .anyMatch(d -> d.getDomain().equals(domain));
    }
}
