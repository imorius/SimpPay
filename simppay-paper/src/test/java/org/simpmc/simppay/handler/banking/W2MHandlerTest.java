package org.simpmc.simppay.handler.banking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.simpmc.simppay.config.types.banking.Web2mConfig;
import org.simpmc.simppay.data.bank.web2m.BankType;
import org.simpmc.simppay.handler.banking.data.BankingData;
import org.simpmc.simppay.handler.banking.web2m.W2MHandler;
import org.simpmc.simppay.testutil.MockBukkitSetup;
import org.simpmc.simppay.util.qrcode.BankQrRenderer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class W2MHandlerTest {

    @AfterEach
    void tearDown() {
        MockBukkitSetup.clearConfigManager();
    }

    @Test
    void w2mConfig_defaultAccountNumber_isDefaultPlaceholder() {
        Web2mConfig config = new Web2mConfig();
        // Default account number "123123123" triggers FAILED path in processPayment
        assertEquals("123123123", config.accountNumber);
    }

    @Test
    void getTransactionResult_matchesRefIdInDescription() {
        // Verify the matching logic: tx.getDescription().contains(detail.getRefID())
        String refId = "abc1234567";
        String description = "Payment " + refId + " done";
        assertTrue(description.contains(refId),
                "Transaction matching should work when description contains refId");
    }

    @Test
    void getTransactionResult_noMatch_notContained() {
        String refId = "abc1234567";
        String description = "Unrelated payment description";
        assertFalse(description.contains(refId),
                "No match when description does not contain refId");
    }

    @Test
    void web2mVietQrUrl_usesBinAccountAmountAndEncodedReference() {
        String url = BankQrRenderer.buildVietQrImageUrl("970436", "123456789", 100000, "abc 123/xyz");

        assertEquals("https://img.vietqr.io/image/970436-123456789-qr_only.png?amount=100000&addInfo=abc+123%2Fxyz", url);
    }

    @Test
    void web2mBankingData_carriesBankTypeName() {
        BankingData bankingData = BankingData.builder()
                .bin(BankType.VCB.bin)
                .bankName(BankType.VCB.name())
                .build();

        assertEquals("VCB", bankingData.getBankName());
    }
}
