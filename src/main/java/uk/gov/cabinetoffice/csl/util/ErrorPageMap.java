package uk.gov.cabinetoffice.csl.util;

import java.util.Map;

public final class ErrorPageMap {
    public static final Map<Integer, String> ERROR_PAGES =
            Map.of(400, "400", 401, "401", 403, "403", 404, "404", 500, "500");
}
