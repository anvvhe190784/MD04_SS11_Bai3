package com.ra.medicine.model.dto;

import com.ra.medicine.model.entity.Medicine;

import java.io.Serial;
import java.io.Serializable;

public record MedicineResponse(
        Long id,
        String name,
        String activeIngredient,
        String manufacturer,
        Double unitPrice,
        Integer stockQuantity) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static MedicineResponse fromEntity(Medicine medicine) {
        if (medicine == null) {
            return null;
        }
        return new MedicineResponse(
                medicine.getId(),
                medicine.getName(),
                medicine.getActiveIngredient(),
                medicine.getManufacturer(),
                medicine.getUnitPrice(),
                medicine.getStockQuantity());
    }
}
