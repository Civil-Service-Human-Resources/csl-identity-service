package uk.gov.cabinetoffice.csl.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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
}
