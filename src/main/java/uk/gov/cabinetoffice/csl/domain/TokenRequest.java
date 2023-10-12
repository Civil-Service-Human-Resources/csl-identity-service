package uk.gov.cabinetoffice.csl.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class TokenRequest implements Serializable {

    private static long serialVersionUID = 1l;

    private String domain;
    private String token;
    private String org;

}
