package com.monocept.project.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class RSAUtil {

    private final KeyPair keyPair;

    public String getPublicKey() {
        return Base64.getEncoder()
                .encodeToString(keyPair.getPublic().getEncoded());
    }

    public String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());

            return new String(
                    cipher.doFinal(Base64.getDecoder().decode(encrypted))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }
}