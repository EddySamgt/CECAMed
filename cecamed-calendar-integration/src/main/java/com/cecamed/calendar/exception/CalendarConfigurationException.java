package com.cecamed.calendar.exception;

public class CalendarConfigurationException extends RuntimeException {
    public CalendarConfigurationException(String message) {
        super(message);
    }

    public CalendarConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
