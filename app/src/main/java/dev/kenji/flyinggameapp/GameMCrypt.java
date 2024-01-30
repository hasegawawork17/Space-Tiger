package dev.kenji.flyinggameapp;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypt and Decrypt Class for SHA Data Sending securely
 *
 * Functions:
 *      encrypt(String message, String key) used for encrypting the data using AES Algorithm
 *      decrypt(String message, String key) used for decrypting the data using AES Algorithm
 */
public class GameMCrypt {
    private static final String METHOD = "AES/CBC/PKCS5Padding";
    private static final String IV = "fedcba9876543210";

    public static String encrypt(String message, String key) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance(METHOD);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));
        byte[] encryptedBytes = cipher.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String message, String key) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance(METHOD);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(message));

        byte[] trimmedBytes = new byte[decryptedBytes.length - 16];
        System.arraycopy(decryptedBytes, 16, trimmedBytes, 0, trimmedBytes.length);

        return new String(trimmedBytes, StandardCharsets.UTF_8);
    }

}