package com.example.blenetwork;

import static java.util.Arrays.copyOf;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.*;
import java.util.Date;

public class VMCert {
  static public final int PUBLIC_KEY_SIZE = 75;
  static final byte[] trusted_issuer_public_key =
      {48, 73, 48, 19, 6, 7, 42, -122, 72, -50, 61, 2, 1, 6, 8, 42, -122, 72, -50, 61, 3, 1, 1, 3, 50, 0, 4, 78, -128, 57, -65, 14, -48, -78, -83, 19, 114, -5, -66, 71, -22, 65, 70, 30, 105, -101, 110, -18, 107, 96, -103, -59, 91, 34, -102, -106, 42, -1, 80, -44, -53, -63, 16, -14, -109, -56, -17, -5, 86, -124, -109, -125, 7, -11, 75};
  byte[] public_key;
  byte[] signature;
  long valid_thru; // in seconds

  public VMCert(byte[] public_key, long valid_thru, byte[] signature) {
    this.public_key = copyOf(public_key, PUBLIC_KEY_SIZE);
    this.signature = copyOf(signature, signature.length);
    this.valid_thru = valid_thru;
  }

  private byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(PUBLIC_KEY_SIZE + 8);
    buffer.put(public_key);
    buffer.putLong(valid_thru);
    return buffer.array();
  }

  public boolean isValid() {
    // Verify certificate public key
    if (!VMCommon.verifySign(trusted_issuer_public_key, serialize(), signature)) {
      return false;
    }

    // Verify certificate expiration time
    long current_timestamp = new Date().getTime() / 1000;
    if (this.valid_thru < current_timestamp) {
      return false;
    }

    return true;
  }

//  public boolean isValid(PublicKey pk) {
//    return VMCommon.verifySign(pk, serialize(), signature);
//  }
}
