package uk.gov.cabinetoffice.csl.repository;

import org.springframework.stereotype.Repository;
import uk.gov.cabinetoffice.csl.service.ICompoundRole;

import java.util.List;

@Repository
public class CompoundRolesRepository implements CompoundRoles {

    @Override
    public List<String> getRoles(ICompoundRole role) {
        return role.getRoles();
    }
}
