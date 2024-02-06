package uk.gov.cabinetoffice.csl.util;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {

    String id() default "123";

    String uid() default "uid123";

    String email() default "test@example.com";

    String password() default "Password123";
}
