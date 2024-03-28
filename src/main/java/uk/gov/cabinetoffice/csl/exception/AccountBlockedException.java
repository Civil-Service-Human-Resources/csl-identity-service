package uk.gov.cabinetoffice.csl.exception;

import org.springframework.security.authentication.AccountStatusException;

/**
 * Thrown if an authentication request is rejected because the account is blocked.
 * This means the username is neither allowListed, nor part of an agency domain, nor invited via LPG identity-management.
 * Makes no assertion whether the credentials were valid or not.
 */
public class AccountBlockedException extends AccountStatusException {
    public AccountBlockedException(String message) {
        super(message);
    }
}
