package com.cecamed.services.dto.patient;

import com.cecamed.core.model.patient.enums.BloodType;
import com.cecamed.core.model.patient.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    private String lastName;

    @NotBlank(message = "La identificación/DNI es obligatoria")
    @Size(max = 50, message = "La identificación no puede exceder 50 caracteres")
    private String identificationNumber;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate birthDate;

    @NotNull(message = "El género/sexo es obligatorio")
    private Gender gender;

    @Builder.Default
    private BloodType bloodType = BloodType.DESCONOCIDO;

    @Size(max = 25, message = "El teléfono no puede exceder 25 caracteres")
    private String phone;

    @Email(message = "El formato de correo electrónico no es válido")
    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String email;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String address;

    @Size(max = 100, message = "El contacto de emergencia no puede exceder 100 caracteres")
    private String emergencyContactName;

    @Size(max = 25, message = "El teléfono de emergencia no puede exceder 25 caracteres")
    private String emergencyContactPhone;

    @Size(max = 50, message = "El parentesco de emergencia no puede exceder 50 caracteres")
    private String emergencyContactRelationship;

    private String notes;
}
