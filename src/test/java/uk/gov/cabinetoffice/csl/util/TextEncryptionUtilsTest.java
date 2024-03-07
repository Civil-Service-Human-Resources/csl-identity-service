package uk.gov.cabinetoffice.csl.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextEncryptionUtilsTest {

    @Test
    void shouldEncryptAndDecryptText() {
        String textToEncrypt = "abc@xyz.com";
        String encryptionKey = "0123456789abcdef0123456789abcdef";
        String encryptedText = TextEncryptionUtils.getEncryptedText(textToEncrypt, encryptionKey);
        String decryptedText = TextEncryptionUtils.getDecryptedText(encryptedText, encryptionKey);
        assertEquals(textToEncrypt, decryptedText);
    }
}
