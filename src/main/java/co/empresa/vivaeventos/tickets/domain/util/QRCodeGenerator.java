package co.empresa.vivaeventos.tickets.domain.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

public class QRCodeGenerator {

    private static final int SIZE = 280;

    public static String toBase64DataUri(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.MARGIN, 1
            );
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, SIZE, SIZE, hints);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", os);
            String b64 = Base64.getEncoder().encodeToString(os.toByteArray());
            return "data:image/png;base64," + b64;
        } catch (WriterException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        } catch (Exception e) {
            return "";
        }
    }
}
