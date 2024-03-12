package uk.gov.cabinetoffice.csl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerificationCodeDetermination {
    private String email;
    private VerificationCodeType verificationCodeType;
}
