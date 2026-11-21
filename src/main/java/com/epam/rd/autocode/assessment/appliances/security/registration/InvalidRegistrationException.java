package com.epam.rd.autocode.assessment.appliances.security.registration;

public class InvalidRegistrationException extends RuntimeException {
    public InvalidRegistrationException() { super("Invalid or expired registration confirmation"); }
}
