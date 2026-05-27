package org.simpmc.simppay.util.qrcode;

import org.bukkit.map.MapPalette;
import org.junit.jupiter.api.Test;
import org.simpmc.simppay.handler.banking.data.BankingData;

import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

class BankQrRendererTest {

    @Test
    void payosQrString_producesNonBlankMapPatch() throws Exception {
        byte[] mapBytes = BankQrRenderer.render(BankingData.builder()
                .qrString("00020101021238540010A00000072701240006970436011012345678900208QRIBFTTA530370454061000005802VN6304ABCD")
                .build());

        assertEquals(16_384, mapBytes.length);
        assertFalse(allBytesMatch(mapBytes, mapBytes[0]));
    }

    @Test
    void web2mFallback_setsVietQrUrlWithEncodedReference() {
        BankingData bankingData = BankingData.builder()
                .bin("970422")
                .accountNumber("123456789")
                .amount(50000)
                .desc("simp pay/ref 1")
                .build();

        String url = BankQrRenderer.buildVietQrImageUrl(
                bankingData.getBin(),
                bankingData.getAccountNumber(),
                Math.round(bankingData.getAmount()),
                bankingData.getDesc()
        );

        assertEquals("https://img.vietqr.io/image/970422-123456789-qr_only.png?amount=50000&addInfo=simp+pay%2Fref+1", url);
    }

    @Test
    @SuppressWarnings({"deprecation", "removal"})
    void imageConversion_outputs128MapAndTreatsTransparencyAsWhite() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x00000000);

        byte[] mapBytes = BankQrRenderer.convertImageToMapBytes(image);

        assertEquals(16_384, mapBytes.length);
        assertEquals(MapPalette.matchColor(255, 255, 255), mapBytes[0]);
    }

    private static boolean allBytesMatch(byte[] bytes, byte expected) {
        for (byte value : bytes) {
            if (value != expected) {
                return false;
            }
        }
        return true;
    }
}
