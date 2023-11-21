package uk.gov.cabinetoffice.csl.service.client;

import org.springframework.http.RequestEntity;

public interface IHttpClient {
    <T, R> T executeRequest(RequestEntity<R> request, Class<T> responseClass);
}
