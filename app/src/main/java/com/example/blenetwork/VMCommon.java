package com.example.blenetwork;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class VMCommon {
  public static final BouncyCastleProvider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();

  // Signs @content with @priv_key
  static byte[] sign(PrivateKey priv_key, byte[] content) {
    Signature sign;
    try {
      sign = Signature.getInstance("SHA256withECDSA", BOUNCY_CASTLE_PROVIDER);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    try {
      sign.initSign(priv_key);
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    }

    try {
      sign.update(content);
      return sign.sign();
    } catch (SignatureException e) {
      throw new RuntimeException(e);
    }
  }

  // Verifies the @signature of @content with a provided @public_key
  static boolean verifySign(PublicKey public_key, byte[] content, byte[] signature) {
    Signature sign;
    try {
      sign = Signature.getInstance("SHA256withECDSA", BOUNCY_CASTLE_PROVIDER);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    try {
      sign.initVerify(public_key);
    } catch (InvalidKeyException e) {
      return false;
    }
    try {
      sign.update(content);
      if (!sign.verify(signature)) {
        return false;
      }
    } catch (SignatureException e) {
      return false;
    }
    return true;
  }

  // Verifies the @signature of @content with a provided public key in bytes format @public_key_bytes
  static boolean verifySign(byte[] public_key_bytes, byte[] content, byte[] signature) {
    PublicKey public_key;
    try {
      public_key = KeyFactory.getInstance("ECDSA", BOUNCY_CASTLE_PROVIDER).generatePublic(
          new X509EncodedKeySpec(public_key_bytes)
      );
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    return verifySign(public_key, content, signature);
  }
}
