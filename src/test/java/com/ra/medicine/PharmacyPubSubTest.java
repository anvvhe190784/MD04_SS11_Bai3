package com.ra.medicine;

import com.ra.medicine.model.dto.ImportMedicineRequest;
import com.ra.medicine.model.dto.PharmacyAlertMessage;
import com.ra.medicine.model.entity.Medicine;
import com.ra.medicine.pubsub.PharmacyAlertPublisher;
import com.ra.medicine.pubsub.PharmacyAlertSubscriber;
import com.ra.medicine.repository.MedicineRepository;
import com.ra.medicine.service.MedicineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PharmacyPubSubTest {

    @Autowired
    private PharmacyAlertPublisher publisher;

    @Autowired
    private PharmacyAlertSubscriber subscriber;

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private MedicineRepository medicineRepository;

    @BeforeEach
    void setUp() {
        // Clean or seed test entity
        if (medicineRepository.count() == 0) {
            Medicine panadol = new Medicine(null, "Panadol Extra", "Paracetamol 500mg, Caffeine 65mg", "GSK", 25000.0, 50);
            medicineRepository.save(panadol);
        }
    }

    @Test
    @DisplayName("Pub/Sub Test: Publish JSON alert and verify Subscriber consumes it asynchronously")
    void testDirectPublishAndSubscribe() throws Exception {
        PharmacyAlertMessage message = new PharmacyAlertMessage("IMPORT", "Đã nhập 100 hộp Panadol");

        publisher.publishAlert(message);

        // Allow async Redis delivery
        TimeUnit.MILLISECONDS.sleep(300);

        PharmacyAlertMessage received = subscriber.getLastReceivedAlert();
        assertThat(received).isNotNull();
        assertThat(received.type()).isEqualTo("IMPORT");
        assertThat(received.message()).isEqualTo("Đã nhập 100 hộp Panadol");
    }

    @Test
    @DisplayName("Pub/Sub Test: Import medicine updates stock and broadcasts alert over channel")
    void testImportMedicineBroadcast() throws Exception {
        Medicine medicine = medicineRepository.findAll().get(0);
        int initialStock = medicine.getStockQuantity();

        ImportMedicineRequest request = new ImportMedicineRequest(medicine.getId(), 50);
        medicineService.importMedicine(request);

        // Allow async Redis delivery
        TimeUnit.MILLISECONDS.sleep(300);

        Medicine updated = medicineRepository.findById(medicine.getId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualTo(initialStock + 50);

        PharmacyAlertMessage received = subscriber.getLastReceivedAlert();
        assertThat(received).isNotNull();
        assertThat(received.type()).isEqualTo("IMPORT");
        assertThat(received.message()).contains("Đã nhập 50 hộp " + medicine.getName());
    }
}
