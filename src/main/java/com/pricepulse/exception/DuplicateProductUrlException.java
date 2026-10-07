package com.pricepulse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateProductUrlException extends RuntimeException {

    public DuplicateProductUrlException(String productUrl) {
        super("A product with URL " + productUrl + " is already being tracked");
    }
}
