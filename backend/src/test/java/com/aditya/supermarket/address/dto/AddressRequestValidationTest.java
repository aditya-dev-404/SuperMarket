package com.aditya.supermarket.address.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AddressRequestValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static AddressRequest request(String phone, String pincode) {
        return new AddressRequest("Aditya Kumar", phone, "Flat 12, Main Road", null,
            "Greater Noida", "Uttar Pradesh", pincode, false);
    }

    private static Set<String> violatedFields(AddressRequest request) {
        return VALIDATOR.validate(request).stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(Object::toString)
            .collect(Collectors.toSet());
    }

    @Test
    void validRequest_hasNoViolations_andLine2IsOptional() {
        assertThat(violatedFields(request("9876543210", "201310"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"5876543210", "987654321", "98765432101", "98765abcde", "", " "})
    void invalidPhone_isRejected(String phone) {
        assertThat(violatedFields(request(phone, "201310"))).contains("phone");
    }

    @ParameterizedTest
    @ValueSource(strings = {"012345", "12345", "1234567", "12ab56", "", " "})
    void invalidPincode_isRejected(String pincode) {
        assertThat(violatedFields(request("9876543210", pincode))).contains("pincode");
    }

    @Test
    void blankRequiredFields_areRejected() {
        AddressRequest blank = new AddressRequest(" ", "9876543210", " ", null, " ", " ", "201310", false);

        assertThat(violatedFields(blank)).containsExactlyInAnyOrder("fullName", "line1", "city", "state");
    }
}
