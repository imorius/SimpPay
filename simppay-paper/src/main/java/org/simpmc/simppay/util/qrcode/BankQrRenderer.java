package org.simpmc.simppay.util.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.bukkit.map.MapPalette;
import org.simpmc.simppay.handler.banking.data.BankingData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

public final class BankQrRenderer {
    public static final int MAP_SIZE = 128;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private BankQrRenderer() {
    }

    public static byte[] render(BankingData bankingData) throws IOException, InterruptedException, WriterException {
        if (hasText(bankingData.getQrImageUrl())) {
            return renderImageUrl(bankingData.getQrImageUrl());
        }
        if (hasText(bankingData.getQrString())) {
            return renderQrString(bankingData.getQrString());
        }
        if (hasText(bankingData.getBin()) && hasText(bankingData.getAccountNumber()) && hasText(bankingData.getDesc())) {
            String qrImageUrl = buildVietQrImageUrl(
                    bankingData.getBin(),
                    bankingData.getAccountNumber(),
                    Math.round(bankingData.getAmount()),
                    bankingData.getDesc()
            );
            bankingData.setQrImageUrl(qrImageUrl);
            return renderImageUrl(qrImageUrl);
        }
        throw new IOException("Banking payment does not contain QR data");
    }

    public static String buildVietQrImageUrl(String bin, String accountNumber, long amount, String reference) {
        return "https://img.vietqr.io/image/"
                + encodePathSegment(bin)
                + "-"
                + encodePathSegment(accountNumber)
                + "-qr_only.png?amount="
                + amount
                + "&addInfo="
                + URLEncoder.encode(reference, StandardCharsets.UTF_8);
    }

    @SuppressWarnings({"deprecation", "removal"})
    public static byte[] renderQrString(String qrString) throws WriterException {
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 1
        );
        BitMatrix matrix = new QRCodeWriter().encode(qrString, BarcodeFormat.QR_CODE, MAP_SIZE, MAP_SIZE, hints);
        byte[] mapBytes = new byte[MAP_SIZE * MAP_SIZE];
        byte black = MapPalette.matchColor(0, 0, 0);
        byte white = MapPalette.matchColor(255, 255, 255);
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                mapBytes[x + y * MAP_SIZE] = matrix.get(x, y) ? black : white;
            }
        }
        return mapBytes;
    }

    public static byte[] renderImageUrl(String imageUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch QR image: HTTP " + response.statusCode());
        }

        try (InputStream body = response.body()) {
            BufferedImage image = ImageIO.read(body);
            if (image == null) {
                throw new IOException("Failed to decode QR image from URL: " + imageUrl);
            }
            return convertImageToMapBytes(image);
        }
    }

    @SuppressWarnings({"deprecation", "removal"})
    public static byte[] convertImageToMapBytes(BufferedImage original) {
        BufferedImage scaled;
        if (original.getWidth() != MAP_SIZE || original.getHeight() != MAP_SIZE) {
            scaled = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_ARGB);
            var graphics = scaled.createGraphics();
            graphics.drawImage(original, 0, 0, MAP_SIZE, MAP_SIZE, null);
            graphics.dispose();
        } else {
            scaled = original;
        }

        byte[] mapBytes = new byte[MAP_SIZE * MAP_SIZE];
        Arrays.fill(mapBytes, MapPalette.matchColor(255, 255, 255));

        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int argb = scaled.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int red = alpha < 128 ? 255 : (argb >> 16) & 0xFF;
                int green = alpha < 128 ? 255 : (argb >> 8) & 0xFF;
                int blue = alpha < 128 ? 255 : argb & 0xFF;
                mapBytes[x + y * MAP_SIZE] = MapPalette.matchColor(red, green, blue);
            }
        }

        return mapBytes;
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
