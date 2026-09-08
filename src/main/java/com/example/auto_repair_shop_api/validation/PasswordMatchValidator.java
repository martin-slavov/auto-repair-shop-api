package com.example.auto_repair_shop_api.validation;

import com.example.auto_repair_shop_api.dto.RegisterRequestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, RegisterRequestDTO> {

    @Override
    public boolean isValid(RegisterRequestDTO value, ConstraintValidatorContext context) {
        if (value.getPassword() == null || value.getConfirmPassword() == null) {
            return true; // @NoBlank will handle it in the dto
        }
        return value.getPassword().equals(value.getConfirmPassword());
    }
}
