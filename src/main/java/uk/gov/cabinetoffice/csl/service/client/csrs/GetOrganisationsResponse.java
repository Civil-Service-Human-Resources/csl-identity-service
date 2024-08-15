package uk.gov.cabinetoffice.csl.service.client.csrs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetOrganisationsResponse {
    private List<OrganisationalUnit> content;
    private Integer page;
    private Integer totalPages;
    private Integer totalElements;
    private Integer size;
}
