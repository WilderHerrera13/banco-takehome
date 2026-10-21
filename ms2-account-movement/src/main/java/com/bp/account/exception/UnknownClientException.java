package com.bp.account.exception;

public class UnknownClientException extends RuntimeException {

    public UnknownClientException(String clientId) {
        super("El client " + clientId + " no esKnown por el microservicio de clients");
    }
}
