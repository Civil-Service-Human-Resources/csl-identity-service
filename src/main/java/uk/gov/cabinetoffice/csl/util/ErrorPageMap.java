package uk.gov.cabinetoffice.csl.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ErrorPageMap {
    public static final Map<Integer, String> ERROR_PAGES;
    static {
        Map<Integer, String> pagesMap = new HashMap<>();
        pagesMap.put(400, "400");
        pagesMap.put(401, "401");
        pagesMap.put(403, "403");
        pagesMap.put(404, "404");
        pagesMap.put(500, "500");
        ERROR_PAGES = Collections.unmodifiableMap(pagesMap);
    }
}
