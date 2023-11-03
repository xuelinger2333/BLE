package com.example.blenetwork;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Date;

class BLENMessage {
  public long time_received; // For debug only
  // 0x0
  public byte magic_word;
  public byte ttl;
  public byte type;
  // 0x3
  public long timestamp; // In seconds // TODO: Use only the last 5 bytes
  // 0x8
  public int sender_uuid;
  public int receiver_uuid;
  public int mes_hash; // Currently only a random integer value

  // 0x14
  public byte[] payload;
  public VMCert certificate;
  public byte[] signature;


  static public final byte MAGIC_WORD = (byte) 0x9c;
  static public final byte MAX_TTL = 16;
  static public final long MAX_LIFESPAN = 600; // In seconds

  static public final int MESSAGE_HEADER_SIZE = 19;

  static public final byte TYPE_ORIGINAL = 0x1;
  static public final byte TYPE_VERIFIED = 0x2;

  public byte[] getBodyBytes() {
    return ByteBuffer.allocate(MESSAGE_HEADER_SIZE + payload.length)
        .put(this.magic_word).put(this.ttl).put(this.type)
        .putInt((int)this.timestamp)
        .putInt(this.sender_uuid).putInt(this.receiver_uuid)
        .putInt(this.mes_hash)
        .put(payload).array();
  }

  // Serialize message to a byte array
  public byte[] getBytes() {
    if (isVerified()) {
      ByteBuffer t = ByteBuffer.allocate(MESSAGE_HEADER_SIZE
              + payload.length + 1
              + VMCert.PUBLIC_KEY_SIZE + 9 + certificate.signature.length
              + signature.length);
      return t.put(this.magic_word).put(this.ttl).put(this.type)
          .putInt((int)this.timestamp)
          .putInt(this.sender_uuid).putInt(this.receiver_uuid)
          .putInt(this.mes_hash)
          .put(payload)
          .put((byte)0)
          .put(certificate.public_key)
          .putLong(certificate.valid_thru)
          .put((byte)certificate.signature.length)
          .put(certificate.signature)
          .put(signature).array();
    }
    return getBodyBytes();
  }

  public String getTextType(){
    String text = new String(this.payload, StandardCharsets.UTF_8);
    String type = "MESSAGE";
    int pivot = 0;
    while (pivot < text.length() && text.charAt(pivot) != '_') {
      pivot += 1;
    }
    if (pivot >= text.length()){
      return "";
    }
    else
      return text.substring(0, pivot);
  }

  public String getText(){
    String text = new String(this.payload, StandardCharsets.UTF_8);
    String type = "MESSAGE";
    int pivot = 0;
    while (pivot < text.length() && text.charAt(pivot) != '/') {
      pivot += 1;
    }
    pivot += 1;
    if (pivot >= text.length()){
      return text;
    }
    else
      return text = text.substring(pivot);
  }
  public String getTextDepartment(){
    String text = new String(this.payload, StandardCharsets.UTF_8);
    String department = "";
    int pivot1 = 0;
    while (pivot1 < text.length() && text.charAt(pivot1) != '_') {
      pivot1 += 1;
    }
    pivot1 += 1;
    int pivot2 = pivot1;
    while (pivot2 < text.length() && text.charAt(pivot2) != '/') {
    //  Log.d("depart", String.valueOf(text.charAt(pivot2)));
      pivot2 += 1;
    }
    //pivot2 += 1;
    Log.d("depart", text.substring(pivot1, pivot2));
    if (pivot2 >= text.length()){
      return department;
    }
    else
      return text = text.substring(pivot1, pivot2);
  }

  public boolean isVerified() {
    return (type & TYPE_VERIFIED) != 0;
  }

  static public long currentTime() {
    return new Date().getTime() / 1000;
  }

  public boolean isValid() {
    long current = currentTime();
    boolean valid = magic_word == MAGIC_WORD
        && 0 < ttl && ttl <= MAX_TTL
        && current - timestamp < MAX_LIFESPAN;
    // For verified message
    if (valid && isVerified())
      if (!certificate.isValid())
        return false;
    return valid;
  }

  // Add a certificate and signature to the message
  public void addSignature(VMCert cert, PrivateKey key) {
    type |= TYPE_VERIFIED;
    this.certificate = cert;
    this.signature = VMCommon.sign(key, this.getBodyBytes());
  }

  public BLENMessage(int mes_hash, int sender_uuid, String payload) {
    this.mes_hash = mes_hash;
    this.sender_uuid = sender_uuid;
    this.receiver_uuid = 0;
    timestamp = currentTime();
    magic_word = MAGIC_WORD;
    ttl = MAX_TTL;
    type = TYPE_ORIGINAL;
    this.payload = payload.getBytes(StandardCharsets.UTF_8);
  }

  // Construct message from a byte array
  // Format: HEADER PAYLOAD CERT SIGNATURE
  // CERT format: PUBLIC_KEY VALID_THRU CERT_SIGNATURE_LENGTH CERT_SIGNATURE
  public BLENMessage(byte[] data) {
    ByteBuffer t = ByteBuffer.wrap(data);
    magic_word = t.get();
    ttl = t.get();
    type = t.get();
    timestamp = t.getInt();
    sender_uuid = t.getInt();
    receiver_uuid = t.getInt();
    mes_hash = t.getInt();
    int i;
    for (i = MESSAGE_HEADER_SIZE; i < data.length; i++) {
      if (data[i] == 0) {
        break;
      }
    }
    payload = Arrays.copyOfRange(data, MESSAGE_HEADER_SIZE, i);

    if ((type & TYPE_VERIFIED) != 0) {
      // Derive certificate and signature data from message
      byte[] verification_data = Arrays.copyOfRange(data, i + 1, data.length);
      t = ByteBuffer.wrap(Arrays.copyOfRange(verification_data,
          VMCert.PUBLIC_KEY_SIZE, VMCert.PUBLIC_KEY_SIZE + 9));
      long cert_valid_thru = t.getLong();
      int cert_sign_size = t.get();
      certificate = new VMCert(
          Arrays.copyOfRange(verification_data, 0, VMCert.PUBLIC_KEY_SIZE),
          cert_valid_thru,
          Arrays.copyOfRange(verification_data, VMCert.PUBLIC_KEY_SIZE + 9, VMCert.PUBLIC_KEY_SIZE + 9 + cert_sign_size)
      );
      signature = Arrays.copyOfRange(verification_data,
          VMCert.PUBLIC_KEY_SIZE + 9 + cert_sign_size, verification_data.length);
    }
  }
}
