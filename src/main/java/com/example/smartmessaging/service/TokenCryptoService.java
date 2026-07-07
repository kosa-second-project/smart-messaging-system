package com.example.smartmessaging.service;

public interface TokenCryptoService {
    String encrypt(String plainText);

    String decrypt(String encryptedText);
}
