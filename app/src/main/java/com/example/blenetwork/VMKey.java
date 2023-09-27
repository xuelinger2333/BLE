package com.example.blenetwork;

import static com.example.blenetwork.VMCommon.BOUNCY_CASTLE_PROVIDER;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

public class VMKey {
  PublicKey pub_key;
  PrivateKey priv_key;

  public VMKey() {
    // Generate a 192-bit ECC key pair
    KeyPairGenerator kpg;
    try {
      kpg = KeyPairGenerator.getInstance("ECDSA", BOUNCY_CASTLE_PROVIDER);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    ECGenParameterSpec ecsp;
    ecsp = new ECGenParameterSpec("secp192r1");
    try {
      kpg.initialize(ecsp);
    } catch (InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }

    KeyPair pair = kpg.genKeyPair();
    priv_key = pair.getPrivate();
    pub_key = pair.getPublic();
  }

  public VMCert getCert() {
    // Sign a certificate with the provided public key and authority's private key
    // NOTE: This step should be conducted in a remote server
    byte[] trusted_issuer_private_key_bytes =
        {48, 123, 2, 1, 0, 48, 19, 6, 7, 42, -122, 72, -50, 61, 2, 1, 6, 8, 42, -122, 72, -50, 61, 3, 1, 1, 4, 97, 48, 95, 2, 1, 1, 4, 24, 4, -28, 35, 44, 86, 91, -47, -17, -111, -11, -5, 43, 6, -27, -37, 6, -117, 125, -109, 36, -108, 77, 40, -21, -96, 10, 6, 8, 42, -122, 72, -50, 61, 3, 1, 1, -95, 52, 3, 50, 0, 4, 78, -128, 57, -65, 14, -48, -78, -83, 19, 114, -5, -66, 71, -22, 65, 70, 30, 105, -101, 110, -18, 107, 96, -103, -59, 91, 34, -102, -106, 42, -1, 80, -44, -53, -63, 16, -14, -109, -56, -17, -5, 86, -124, -109, -125, 7, -11, 75};

    PrivateKey trusted_issuer_private_key;
    try {
       trusted_issuer_private_key = KeyFactory.getInstance("ECDSA", BOUNCY_CASTLE_PROVIDER).generatePrivate(
           new PKCS8EncodedKeySpec(trusted_issuer_private_key_bytes)
      );
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    long valid_thru = new Date().getTime() / 1000 + 7 * 24 * 3600;

    ByteBuffer buffer = ByteBuffer.allocate(VMCert.PUBLIC_KEY_SIZE + 8);
    buffer.put(pub_key.getEncoded());
    buffer.putLong(valid_thru);
    byte[] signature = VMCommon.sign(trusted_issuer_private_key, buffer.array());

    return new VMCert(pub_key.getEncoded(), valid_thru, signature);
  }

//  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
//  public static String bytesToHex(byte[] bytes) {
//    char[] hexChars = new char[bytes.length * 2];
//    for (int j = 0; j < bytes.length; j++) {
//      int v = bytes[j] & 0xFF;
//      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
//      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
//    }
//    return new String(hexChars);
//  }

//  public static void main(String args[]) {
//    System.out.println(Security.getProviders("AlgorithmParameters.EC")[0].getService("AlgorithmParameters", "EC").getAttribute("SupportedCurves"));
//    new VMKey().getCert();
//  }
}


