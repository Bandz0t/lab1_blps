package com.example.youtubemonetization.service.camunda;

public final class CamundaVariables {

    private CamundaVariables() {
    }

    public static Long longValue(Object value, String variableName) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Long.parseLong(stringValue);
        }
        throw new IllegalArgumentException("Camunda variable '" + variableName + "' must be a number");
    }

    public static Integer intValue(Object value, String variableName) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Integer.parseInt(stringValue);
        }
        throw new IllegalArgumentException("Camunda variable '" + variableName + "' must be a number");
    }
}
