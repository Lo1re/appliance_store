package com.epam.rd.autocode.assessment.appliances.exception;

public class OrderException extends BusinessException {
    public OrderException(OrderError error) {
        super(error);
    }

    @Override
    public OrderError error() {
        return (OrderError) super.error();
    }
}
