package org.simpmc.simppay.forms;

import org.junit.jupiter.api.Test;
import org.simpmc.simppay.config.types.menu.FormsConfig;
import org.simpmc.simppay.handler.banking.data.BankingData;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingFormTest {

    @Test
    void buildContent_includesBankingDetails() {
        FormsConfig.BankingFormStrings strings = new FormsConfig.BankingFormStrings();
        BankingData bankingData = BankingData.builder()
                .accountNumber("123456789")
                .bankName("VCB")
                .amount(50000)
                .desc("smc123")
                .build();

        String content = BankingForm.buildContent(bankingData, strings);

        assertTrue(content.contains("123456789"));
        assertTrue(content.contains("VCB"));
        assertTrue(content.contains("50.000đ"));
        assertTrue(content.contains("smc123"));
    }

    @Test
    void buildContent_fallsBackToBinWhenBankNameMissing() {
        FormsConfig.BankingFormStrings strings = new FormsConfig.BankingFormStrings();
        BankingData bankingData = BankingData.builder()
                .bin("970436")
                .amount(100000)
                .build();

        String content = BankingForm.buildContent(bankingData, strings);

        assertTrue(content.contains("970436"));
    }
}
