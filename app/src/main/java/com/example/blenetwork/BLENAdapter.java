package com.example.blenetwork;

import static android.content.Context.BLUETOOTH_SERVICE;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BLENAdapter {
  private static final UUID BLEServiceUUID = UUID.fromString("740A6F95-1CDC-452A-8E9D-2673C6FCC1CD");
  private static final UUID CharContentUUID = UUID.fromString("8762C1A3-2934-436E-8FFE-D1E50F1372C6");
  private static final UUID AdvertiseDataUUID = UUID.fromString("00009C27-0000-1000-8000-00805F9B34FB");

  private static final int MESSAGE_BUFFER_SIZE = 512;

  private VMCert cert;
  private VMKey key;
  // Set to true when a certificate is setup for verified messages
  public boolean cert_setup = false;

  protected final BluetoothAdapter bluetoothAdapter =
      BluetoothAdapter.getDefaultAdapter();
  private final BluetoothLeScanner bluetoothLeScanner =
      bluetoothAdapter.getBluetoothLeScanner();
  private final BluetoothLeAdvertiser bluetoothLeAdvertiser =
      bluetoothAdapter.getBluetoothLeAdvertiser();
  private final BluetoothManager bluetoothManager;

  private int device_RID;

  public int getID() {
    return device_RID;
  }

  ArrayList<BLENDevice> devicesList = new ArrayList<>();

  TreeMap<Long, BLENMessage> message_list = new TreeMap<>();
  HashSet<Integer> message_ids = new HashSet<>();
  HashSet<BigInteger> block_list = new HashSet<BigInteger>();

  TreeMap<String, byte[]> message_fragment_buffer = new TreeMap<>();

  AdvertiseCallback leAdvertiseCallback = new AdvertiseCallback() {
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
      super.onStartSuccess(settingsInEffect);
      log("[BLE Peripheral] Advertising started successfully.");
    }

    @Override
    public void onStartFailure(int errorCode) {
      super.onStartFailure(errorCode);
      log(String.format("[BLE Peripheral] Failed to start advertising (Code: %d).", errorCode));
    }
  };

  private void startAdvertising() {
    if (bluetoothLeAdvertiser == null) {
      log("[BLE Peripheral] Failed to create advertiser.");
      return;
    }

    log("[BLE Peripheral] Initiating advertising");
    AdvertiseSettings settings = new AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .build();

    ParcelUuid advertise_uuid = new ParcelUuid(AdvertiseDataUUID);
    AdvertiseData data = new AdvertiseData.Builder()
        .setIncludeDeviceName(false)
        .setIncludeTxPowerLevel(false)
        .addServiceData(advertise_uuid, ByteBuffer.allocate(24).putInt(device_RID).array())
        .build();

    bluetoothLeAdvertiser.startAdvertising(settings, data, leAdvertiseCallback);
  }

  private void stopAdvertising() {
    if (bluetoothLeAdvertiser == null) return;
    bluetoothLeAdvertiser.stopAdvertising(leAdvertiseCallback);
    log("[BLE Peripheral] Bluetooth Advertising stopped.");
  }

  private BLENDevice findDeviceByRID(int RID) {
    for (BLENDevice network_device : devicesList) {
      if (network_device.RID == RID) {
        return network_device;
      }
    }
    return null;
  }

  // Finds a known device by its MAC address
  private BLENDevice findDeviceByMAC(String address) {
    for (BLENDevice network_device : devicesList) {
      if (network_device.MAC.equals(address)) {
        return network_device;
      }
    }
    return null;
  }

  public void blockSender(BLENMessage message) {
    block_list.add(new BigInteger(message.certificate.public_key));
  }

  private boolean addMessage(BLENMessage message) {
    if (!message.isValid()) return false;
    if (message_ids.contains(message.mes_hash)) {
      return false;
    }
    if (message.isVerified() && block_list.contains(new BigInteger(message.certificate.public_key))) {
      return false;
    }
    if (message.timestamp > new Date().getTime()) {
      message.timestamp = new Date().getTime();
    }
    message_ids.add(message.mes_hash);
    message_list.put(message.timestamp, message);
    if (this.listener != null) {
      this.listener.onMessageReceived(message);
    }
    log("[BLE Peripheral] New message from: " + message.sender_uuid + ", length: " + message.payload.length);
    return true;
  }

  // Called when a message is received from a device
  private void handleMessage(BluetoothDevice device) {
    byte[] buffer = message_fragment_buffer.get(device.getAddress());
    if (buffer == null) return;
    BLENMessage message = new BLENMessage(buffer);
    message_fragment_buffer.remove(device.getAddress());

    if (!addMessage(message)) return;

    // Clear expired messages in storage
    while (message_list.size() > 0
        && message_list.firstKey() + BLENMessage.MAX_LIFESPAN + 10 < BLENMessage.currentTime()) {
      message_ids.remove(message_list.firstEntry().getValue().mes_hash);
      message_list.pollFirstEntry();
    }

    // Forward message to other devices of the network
    if (message.ttl <= 1) return;
    message.ttl -= 1;
    if ((message.type & BLENMessage.TYPE_ORIGINAL) != 0) {
      message.type -= BLENMessage.TYPE_ORIGINAL;
    }
    for (BLENDevice network_device : devicesList) {
      if (!network_device.MAC.equals(device.getAddress())
          && network_device.RID != message.sender_uuid)
        sendMessage(message, network_device);
    }
  }

  // BLE peripheral handler
  protected final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
      super.onConnectionStateChange(device, status, newState);
      log(String.format("[BLE Peripheral] %s in connection state %d (status %d)", device.getAddress(), newState, status));
    }

    @Override
    public void onCharacteristicReadRequest(
        BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
      bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
    }

    private boolean handleMessageFragment(BluetoothDevice device, int offset, byte[] value, boolean isPartial) {
      if (!isPartial) {
        try {
          addMessage(new BLENMessage(value));
        } catch (Exception e) {
          return false;
        }
        return true;
      }
      byte[] buffer = message_fragment_buffer.get(device.getAddress());
      if (buffer == null) {
        buffer = new byte[MESSAGE_BUFFER_SIZE];
        message_fragment_buffer.put(device.getAddress(), buffer);
      }
      if (offset + value.length >= MESSAGE_BUFFER_SIZE)
        return false;
      System.arraycopy(value, 0, buffer, offset, value.length);
      return true;
    }

    @Override
    public void onCharacteristicWriteRequest(
        BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic,
        boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
      super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
      if (characteristic.getUuid().equals(CharContentUUID)
          && handleMessageFragment(device, offset, value, preparedWrite)) {
        BLENDevice network_device = findDeviceByMAC(device.getAddress());
        if (network_device != null) {
          network_device.communicated();
        }
        if (responseNeeded) {
          bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }
      } else if (responseNeeded) {
        bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, value);
      }
    }

    @Override
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
      super.onExecuteWrite(device, requestId, execute);
      bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
      handleMessage(device);
    }
  };

  private BluetoothGattServer bluetoothGattServer;

  public void startServer() {
    if (bluetoothGattServer != null) {
      stopServer();
      return;
    }
    bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback);
    if (bluetoothGattServer == null) {
      log("[BLE Peripheral] Unable to start GATT server");
      return;
    }
    BluetoothGattService service = new BluetoothGattService(
        BLEServiceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
    BluetoothGattCharacteristic content_char = new BluetoothGattCharacteristic(
        CharContentUUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);

    service.addCharacteristic(content_char);
    log("[BLE Peripheral] GATT Server started.");
    bluetoothGattServer.addService(service);
    startAdvertising();
  }

  public void stopServer() {
    if (bluetoothGattServer == null) return;

    log("[BLE Peripheral] Connected to " + getPeriDevices().size() + " device(s)");

    bluetoothGattServer.close();
    bluetoothGattServer = null;
    stopAdvertising();
    log("[BLE Peripheral] GATT server stopped.");
  }

  public List<BluetoothDevice> getPeriDevices() {
    return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
  }

  // BLE central handler
  protected final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      super.onConnectionStateChange(gatt, status, newState);
      log(String.format("[BLE Central] %s in connection state %d (status %d)", gatt.getDevice().getAddress(), newState, status));
      BLENDevice network_device = findDeviceByMAC(gatt.getDevice().getAddress());
      if (network_device != null) {
        network_device.gatt = gatt;
        network_device.gatt_status = newState;
        if (newState == BluetoothProfile.STATE_CONNECTED) {
          gatt.discoverServices();
          network_device.seen();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
          log("[BLE Central] Connection closed (" + gatt.getDevice() + ")");
          network_device.disconnect();
        }
      }
      if (newState == BluetoothGatt.STATE_DISCONNECTED) {
        gatt.close();
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      super.onServicesDiscovered(gatt, status);
      BLENDevice network_device = findDeviceByMAC(gatt.getDevice().getAddress());
      if (status == BluetoothGatt.GATT_SUCCESS) {
        if (network_device != null && network_device.message_buffer.size() > 0)
          sendMessage(network_device.message_buffer.poll(), network_device);
      } else {
        log("[BLE Central] Error while discovering service on (" + gatt.getDevice() + "), closing connection");
        if (network_device != null) network_device.disconnect();
        else gatt.close();
      }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      super.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      super.onCharacteristicWrite(gatt, characteristic, status);
      if (status == BluetoothGatt.GATT_SUCCESS || status == BluetoothGatt.CONNECTION_PRIORITY_HIGH) {
        if (characteristic.getUuid().equals(CharContentUUID)) {
          log("[BLE Central] Content written successfully");
          BLENDevice network_device = findDeviceByMAC(gatt.getDevice().getAddress());
          if (network_device != null) {
            network_device.seen();
            if (network_device.message_buffer.size() > 0) {
              sendMessage(network_device.message_buffer.poll(), network_device);
              network_device.communicated();
            }
          }
        }
      } else {
        log("[BLE Central] Failed to write remote characteristic on " + gatt.getDevice() +
            " (status " + status + ")");
      }
    }
  };

  // Sends a message to a specific device
  public void sendMessage(BLENMessage message, BLENDevice network_device) {
    BluetoothGatt gatt = network_device.gatt;
    if (gatt == null || network_device.gatt_status != BluetoothProfile.STATE_CONNECTED) {
      network_device.message_buffer.offer(message);
      network_device.connect(this);
      return;
    }

    BluetoothGattService service = gatt.getService(BLEServiceUUID);
    if (service != null) {
      BluetoothGattCharacteristic content_char = service.getCharacteristic(CharContentUUID);
      if (content_char != null) {
        content_char.setValue(message.getBytes());
        if (!gatt.writeCharacteristic(content_char)) {
          log("[BLE Central] Failed to send message on device " + gatt.getDevice().getAddress());
        } else {
          network_device.seen();
          return;
        }
      } else {
        log("[BLE Central] Failed to discover GATT character on device " + gatt.getDevice().getAddress());
      }
    } else {
      log("[BLE Central] No available service found on device " + gatt.getDevice().getAddress());
    }
    network_device.message_buffer.offer(message);
  }

  // Broadcasts a message to all nearby devices, appending certificate and signature
  public BLENMessage  broadcastVerifiedMessage(String mes) {
    final int mesRID = new Random().nextInt();
    BLENMessage message = new BLENMessage(mesRID, device_RID, mes);
    message.addSignature(cert, key.priv_key);
    if (this.listener != null) {
      this.listener.onMessageReceived(message);
    }
    message_ids.add(message.mes_hash);
    message_list.put(message.timestamp, message);
    for (BLENDevice network_device : devicesList) {
      sendMessage(message, network_device);
    }
    return message;
  }

  // Broadcasts a message to all nearby devices
  public BLENMessage broadcastMessage(String mes) {
    final int mesRID = new Random().nextInt();
    BLENMessage message = new BLENMessage(mesRID, device_RID, mes);
    if (this.listener != null)
      this.listener.onMessageReceived(message);
    message_ids.add(message.mes_hash);
    message_list.put(message.timestamp, message);
    for (BLENDevice network_device : devicesList) {
      sendMessage(message, network_device);
    }
    return message;
  }

  // Remove inactive devices
  private void cleanup() {
    Iterator<BLENDevice> network_device_iterator = devicesList.iterator();
    while (network_device_iterator.hasNext()) {
      BLENDevice network_device = network_device_iterator.next();
      if (network_device.last_seen <= new Date().getTime() - 3 * 60 * 1000) {
        network_device.disconnect();
        log("[Cleaner] Removing " + network_device.MAC + " due to inactivity");
        network_device_iterator.remove();
      } else if (network_device.gatt != null
          && network_device.gatt_status == BluetoothGatt.STATE_CONNECTED
          && network_device.last_communicate <= new Date().getTime() - 15 * 1000) {
        log("[Cleaner] Disconnecting from " + network_device.MAC + " due to inactivity");
        network_device.disconnect();
      }
    }
  }

  public void scan() {
    if (bluetoothAdapter == null || bluetoothLeScanner == null) {
      log("[Scan] Bluetooth not supported");
      return;
    }
    if (!bluetoothAdapter.isEnabled()) {
      log("[Scan] Bluetooth not enabled");
      return;
    }

    String permission;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) permission = Manifest.permission.BLUETOOTH_SCAN;
    else permission = Manifest.permission.ACCESS_FINE_LOCATION;
    if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
      scanLeDevice();
    } else {
      log("[Scan] Bluetooth permission not granted.");
    }
  }

  private final ScanCallback leScanCallback = new ScanCallback() {
    private void processDevice(ScanResult result) {
      BluetoothDevice device = result.getDevice();
      if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE
          && device.getType() != BluetoothDevice.DEVICE_TYPE_UNKNOWN) return;

      byte[] RID_bytes = result.getScanRecord().getServiceData(new ParcelUuid(AdvertiseDataUUID));
      if (RID_bytes == null) return;
      int RID = ByteBuffer.wrap(RID_bytes).getInt();

      BLENDevice network_device = findDeviceByMAC(device.getAddress());
      if (network_device != null) {
        network_device.seen();
        if (network_device.RID != RID) {
          network_device.RID = RID;
        }
        return;
      }

      String address = device.getAddress();
      log("[Scan] Found new device: " + address + " " + EmojiName.getName(RID) + " (" + RID + ")");
      network_device = new BLENDevice(RID, address);
      devicesList.add(network_device);
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      super.onScanResult(callbackType, result);
      processDevice(result);
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
      super.onBatchScanResults(results);
      for (ScanResult result : results) {
        processDevice(result);
      }
    }
  };

  private boolean scanning;
  private final Handler handler = new Handler();

  // Initializes scanning process
  private void scanLeDevice() {
    if (!scanning) {
      // Stops scanning after a predefined scan period.
      final long SCAN_PERIOD = 10000;
      handler.postDelayed(() -> {
        scanning = false;
        bluetoothLeScanner.stopScan(leScanCallback);
        log("[Scan] Scan completed");
      }, SCAN_PERIOD);

      scanning = true;
      bluetoothLeScanner.startScan(null, new ScanSettings.Builder()
          .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
          .build(), leScanCallback);
      log("[Scan] Scan initiated");
    } else {
      scanning = false;
      bluetoothLeScanner.stopScan(leScanCallback);
      log("[Scan] Scan aborted.");
    }
  }

  Context context;

  protected void log(String str) {
    Log.d("BLENAdapter", str);
    String time_str = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
    str = time_str + " " + str;
    ((BLENetworkApplication) context.getApplicationContext()).app_logs.add(str);
    if (((BLENetworkApplication) context.getApplicationContext()).console != null) {
      ((BLENetworkApplication) context.getApplicationContext()).console.log(str);
    }
  }

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  public void setScheduledScan() {
    scheduler.scheduleAtFixedRate(this::scan, 0, 15, TimeUnit.SECONDS);
    scheduler.scheduleAtFixedRate(this::cleanup, 0, 5, TimeUnit.SECONDS);
  }

  BLENService.BLENServiceListener listener;
  public void setListener(BLENService.BLENServiceListener listener) {
    this.listener = listener;
  }

  public BLENAdapter(Context ctx) {
    context = ctx;
    bluetoothManager = (BluetoothManager) ctx.getSystemService(BLUETOOTH_SERVICE);
    do {
      device_RID = new Random().nextInt();
    } while (device_RID == 0);

    // Generate key and certificate for verified messages
    try {
      Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
      key = new VMKey();
      cert = key.getCert();
      cert_setup = true;
    } catch (RuntimeException e) {
      cert_setup = false;
    }

    log("RID: " + device_RID);
  }
}
