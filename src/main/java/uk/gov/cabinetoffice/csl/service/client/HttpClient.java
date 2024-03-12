package uk.gov.cabinetoffice.csl.service.client;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;

@Slf4j
@AllArgsConstructor
public class HttpClient implements IHttpClient {

    private final RestTemplate restTemplate;

    @Override
    public <T, R> T executeRequest(RequestEntity<R> request, Class<T> responseClass) {
        try {
            log.debug("Sending request: {}", request);
            ResponseEntity<T> response = restTemplate.exchange(request, responseClass);
            log.debug("Request response: {}", response);
            return response.getBody();
        } catch (RestClientResponseException e) {
            String msg = String.format("Error sending '%s' request to endpoint", request.getMethod());
            if (request.getBody() != null) {
                msg = String.format("%s Body was: %s.", msg, request.getBody().toString());
            }
            msg = String.format("%s Error was: %s", msg, e.getMessage());
            log.error(msg);
            throw new GenericServerException("System error");
        }
    }
}
