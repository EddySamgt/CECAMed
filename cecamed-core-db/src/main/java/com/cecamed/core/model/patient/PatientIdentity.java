package com.cecamed.core.model.patient;

import java.text.Normalizer;
import java.util.Locale;

public final class PatientIdentity {
    private PatientIdentity() {}

    public static String normalizeName(String name) {
        if (name == null) return null;
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036f]", "")
                .toLowerCase(Locale.ROOT).replaceAll("(?U)\\s+", " ").trim();
    }
}
