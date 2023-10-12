package uk.gov.cabinetoffice.csl.domain;

import lombok.Data;

@Data
public class OrganisationalUnitDto {
    protected String name;
    protected String href;
    protected String abbreviation;
    protected String formattedName;
    private String code;
}
