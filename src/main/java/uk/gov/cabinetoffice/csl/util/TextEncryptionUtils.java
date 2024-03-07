package uk.gov.cabinetoffice.csl.util;

import lombok.extern.slf4j.Slf4j;
import uk.gov.cabinetoffice.csl.exception.GenericServerException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Slf4j
public class TextEncryptionUtils {

    public static String getEncryptedText(String rawText, String encryptionKey) {
        try {
            Key aesKey = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encrypted = cipher.doFinal(rawText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("TextEncryptionUtils.getEncryptedText: Error has occurred", e);
            throw new GenericServerException("System error");
        }
    }

    public static String getDecryptedText(String encryptedText, String encryptionKey) {
        try {
        Key aesKey = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(plainText);
        } catch (Exception e) {
            log.error("TextEncryptionUtils.getDecryptedText: Error has occurred", e);
            throw new GenericServerException("System error");
        }
    }
}
