package uk.gov.cabinetoffice.csl.dto;

import lombok.Data;

@Data
public class OrganisationalUnit {
    protected String name;
    protected String href;
    protected String abbreviation;
    protected String formattedName;
    private String code;
}
