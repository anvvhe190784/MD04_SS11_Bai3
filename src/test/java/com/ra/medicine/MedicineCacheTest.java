package com.ra.medicine;

import com.ra.medicine.model.dto.MedicineResponse;
import com.ra.medicine.model.dto.UpdateMedicineRequest;
import com.ra.medicine.model.entity.Medicine;
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
    @DisplayName("Bài tập 1: Should cache medicine in Redis on first call and hit cache on subsequent calls")
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

        // Verify Redis now contains the key
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey)), "Redis must contain key after first call");
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(cachedJson, "Cached value in Redis must not be null");

        // 2nd Call - Cache Hit (From Redis, no DB query)
        long start2 = System.nanoTime();
        MedicineResponse res2 = medicineService.getMedicineById(medicineId);
        long duration2Ms = (System.nanoTime() - start2) / 1_000_000;

        assertNotNull(res2);
        assertEquals(res1.id(), res2.id());
        assertEquals(res1.unitPrice(), res2.unitPrice());

        // 3rd Call - Subsequent warm cache call
        long start3 = System.nanoTime();
        MedicineResponse res3 = medicineService.getMedicineById(medicineId);
        long duration3Ms = (System.nanoTime() - start3) / 1_000_000;

        System.out.printf(">>> 1st call duration: %d ms (DB query - Cache Miss)%n", duration1Ms);
        System.out.printf(">>> 2nd call duration: %d ms (Redis Cache Hit)%n", duration2Ms);
        System.out.printf(">>> 3rd call duration: %d ms (Warmed Redis Cache Hit)%n", duration3Ms);

        assertTrue(duration2Ms < duration1Ms, "Cache hit must be faster than DB query");
    }

    @Test
    @DisplayName("Bài tập 2: Should evict cache on price update and reload updated data on next query")
    void shouldEvictCacheOnUpdateAndReloadFromDb() throws Exception {
        Long medicineId = 2L;
        String cacheKey = "medicines::" + medicineId;

        // Clean cache initially
        redisTemplate.delete(cacheKey);

        // Step 1: Lần 1 lấy thuốc -> Cache miss, query DB, nạp vào Redis
        System.out.println("=== BƯỚC 1: Lần đầu lấy thuốc ID=2 (Cache Miss - Query DB) ===");
        MedicineResponse step1Res = medicineService.getMedicineById(medicineId);
        assertNotNull(step1Res);
        assertEquals(medicineId, step1Res.id());
        Double originalPrice = step1Res.unitPrice();
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey)), "Redis phải chứa key medicines::2 sau Bước 1");

        // Step 2: Lần 2 lấy thuốc -> Cache hit, không query DB
        System.out.println("=== BƯỚC 2: Gọi lại thuốc ID=2 (Cache Hit - Lấy từ Redis) ===");
        MedicineResponse step2Res = medicineService.getMedicineById(medicineId);
        assertNotNull(step2Res);
        assertEquals(originalPrice, step2Res.unitPrice());

        // Step 3 (Chốt hạ): Cập nhật giá thuốc -> @CacheEvict xóa key khỏi Redis
        System.out.println("=== BƯỚC 3: Cập nhật giá thuốc ID=2 (Kích hoạt @CacheEvict) ===");
        Double newPrice = 9999.0;
        Medicine medicineToUpdate = new Medicine(
                medicineId,
                step1Res.name(),
                step1Res.activeIngredient(),
                step1Res.manufacturer(),
                newPrice,
                step1Res.stockQuantity()
        );
        MedicineResponse updatedRes = medicineService.updateMedicine(medicineToUpdate);
        assertEquals(newPrice, updatedRes.unitPrice());

        // Kiểm tra Redis: key medicines::2 phải biến mất ngay lập tức!
        Thread.sleep(100);
        boolean keyExistsInRedis = Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        System.out.println("Key 'medicines::2' tồn tại trong Redis sau update: " + keyExistsInRedis);
        assertFalse(keyExistsInRedis, "Key medicines::2 phải bị xóa khỏi Redis ngay lập tức sau khi cập nhật");

        // Step 4: Gọi lại API lấy thuốc ID=2 -> Query DB lại, nạp dữ liệu mới vào cache
        System.out.println("=== BƯỚC 4: Lấy lại thuốc ID=2 (Cache Miss lại - Query DB lấy giá mới) ===");
        MedicineResponse step4Res = medicineService.getMedicineById(medicineId);
        assertNotNull(step4Res);
        assertEquals(newPrice, step4Res.unitPrice(), "Giá thuốc trả về phải là giá mới sau khi nạp lại từ DB");

        // Xác nhận Redis đã được nạp lại với key và giá mới
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey)), "Redis phải nạp lại key medicines::2 sau Bước 4");
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(cachedValue);
        assertTrue(cachedValue.contains("9999.0"), "Dữ liệu cache trong Redis phải chứa giá mới 9999.0");
    }
}
