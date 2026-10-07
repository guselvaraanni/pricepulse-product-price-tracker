package com.pricepulse.exception;

public class DuplicateProductUrlException extends RuntimeException {

    public DuplicateProductUrlException(String productUrl) {
        super("A product with URL " + productUrl + " is already being tracked");
    }
}
