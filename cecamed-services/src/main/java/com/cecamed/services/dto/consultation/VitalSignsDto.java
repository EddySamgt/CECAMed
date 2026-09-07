package com.cecamed.services.dto.consultation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSignsDto {
    private Integer systolicPressure;
    private Integer diastolicPressure;
    private Integer heartRate;
    private Integer respiratoryRate;
    private BigDecimal temperatureCelsius;
    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private BigDecimal bmi;
    private Integer oxygenSaturationPercentage;
}
