package uk.gov.cabinetoffice.csl.controller.agencytoken;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class VerifyTokenForm {
    @NotBlank(message = "{validation.emailUpdatedEnterToken.organisation.NotBlank}")
    private String organisation;
    @NotBlank(message = "{validation.emailUpdatedEnterToken.token.NotBlank}")
    private String token;
    private String uid;
    private String code;
}
