package com.bp.client.exception;

public class DuplicateIdentificationException extends RuntimeException {

    public DuplicateIdentificationException(String identification) {
        super("La identification ya esta registrada: " + identification);
    }
}
