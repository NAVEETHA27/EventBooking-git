package com.eventbooking.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates QR code PNG images for ticket IDs.
 */
@Component
@Slf4j
public class QrCodeGenerator {

    private static final int WIDTH  = 300;
    private static final int HEIGHT = 300;

    public String generate(String content) throws Exception {
        Path dir = Paths.get("uploads/qrcodes");
        Files.createDirectories(dir);

        String filename = "qr_" + content + ".png";
        Path filePath = dir.resolve(filename);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, WIDTH, HEIGHT);
        MatrixToImageWriter.writeToPath(matrix, "PNG", filePath);

        log.debug("QR code generated: {}", filePath);
        return "/uploads/qrcodes/" + filename;
    }
}
