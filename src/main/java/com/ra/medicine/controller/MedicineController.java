package com.ra.medicine.controller;

import com.ra.medicine.model.dto.MedicineResponse;
import com.ra.medicine.model.dto.UpdateMedicineRequest;
import com.ra.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping("/{id}")
    public ResponseEntity<MedicineResponse> getMedicineById(@PathVariable Long id) {
        MedicineResponse medicine = medicineService.getMedicineById(id);
        return ResponseEntity.ok(medicine);
    }

    @GetMapping
    public ResponseEntity<List<MedicineResponse>> getAllMedicines() {
        List<MedicineResponse> medicines = medicineService.getAllMedicines();
        return ResponseEntity.ok(medicines);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicineResponse> updateMedicine(
            @PathVariable Long id,
            @RequestBody UpdateMedicineRequest request
    ) {
        MedicineResponse updated = medicineService.updateMedicine(id, request);
        return ResponseEntity.ok(updated);
    }
}
