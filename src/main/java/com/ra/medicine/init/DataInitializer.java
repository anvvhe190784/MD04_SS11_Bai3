package com.ra.medicine.init;

import com.ra.medicine.model.entity.Medicine;
import com.ra.medicine.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final MedicineRepository medicineRepository;

    @Override
    public void run(String... args) {
        if (medicineRepository.count() == 0) {
            log.info("Initializing medicine catalog sample data...");
            List<Medicine> initialMedicines = List.of(
                    new Medicine(null, "Paracetamol 500mg", "Paracetamol", "Hau Giang Pharma", 1500.0, 5000),
                    new Medicine(null, "Panadol Extra", "Paracetamol, Caffeine", "GSK", 2500.0, 3000),
                    new Medicine(null, "Amoxicillin 500mg", "Amoxicillin", "Mekophar", 3000.0, 2000),
                    new Medicine(null, "Ibuprofen 400mg", "Ibuprofen", "Domesco", 2200.0, 1500),
                    new Medicine(null, "Cetirizine 10mg", "Cetirizine Hydrochloride", "Traphaco", 1800.0, 4000),
                    new Medicine(null, "Augmentin 1g", "Amoxicillin, Clavulanic Acid", "GSK", 18000.0, 800),
                    new Medicine(null, "Berberin 100mg", "Berberin Chloride", "OPC Pharma", 800.0, 10000));
            medicineRepository.saveAll(initialMedicines);
            log.info("Successfully initialized {} medicines into the database.", initialMedicines.size());
        }
    }
}
