package uk.gov.cabinetoffice.csl.exception;

import org.springframework.security.authentication.AccountStatusException;

/**
 * Thrown if an authentication request is rejected because the account is blocked.
 * This means the username is neither allowListed, nor part of an agency domain, nor invited via LPG identity-management.
 * Makes no assertion whether the credentials were valid or not.
 */
public class AccountBlockedException extends AccountStatusException {
    // ~ Constructors
    // ===================================================================================================

    /**
     * Constructs a <code>AccountBlockedException</code> with the specified message.
     *
     * @param msg the detail message.
     */
    public AccountBlockedException(String msg) {
        super(msg);
    }

    /**
     * Constructs a <code>AccountBlockedException</code> with the specified message and root
     * cause.
     *
     * @param msg the detail message.
     * @param t   root cause
     */
    public AccountBlockedException(String msg, Throwable t) {
        super(msg, t);
    }
}
