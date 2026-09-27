package com.ra.medicine.service.impl;

import com.ra.medicine.model.dto.ImportMedicineRequest;
import com.ra.medicine.model.dto.MedicineResponse;
import com.ra.medicine.model.dto.PharmacyAlertMessage;
import com.ra.medicine.model.dto.UpdateMedicineRequest;
import com.ra.medicine.model.entity.Medicine;
import com.ra.medicine.pubsub.PharmacyAlertPublisher;
import com.ra.medicine.repository.MedicineRepository;
import com.ra.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
    private final PharmacyAlertPublisher pharmacyAlertPublisher;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "medicines", key = "#id")
    public MedicineResponse getMedicineById(Long id) {
        log.info(">>> [DATABASE HIT] Cache miss for medicine id: {}. Querying database...", id);
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found with id: " + id));
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

    @Override
    @Transactional
    @CacheEvict(value = "medicines", key = "#medicine.id")
    public MedicineResponse updateMedicine(Medicine medicine) {
        log.info(">>> [CACHE EVICT] Evicting cache for medicine id: {}. Updating database...", medicine.getId());
        if (medicine.getId() == null || !medicineRepository.existsById(medicine.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found with id: " + medicine.getId());
        }
        Medicine saved = medicineRepository.save(medicine);
        log.info(">>> [DATABASE UPDATE] Medicine id {} successfully updated with price {}.", saved.getId(), saved.getUnitPrice());
        return MedicineResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "medicines", key = "#id")
    public MedicineResponse updateMedicine(Long id, UpdateMedicineRequest request) {
        log.info(">>> [CACHE EVICT] Evicting cache for medicine id: {} via DTO update.", id);
        Medicine existing = medicineRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found with id: " + id));

        if (request.name() != null) {
            existing.setName(request.name());
        }
        if (request.activeIngredient() != null) {
            existing.setActiveIngredient(request.activeIngredient());
        }
        if (request.manufacturer() != null) {
            existing.setManufacturer(request.manufacturer());
        }
        if (request.unitPrice() != null) {
            existing.setUnitPrice(request.unitPrice());
        }
        if (request.stockQuantity() != null) {
            existing.setStockQuantity(request.stockQuantity());
        }

        Medicine saved = medicineRepository.save(existing);
        log.info(">>> [DATABASE UPDATE] Medicine id {} updated with new price {}.", saved.getId(), saved.getUnitPrice());
        return MedicineResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "medicines", key = "#request.medicineId()")
    public MedicineResponse importMedicine(ImportMedicineRequest request) {
        log.info(">>> [MEDICINE IMPORT] Processing import for medicine id: {}, quantity: {}",
                request.medicineId(), request.quantity());

        Medicine medicine = medicineRepository.findById(request.medicineId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Medicine not found with id: " + request.medicineId()));

        int currentStock = medicine.getStockQuantity() != null ? medicine.getStockQuantity() : 0;
        medicine.setStockQuantity(currentStock + request.quantity());
        Medicine saved = medicineRepository.save(medicine);

        // Publish alert to Redis Pub/Sub channel pharmacy-alerts
        String alertText = "Đã nhập " + request.quantity() + " hộp " + saved.getName();
        PharmacyAlertMessage alert = new PharmacyAlertMessage("IMPORT", alertText);
        pharmacyAlertPublisher.publishAlert(alert);

        return MedicineResponse.fromEntity(saved);
    }
}
