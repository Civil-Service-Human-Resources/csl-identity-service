package uk.gov.cabinetoffice.csl.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Data
@NoArgsConstructor
public class TokenRequest implements Serializable {

    private String domain;
    private String token;
    private String org;

    public boolean hasData() {
        return isNotBlank(this.getDomain())
                && isNotBlank(this.getToken())
                && isNotBlank(this.getOrg());
    }
}
