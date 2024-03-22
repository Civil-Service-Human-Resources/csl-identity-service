package uk.gov.cabinetoffice.csl.service;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CompoundRoleRepositoryImpl implements CompoundRoleRepository {

    @Override
    public List<String> getRoles(ICompoundRole role) {
        return role.getRoles();
    }
}
