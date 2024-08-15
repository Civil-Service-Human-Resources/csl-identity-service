package uk.gov.cabinetoffice.csl.controller.error;

import java.util.Map;

public final class ErrorPageMap {
    public static final Map<Integer, String> ERROR_PAGES =
            Map.of(
                    400, "error/400",
                    401, "error/401",
                    403, "error/403",
                    404, "error/404",
                    500, "error/500",
                    503, "error/503"
            );
}
