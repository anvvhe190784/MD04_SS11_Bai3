package com.ra.medicine.model.dto;

public record UpdateMedicineRequest(
        String name,
        String activeIngredient,
        String manufacturer,
        Double unitPrice,
        Integer stockQuantity
) {
}
