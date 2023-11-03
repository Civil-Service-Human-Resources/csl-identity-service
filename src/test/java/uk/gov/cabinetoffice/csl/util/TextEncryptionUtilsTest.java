package uk.gov.cabinetoffice.csl.util;

import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class TextEncryptionUtilsTest {

    @Test
    void shouldEncryptAndDecryptText()
            throws NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String textToEncrypt = "abc@xyz.com";
        String encryptionKey = "0123456789abcdef0123456789abcdef";
        String encryptedText = TextEncryptionUtils.getEncryptedText(textToEncrypt, encryptionKey);
        String decryptedText = TextEncryptionUtils.getDecryptedText(encryptedText, encryptionKey);
        assertEquals(textToEncrypt, decryptedText);
    }
}
