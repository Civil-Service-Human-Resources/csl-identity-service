package uk.gov.cabinetoffice.csl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.cabinetoffice.csl.dto.AgencyToken;
import uk.gov.cabinetoffice.csl.dto.Domain;
import uk.gov.cabinetoffice.csl.dto.OrganisationalUnit;
import uk.gov.cabinetoffice.csl.service.client.csrs.CsrsServiceDataTransformer;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("no-redis")
public class CsrsServiceDataTransformerTest {

    private CsrsServiceDataTransformer csrsServiceDataTransformer;

    @BeforeEach
    public void setUp() {
        csrsServiceDataTransformer = new CsrsServiceDataTransformer();
    }

    private List<OrganisationalUnit> getSampleOrganisations() {
        OrganisationalUnit parent = new OrganisationalUnit();
        parent.setId(1);
        parent.setFormattedName("PARENT");

        OrganisationalUnit agencyChild = new OrganisationalUnit();
        agencyChild.setId(3);
        agencyChild.setParentId(1);
        agencyChild.setFormattedName("PARENT | AGENCY_CHILD");

        AgencyToken agencyToken1 = new AgencyToken();
        agencyToken1.setUid("uid1");
        agencyToken1.setToken("token1");
        agencyToken1.setCapacity(1L);

        List<Domain> domains1 = asList(new Domain(1L, "domain1.com"), new Domain(1L, "domain2.com"));
        agencyToken1.setAgencyDomains(domains1);
        agencyChild.setAgencyToken(agencyToken1);

        OrganisationalUnit agencyGrandchild = new OrganisationalUnit();
        agencyGrandchild.setId(6);
        agencyGrandchild.setParentId(3);
        agencyGrandchild.setFormattedName("PARENT | AGENCY_CHILD | AGENCY_GRANDCHILD");

        OrganisationalUnit parent2 = new OrganisationalUnit();
        parent2.setId(7);
        parent2.setFormattedName("PARENT2");
        parent2.setDomains(singletonList(new Domain(1L, "domain2.com")));

        OrganisationalUnit child2 = new OrganisationalUnit();
        child2.setId(10);
        child2.setParentId(7);
        child2.setFormattedName("PARENT2 | CHILD2");
        child2.setDomains(singletonList(new Domain(1L, "domain3.com")));

        OrganisationalUnit otherAgency = new OrganisationalUnit();
        otherAgency.setId(14);
        otherAgency.setFormattedName("OTHER_AGENCY");

        AgencyToken agencyToken2 = new AgencyToken();
        agencyToken2.setUid("uid2");
        agencyToken2.setToken("token2");
        agencyToken2.setCapacity(1L);
        List<Domain> domains2 = List.of(new Domain(1L, "domain2.com"));
        agencyToken2.setAgencyDomains(domains2);
        agencyChild.setAgencyToken(agencyToken2);

        OrganisationalUnit otherAgencyChild = new OrganisationalUnit();
        otherAgencyChild.setId(20);
        otherAgencyChild.setParentId(14);
        otherAgencyChild.setFormattedName("OTHER_AGENCY | CHILD");

        AgencyToken agencyToken3 = new AgencyToken();
        agencyToken3.setUid("uid3");
        agencyToken3.setToken("token3");
        agencyToken3.setCapacity(1L);
        List<Domain> domains3 = singletonList(new Domain(1L, "domain2.com"));
        agencyToken2.setAgencyDomains(domains3);

        otherAgencyChild.setAgencyToken(agencyToken3);

        return asList(child2, otherAgencyChild, agencyGrandchild, parent2, agencyChild, parent, otherAgency);
    }

    @Test
    public void shouldTransformOrganisations() {
        List<OrganisationalUnit> result = csrsServiceDataTransformer.transformOrganisations(getSampleOrganisations());
        assertEquals(7, result.size());
        assertEquals(result.get(0).getFormattedName(), "OTHER_AGENCY");
        assertEquals(result.get(0).getAgencyToken().getToken(), "token2");
        assertEquals(result.get(1).getFormattedName(), "OTHER_AGENCY | CHILD");
        assertEquals(result.get(1).getAgencyToken().getToken(), "token3");
        assertEquals(result.get(2).getFormattedName(), "PARENT");
        assertEquals(result.get(3).getFormattedName(), "PARENT | AGENCY_CHILD");
        assertEquals(result.get(3).getAgencyToken().getToken(), "token1");
        assertEquals(result.get(4).getFormattedName(), "PARENT | AGENCY_CHILD | AGENCY_GRANDCHILD");
        assertEquals(result.get(4).getAgencyToken().getToken(), "token1");
        assertEquals(result.get(5).getFormattedName(), "PARENT2");
        assertEquals(result.get(6).getFormattedName(), "PARENT2 | CHILD2");
    }
}
