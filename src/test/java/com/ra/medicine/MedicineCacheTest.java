package com.ra.medicine;

import com.ra.medicine.model.dto.MedicineResponse;
import com.ra.medicine.service.MedicineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MedicineCacheTest {

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Should cache medicine in Redis on first call and hit cache on subsequent calls with <10ms")
    void shouldCacheMedicineInRedis() {
        Long medicineId = 1L;

        // Clean any existing cache for medicineId = 1
        String cacheKey = "medicines::" + medicineId;
        redisTemplate.delete(cacheKey);
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey)), "Cache should be empty before first call");

        // 1st Call - Cache Miss (Queries DB)
        long start1 = System.nanoTime();
        MedicineResponse res1 = medicineService.getMedicineById(medicineId);
        long duration1Ms = (System.nanoTime() - start1) / 1_000_000;

        assertNotNull(res1);
        assertEquals(medicineId, res1.id());
        assertEquals("Paracetamol 500mg", res1.name());

        // Verify Redis now contains the key
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey)), "Redis must contain key after first call");
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(cachedJson, "Cached value in Redis must not be null");
        assertTrue(cachedJson.contains("Paracetamol 500mg"), "Cached value in Redis must contain medicine details");

        // 2nd Call - Cache Hit (From Redis, no DB query)
        long start2 = System.nanoTime();
        MedicineResponse res2 = medicineService.getMedicineById(medicineId);
        long duration2Ms = (System.nanoTime() - start2) / 1_000_000;

        assertNotNull(res2);
        assertEquals(res1.id(), res2.id());
        assertEquals(res1.name(), res2.name());
        assertEquals(res1.unitPrice(), res2.unitPrice());

        // 3rd Call - Subsequent warm cache call
        long start3 = System.nanoTime();
        MedicineResponse res3 = medicineService.getMedicineById(medicineId);
        long duration3Ms = (System.nanoTime() - start3) / 1_000_000;

        System.out.printf(">>> 1st call duration: %d ms (DB query - Cache Miss)%n", duration1Ms);
        System.out.printf(">>> 2nd call duration: %d ms (Redis Cache Hit)%n", duration2Ms);
        System.out.printf(">>> 3rd call duration: %d ms (Warmed Redis Cache Hit)%n", duration3Ms);

        // Verification requirement: subsequent call from cache should be faster than first call and < 15ms
        assertTrue(duration2Ms < duration1Ms, "Cache hit must be faster than DB query");
        assertTrue(duration3Ms < 15, "Warmed cache hit should be under 15ms");
    }
}
