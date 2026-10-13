package com.employeehub.query;

/** The query asks for something that doesn't make sense or isn't allowed (e.g. an unknown department). */
public class InvalidQueryException extends RuntimeException {
    public InvalidQueryException(String message) {
        super(message);
    }
}
