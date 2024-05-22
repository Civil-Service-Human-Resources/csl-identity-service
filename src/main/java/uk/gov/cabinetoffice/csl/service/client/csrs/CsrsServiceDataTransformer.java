package uk.gov.cabinetoffice.csl.service.client.csrs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;

import java.util.*;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsrsServiceDataTransformer {

    public List<OrganisationalUnit> transformOrganisations(List<OrganisationalUnit> organisationalUnits) {
        List<OrganisationalUnit> tree = this.transformOrgsIntoTree(organisationalUnits);
        tree.forEach(OrganisationalUnit::applyAgencyTokenToDescendants);
        return tree
                .stream()
                .flatMap(o -> o.getHierarchyAsFlatList().stream())
                .sorted(comparing(OrganisationalUnit::getFormattedName))
                .collect(toList());
    }

    public List<OrganisationalUnit> transformOrgsIntoTree(List<OrganisationalUnit> organisationalUnits) {
        Map<Integer, Integer> orgIdMap = new HashMap<>();
        List<OrganisationalUnit> roots = new ArrayList<>();
        for (int i = 0; i < organisationalUnits.size(); i++) {
            OrganisationalUnit organisationalUnit = organisationalUnits.get(i);
            orgIdMap.put(organisationalUnit.getId(), i);
        }
        organisationalUnits.forEach(o -> {
            if (o.getParentId() != null) {
                int index = orgIdMap.get(o.getParentId());
                organisationalUnits.get(index).addDescendant(o);
            } else {
                roots.add(o);
            }
        });
        return roots;
    }
}
