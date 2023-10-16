package uk.gov.cabinetoffice.csl.utils;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.support.WebTestUtils;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public class CsrfRequestPostProcessor implements RequestPostProcessor {

    private boolean useInvalidToken = false;

    private boolean asHeader = false;

    public static CsrfRequestPostProcessor csrf() {
        return new CsrfRequestPostProcessor();
    }

    @Override
    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
        CsrfTokenRepository repository = WebTestUtils.getCsrfTokenRepository(request);
        CsrfToken token = repository.generateToken(request);
        repository.saveToken(token, request, new MockHttpServletResponse());
        String tokenValue = useInvalidToken ? "invalid" + token.getToken() : token
                .getToken();
        if (asHeader) {
            request.setAttribute(token.getHeaderName(), token);
        }
        else {
            request.setAttribute(token.getParameterName(), token);
        }
        return request;
    }

    public RequestPostProcessor invalidToken() {
        this.useInvalidToken = true;
        return this;
    }

    public RequestPostProcessor asHeader() {
        this.asHeader = true;
        return this;
    }

}
