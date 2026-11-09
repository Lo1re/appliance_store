package com.epam.rd.autocode.assessment.appliances.exception;

public class OrderException extends RuntimeException {

    public OrderException(String messageKey) {
        super(messageKey);
    }
}