package com.ra.medicine.service;

import com.ra.medicine.model.dto.MedicineResponse;

import java.util.List;

public interface MedicineService {

    MedicineResponse getMedicineById(Long id);

    List<MedicineResponse> getAllMedicines();
}
