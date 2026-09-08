package com.example.auto_repair_shop_api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Year;

public class VehicleYearValidator implements ConstraintValidator<ValidVehicleYear, Integer> {

    @Override
    public boolean isValid(Integer year, ConstraintValidatorContext context) {
        if (year == null) {
            return true; // @NotNull will handle null values
        }
        int currentYear = Year.now().getValue();
        return year >= 1886 && year <= currentYear; // The first car was invented in 1886
    }
}
