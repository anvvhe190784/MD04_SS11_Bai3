package com.ra.medicine.model.dto;

public record ImportMedicineRequest(
        Long medicineId,
        Integer quantity
) {
}
