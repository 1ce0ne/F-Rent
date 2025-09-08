package com.example.akkubattrent.Utils;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    public static String encrypt(String data, String key) throws Exception {
        // Декодируем ключ из Base64
        byte[] decodedKey = Base64.decode(key, Base64.DEFAULT);
        SecretKeySpec secretKey = new SecretKeySpec(decodedKey, ALGORITHM);

        // Генерация IV
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Инициализация шифрования
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        // Шифрование данных
        byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"));

        // Объединяем IV и зашифрованные данные
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    public static String decrypt(String encryptedData, String key) throws Exception {
        // Декодируем ключ из Base64
        byte[] decodedKey = Base64.decode(key, Base64.DEFAULT);
        SecretKeySpec secretKey = new SecretKeySpec(decodedKey, ALGORITHM);

        // Декодируем зашифрованные данные
        byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT);

        // Извлекаем IV и зашифрованные данные
        byte[] iv = new byte[16];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        byte[] encryptedBytes = new byte[combined.length - iv.length];
        System.arraycopy(combined, iv.length, encryptedBytes, 0, encryptedBytes.length);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Инициализация дешифрования
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        // Дешифрование данных
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, "UTF-8");
    }
}