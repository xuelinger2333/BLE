package com.example.blenetwork;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

class BLENDevice {
  int RID;
  String MAC;
  BluetoothGatt gatt;

  // If the device is not connected, the latest message to send is stored here.
  Queue<BLENMessage> message_buffer;

  int gatt_status;
  long last_seen;
  long last_communicate;

  void seen() {
    last_seen = new Date().getTime();
  }
  void communicated() { last_seen = last_communicate = new Date().getTime(); }

  void connect(BLENAdapter adapter) {
    adapter.bluetoothAdapter.cancelDiscovery();
    if (gatt == null) {
      try {
        final BluetoothDevice device = adapter.bluetoothAdapter.getRemoteDevice(MAC);
        if (device != null) {
          device.connectGatt(adapter.context, false, adapter.gattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
          adapter.log("[BLE Central] Cannot get remote device");
        }
      } catch (Exception e) {
        adapter.log("[BLE Central] Failed to initiate BLE connection");
      }
    } else if (gatt_status == BluetoothProfile.STATE_DISCONNECTED) {
      gatt.connect();
    }
  }

  void disconnect() {
    if (gatt == null) return;
    if (gatt_status != BluetoothGatt.STATE_DISCONNECTED) gatt.disconnect();

    gatt = null;
    gatt_status = BluetoothGatt.STATE_DISCONNECTED;
  }

  BLENDevice(int RID, String MAC) {
    this.MAC = MAC;
    this.gatt = null;
    this.RID = RID;
    this.last_seen = this.last_communicate = new Date().getTime();
    this.message_buffer = new LinkedList<BLENMessage>();
    this.gatt_status = 0;
  }
}
