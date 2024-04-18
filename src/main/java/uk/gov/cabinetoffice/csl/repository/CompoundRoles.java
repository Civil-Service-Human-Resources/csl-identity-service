package uk.gov.cabinetoffice.csl.repository;

import uk.gov.cabinetoffice.csl.service.ICompoundRole;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

public interface CompoundRoles {

    List<String> getRoles(ICompoundRole role);

    default List<String> getRoles(Collection<ICompoundRole> roles) {
        return roles
                .stream()
                .flatMap(r -> this.getRoles(r)
                        .stream())
                        .collect(toList());
    }
}
