package com.ra.medicine.service;

import com.ra.medicine.model.dto.ImportMedicineRequest;
import com.ra.medicine.model.dto.MedicineResponse;
import com.ra.medicine.model.dto.UpdateMedicineRequest;
import com.ra.medicine.model.entity.Medicine;

import java.util.List;

public interface MedicineService {

    MedicineResponse getMedicineById(Long id);

    List<MedicineResponse> getAllMedicines();

    MedicineResponse updateMedicine(Medicine medicine);

    MedicineResponse updateMedicine(Long id, UpdateMedicineRequest request);

    MedicineResponse importMedicine(ImportMedicineRequest request);
}
