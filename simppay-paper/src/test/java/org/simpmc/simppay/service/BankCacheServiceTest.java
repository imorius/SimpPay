package org.simpmc.simppay.service;

import org.junit.jupiter.api.Test;
import org.simpmc.simppay.handler.banking.sepay.data.BankData;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BankCacheServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void getBankByBin_returnsMatchingCachedBank() throws Exception {
        BankCacheService service = new BankCacheService();
        BankData bank = new BankData(1, "Vietcombank", "VCB", "970436", "VCB", "logo", 1, 1, null);

        Field cacheField = BankCacheService.class.getDeclaredField("bankCache");
        cacheField.setAccessible(true);
        Map<String, BankData> cache = (Map<String, BankData>) cacheField.get(service);
        cache.put("vcb", bank);

        assertSame(bank, service.getBankByBin("970436"));
        assertNull(service.getBankByBin("000000"));
        assertNull(service.getBankByBin(null));
    }
}
