package uk.gov.cabinetoffice.csl.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum CompoundRole implements ICompoundRole {

    REPORTER(Arrays.asList("ORGANISATION_REPORTER",
            "PROFESSION_REPORTER",
            "CSHR_REPORTER",
            "DOWNLOAD_BOOKING_FEED",
            "SUPPLIER_REPORTER",
            "KORNFERRY_SUPPLIER_REPORTER")),
    UNRESTRICTED_ORGANISATION(Collections.singletonList("UNRESTRICTED_ORGANISATION"));

    private final List<String> roles;

    CompoundRole(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getRoles() {
        return roles;
    }
}
