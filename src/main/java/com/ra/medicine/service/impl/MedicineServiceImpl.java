package com.ra.medicine.service.impl;

import com.ra.medicine.model.dto.MedicineResponse;
import com.ra.medicine.model.entity.Medicine;
import com.ra.medicine.repository.MedicineRepository;
import com.ra.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "medicines", key = "#id")
    public MedicineResponse getMedicineById(Long id) {
        log.info(">>> [DATABASE HIT] Cache miss for medicine id: {}. Querying database...", id);
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found with id: " + id));
        log.info(">>> [DATABASE SUCCESS] Found medicine '{}' from database.", medicine.getName());
        return MedicineResponse.fromEntity(medicine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineResponse> getAllMedicines() {
        log.info(">>> [DATABASE HIT] Fetching all medicines from database...");
        return medicineRepository.findAll()
                .stream()
                .map(MedicineResponse::fromEntity)
                .toList();
    }
}
