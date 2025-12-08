package com.loopers.infrastructure.payment.client;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
public class PgServiceUnavailableException extends CoreException {

    public PgServiceUnavailableException(String message, Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, message);
        initCause(cause);
    }
}
