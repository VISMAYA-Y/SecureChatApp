package com.example.cryptext.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    
    /**
     * Encrypt a message using AES algorithm
     * @param message message to encrypt
     * @param secretKey private key for encryption
     * @return Base64-encoded encrypted message with IV prepended
     */
    public static String encrypt(String message, String secretKey) {
        try {
            // Generate a secure IV (Initialization Vector)
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[16]; // 16 bytes for AES
            secureRandom.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            
            // Create key
            Key key = generateKey(secretKey);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParameterSpec);
            
            // Encrypt
            byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted part
            byte[] encryptedIvPlusData = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedIvPlusData, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedIvPlusData, iv.length, encrypted.length);
            
            // Encode with Base64 for safe storage/transmission
            return Base64.encodeToString(encryptedIvPlusData, Base64.DEFAULT);
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Decrypt AES-encrypted message
     * @param encryptedData Base64-encoded encrypted data with IV prepended
     * @param secretKey private key for decryption
     * @return decrypted message
     */
    public static String decrypt(String encryptedData, String secretKey) {
        try {
            // Decode from Base64
            byte[] encryptedIvPlusData = Base64.decode(encryptedData, Base64.DEFAULT);
            
            // Extract IV
            byte[] iv = new byte[16]; // 16 bytes for AES
            System.arraycopy(encryptedIvPlusData, 0, iv, 0, iv.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            
            // Extract encrypted part
            byte[] encryptedBytes = new byte[encryptedIvPlusData.length - iv.length];
            System.arraycopy(encryptedIvPlusData, iv.length, encryptedBytes, 0, encryptedBytes.length);
            
            // Create key
            Key key = generateKey(secretKey);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
            
            // Decrypt
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            
            return new String(decrypted, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generate a key from a passphrase
     * @param secretKey passphrase
     * @return encryption key
     */
    private static Key generateKey(String secretKey) throws NoSuchAlgorithmException {
        // Create SHA-256 hash of the key
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        
        // Use first 16 bytes for AES-128
        byte[] truncatedKey = new byte[16];
        System.arraycopy(keyBytes, 0, truncatedKey, 0, truncatedKey.length);
        
        return new SecretKeySpec(truncatedKey, ALGORITHM);
    }
} 