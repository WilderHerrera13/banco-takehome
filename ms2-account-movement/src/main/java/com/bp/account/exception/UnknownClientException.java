package com.bp.account.exception;

public class UnknownClientException extends RuntimeException {

    public UnknownClientException(String clientId) {
        super("El client " + clientId + " no es conocido por el microservicio de clients");
    }
}
