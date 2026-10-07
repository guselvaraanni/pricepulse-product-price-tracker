package com.pricepulse.exception;

public class InactiveProductException extends RuntimeException {

    public InactiveProductException(Long id) {
        super("Product " + id + " is inactive; reactivate it before recording prices");
    }
}
