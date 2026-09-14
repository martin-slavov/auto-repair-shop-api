package com.example.auto_repair_shop_api.validation.validator;

import com.example.auto_repair_shop_api.dto.auth.RegisterRequestDTO;
import com.example.auto_repair_shop_api.validation.PasswordMatch;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, RegisterRequestDTO> {

    @Override
    public boolean isValid(RegisterRequestDTO value, ConstraintValidatorContext context) {
        if (value.password() == null || value.confirmPassword() == null) {
            return true; // @NoBlank will handle it in the dto
        }
        return value.password().equals(value.confirmPassword());
    }
}
