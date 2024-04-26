package uk.gov.cabinetoffice.csl.service.client.csrs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsrsServiceDataTransformer {

    public List<OrganisationalUnit> transformOrganisations(List<OrganisationalUnit> organisationalUnitDtos) {
        List<OrganisationalUnit> tree = this.transformOrgsIntoTree(organisationalUnitDtos);
        tree.forEach(OrganisationalUnit::applyAgencyTokenToDescendants);
        List<OrganisationalUnit> flattened = tree.stream().flatMap(o -> o.getHierarchyAsFlatList().stream()).collect(Collectors.toList());
        flattened.sort(Comparator.comparing(OrganisationalUnit::getFormattedName));
        return flattened;
    }

    public List<OrganisationalUnit> transformOrgsIntoTree(List<OrganisationalUnit> organisationalUnitDtos) {
        Map<Integer, Integer> orgIdMap = new HashMap<>();
        List<OrganisationalUnit> roots = new ArrayList<>();
        for (int i = 0; i < organisationalUnitDtos.size(); i++) {
            OrganisationalUnit organisationalUnitDto = organisationalUnitDtos.get(i);
            orgIdMap.put(organisationalUnitDto.getId(), i);
        }
        organisationalUnitDtos.forEach(o -> {
            if (o.getParentId() != null) {
                int index = orgIdMap.get(o.getParentId());
                organisationalUnitDtos.get(index).addDescendant(o);
            } else {
                roots.add(o);
            }
        });
        return roots;
    }
}
