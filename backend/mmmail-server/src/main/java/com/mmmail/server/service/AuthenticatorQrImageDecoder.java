package com.mmmail.server.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class AuthenticatorQrImageDecoder {

    private static final String DATA_URL_MARKER = ";base64,";

    public String decodeOtpauthUri(String dataUrl) {
        byte[] imageBytes = extractImageBytes(dataUrl);
        BufferedImage image = readImage(imageBytes);
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            String payload = new MultiFormatReader().decode(bitmap).getText();
            if (!StringUtils.hasText(payload) || !payload.trim().startsWith("otpauth://totp/")) {
                throw new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Authenticator QR image must contain an otpauth://totp URI");
            }
            return payload.trim();
        } catch (NotFoundException ex) {
            throw new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Authenticator QR image could not be decoded");
        }
    }

    private byte[] extractImageBytes(String dataUrl) {
        if (!StringUtils.hasText(dataUrl) || !dataUrl.startsWith("data:image/") || !dataUrl.contains(DATA_URL_MARKER)) {
            throw new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Authenticator QR image is invalid");
        }
        String base64 = dataUrl.substring(dataUrl.indexOf(DATA_URL_MARKER) + DATA_URL_MARKER.length());
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Authenticator QR image is invalid");
        }
    }

    private BufferedImage readImage(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Authenticator QR image is invalid");
            }
            return image;
        } catch (IOException ex) {
            throw new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Authenticator QR image is invalid");
        }
    }
}
