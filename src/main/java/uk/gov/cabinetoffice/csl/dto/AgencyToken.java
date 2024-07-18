package uk.gov.cabinetoffice.csl.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Data
@NoArgsConstructor
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgencyToken implements Serializable {

    private String uid;
    private String token;
    private Long capacity;
    private List<Domain> agencyDomains = emptyList();

    private String domain;
    private String org;

    private Long capacityUsed;

    public AgencyToken(String uid, String token, Long capacity, List<Domain> agencyDomains) {
        this.uid = uid;
        this.token = token;
        this.capacity = capacity;
        this.agencyDomains = agencyDomains;
    }

    public AgencyToken(String domain, String token, String org) {
        this.domain = domain;
        this.token = token;
        this.org = org;
    }

    public AgencyToken(Long capacityUsed) {
        this.capacityUsed = capacityUsed;
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
                .anyMatch(d -> d.getDomain().equalsIgnoreCase(domain));
    }
}
