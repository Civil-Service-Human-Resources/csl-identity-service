package uk.gov.cabinetoffice.csl.service;

import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class CompoundRoleRepositoryImpl implements CompoundRoleRepository {
    @Override
    public List<String> getReportingRoles() {
        return Arrays.asList("ORGANISATION_REPORTER",
                             "PROFESSION_REPORTER",
                             "CSHR_REPORTER",
                             "DOWNLOAD_BOOKING_FEED",
                             "SUPPLIER_REPORTER",
                             "KORNFERRY_SUPPLIER_REPORTER");
    }
}
