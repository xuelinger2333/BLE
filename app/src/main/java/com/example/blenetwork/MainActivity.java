package com.example.blenetwork;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
  public BLENAdapter BLEN_adapter;
  private Intent BLEN_intent;

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (grantResults.length > 0 &&
        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.d("Main", String.format(Locale.getDefault(), "Permission granted. (Code: %d)", requestCode));
    } else {
      Log.d("Main", String.format(Locale.getDefault(), "Permission denied. (Code: %d)", requestCode));
    }
  }

  private Intent console_intent;
  public void showConsole() {
    startActivity(console_intent);
  }

  private Intent device_list_intent;
  public void showDeviceList() {
    startActivity(device_list_intent);
  }

  public void sendMessage() {
    if (BLEN_adapter == null) {
      return;
    }

    final boolean[] send_as_verified = {true};
    View dialog_view = View.inflate(this, R.layout.broadcast_dialog, null);
    if (BLEN_adapter.cert_setup) {
      ((SwitchCompat)dialog_view.findViewById(R.id.message_verified_switch)).setOnCheckedChangeListener(
          (compoundButton, b) -> send_as_verified[0] = b
      );
    } else {
      dialog_view.findViewById(R.id.message_verified_switch).setVisibility(View.GONE);
    }

    new MaterialAlertDialogBuilder(this)
        .setTitle("Broadcast:")
        .setNeutralButton("Cancel", null)
        .setPositiveButton("Send", (dialogInterface, i) -> {
          String message_text = ((EditText)dialog_view.findViewById(R.id.message_text)).getText().toString();
          if (send_as_verified[0]) {
            BLEN_adapter.broadcastVerifiedMessage(message_text);
          } else {
            BLEN_adapter.broadcastMessage(message_text);
          }
        })
        .setView(dialog_view)
        .show();
  }

  RecyclerView rv_message;
  ArrayList<BLENMessage> message_list = new ArrayList<>();
  MSRVAdapter MSRV_adapter;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final TreeMap<Long, Long> records = new TreeMap<>();
  private final TreeMap<Long, Long> latency = new TreeMap<>();
  private Timer timer;

  private final BLENService.BLENServiceListener addMessage = mes -> {
    message_list.add(mes);
    mes.time_received = new Date().getTime(); // For debug only

    // Update UI
    runOnUiThread(() -> {
      MSRV_adapter.notifyDataSetChanged();
      rv_message.scrollToPosition(message_list.size() - 1);
    });

    // Automatic reply

    String text = new String(mes.payload, StandardCharsets.UTF_8);
    final int[] count = {0};
    if (text.equals("start") && mes.sender_uuid == BLEN_adapter.getID()) {
      timer = new Timer();
      timer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          records.put((long) count[0], new Date().getTime());
          int num = 50;
          BLEN_adapter.broadcastVerifiedMessage("PING " + Integer.toString(count[0]));
          count[0]++;
          if (count[0] == num)
            cancel();
        }
      }, 0, 1000);
    }
    else if (text.startsWith("PING ") && mes.sender_uuid != BLEN_adapter.getID()) {
      BLEN_adapter.broadcastMessage("ACK " + text.substring(5));
    } else if (text.startsWith("ACK ")) {
      long i = Long.parseLong(text.substring(4));
      if (records.containsKey(i)) {
        long delta = mes.time_received - records.get(i);
        latency.put(i, delta);
        records.remove(i);
      }
    } else if (text.equals("no")) {
      StringBuilder res = new StringBuilder();
      int sum = 0;
      res.append("Received: " + latency.size() + ", ");
      res.append("Timeout: " + records.size() + ", ");
      for (TreeMap.Entry<Long, Long> latency_record : latency.entrySet()) {
        res.append(Long.toString(latency_record.getValue()));
        res.append(" ");
        sum += latency_record.getValue() / 2;
      }
      String time_str = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
      sum = sum / latency.size();
      String str = time_str + " [PING RESULT] " + res.toString();
      BLEN_adapter.broadcastMessage(str);
      BLEN_adapter.broadcastMessage(String.valueOf(sum));
      ((BLENetworkApplication) getApplicationContext()).app_logs.add(str);
      if (((BLENetworkApplication) getApplicationContext()).console != null) {
        ((BLENetworkApplication) getApplicationContext()).console.log(str);
      }
    }


//    String text = new String(mes.payload, StandardCharsets.UTF_8);
//    final int[] count = {0};
//    if (text.equals("start") && mes.sender_uuid == BLEN_adapter.getID()) {
//      timer = new Timer();
//      timer.scheduleAtFixedRate(new TimerTask() {
//        @Override
//        public void run() {
//          BLENMessage test_message = BLEN_adapter.broadcastVerifiedMessage("PING " + Integer.toString(count[0]));
//          int ll = test_message.getBytes().length;
//          count[0]++;
//        }
//      }, 0, 500);
//    }

    /*if (mes.sender_uuid == BLEN_adapter.getID()) return;
    if (mes.content.equals("start")) {
      timer = new Timer();
      timer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
          records.put(inc_id[0], new Date().getTime());
          BLEN_adapter.broadcastMessage("PING " + Long.toString(inc_id[0]));
          inc_id[0] += 1;
        }
      }, 0, 600);
    } else if (mes.content.startsWith("PING ")) {
      BLEN_adapter.broadcastMessage("ACK " + mes.content.substring(5));
    } else if (mes.content.startsWith("ACK ")) {
      long num = Long.parseLong(mes.content.substring(4));
      if (records.containsKey(num)) {
        long delta = mes.time_received - records.get(num);
        latency.put(num, delta);
        records.remove(num);
      }
    } else if (mes.content.equals("stop")) {
      timer.cancel();
      StringBuilder res = new StringBuilder();
      res.append("Received: " + latency.size() + ", ");
      res.append("Timeout: " + records.size() + ", ");
      for (TreeMap.Entry<Long, Long> latency_record : latency.entrySet()) {
        res.append(Long.toString(latency_record.getValue()));
        res.append(" ");
      }
      String time_str = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
      String str = time_str + " [PING RESULT] " + res.toString();
      ((BLENetworkApplication) getApplicationContext()).app_logs.add(str);
      if (((BLENetworkApplication) getApplicationContext()).console != null) {
        ((BLENetworkApplication) getApplicationContext()).console.log(str);
      }
    }*/
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    rv_message = findViewById(R.id.recycler_view_message);
    rv_message.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    rv_message.setAdapter(MSRV_adapter = new MSRVAdapter(message_list));

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
      // For SDK version 30 or lower
      if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
      || checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_DENIED) {
        ActivityCompat.requestPermissions(this,
            new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            }, 1);
      }
    } else {
      // For SDK version 31 or higher
      if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_DENIED
      || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED
      || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
        ActivityCompat.requestPermissions(this,
            new String[]{
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            }, 1);
      }
    }

    BLEN_intent = new Intent(this, BLENService.class);

    startService(BLEN_intent);
    final long[] inc_id = {100000, 0};
    bindService(BLEN_intent, new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        BLEN_adapter = ((BLENService.BLENServiceBinder)service).getAdapter();
        ((BLENService.BLENServiceBinder)service).setListener(addMessage);
        ((BLENetworkApplication) getApplicationContext()).adapter = BLEN_adapter;
        if (getSupportActionBar() != null) {
          getSupportActionBar().setTitle("BLENetwork - " + EmojiName.getName(BLEN_adapter.getID()));
        }
        BLEN_adapter.setScheduledScan();
        scheduler.scheduleAtFixedRate(() -> {
          runOnUiThread(() -> MSRV_adapter.notifyDataSetChanged());
        }, 5, 5, TimeUnit.SECONDS);
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        BLEN_adapter = null;
        ((BLENetworkApplication) getApplicationContext()).adapter = null;
        if (getSupportActionBar() != null) {
          getSupportActionBar().setTitle("BLENetwork");
        }
      }
    }, BIND_AUTO_CREATE);

    console_intent = new Intent(MainActivity.this, ConsoleActivity.class);
    device_list_intent = new Intent(MainActivity.this, DeviceList.class);

    FloatingActionButton fab_console = findViewById(R.id.fab_console);
    fab_console.setOnClickListener(v -> showConsole());
    FloatingActionButton fab_devices = findViewById(R.id.fab_devices);
    fab_devices.setOnClickListener(v -> showDeviceList());
    MaterialButton button_send_message = findViewById(R.id.button_send_message);
    button_send_message.setOnClickListener(v -> sendMessage());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    stopService(BLEN_intent);
  }
}