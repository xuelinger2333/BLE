package com.example.blenetwork;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class BLENService extends Service {
  public BLENService() {
  }

  public interface BLENServiceListener {
    void onMessageReceived(BLENMessage mes);
  }

  class BLENServiceBinder extends Binder {
    BLENAdapter getAdapter() {
      return BLEN_adapter;
    }

    public void setListener(BLENServiceListener listener) {
      getAdapter().setListener(listener);
    }
  }

  public BLENAdapter BLEN_adapter;
  private final IBinder binder = new BLENServiceBinder();

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    BLEN_adapter = new BLENAdapter(this);
    ((BLENetworkApplication) getApplicationContext()).adapter = BLEN_adapter;
    BLEN_adapter.startServer();
  }
}