package com.cecamed.core.model.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Componente embebible para registrar los signos vitales del paciente durante una consulta.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class VitalSigns {

    @Column(name = "systolic_pressure")
    private Integer systolicPressure; // mmHg

    @Column(name = "diastolic_pressure")
    private Integer diastolicPressure; // mmHg

    @Column(name = "heart_rate")
    private Integer heartRate; // ppm / bpm

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate; // rpm

    @Column(name = "temperature_celsius", precision = 4, scale = 2)
    private BigDecimal temperatureCelsius; // °C

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg; // kg

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm; // cm

    @Column(name = "bmi", precision = 4, scale = 2)
    private BigDecimal bmi; // Índice de Masa Corporal (kg/m²)

    @Column(name = "oxygen_saturation_percentage")
    private Integer oxygenSaturationPercentage; // % SpO2

    /**
     * Calcula y actualiza automáticamente el IMC si el peso y la altura están presentes.
     */
    public BigDecimal calculateAndSetBmi() {
        if (weightKg != null && heightCm != null && heightCm.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal heightInMeters = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal heightSquared = heightInMeters.multiply(heightInMeters);
            this.bmi = weightKg.divide(heightSquared, 2, RoundingMode.HALF_UP);
        }
        return this.bmi;
    }
}
