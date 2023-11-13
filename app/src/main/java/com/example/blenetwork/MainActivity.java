package com.example.blenetwork;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
  public static BLENAdapter BLEN_adapter;
  private Intent BLEN_intent;

  public static boolean iUserAdimin = false;
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

  int sum_22 = GlobalVariables.sum111;
  String message_type = "0";
  String message_text = "0";
  ArrayList<BLENMessage> display_message = new ArrayList<BLENMessage>();
  String type_department = "ALL";
  String type_display = "ALL";


  public void sendMessage() {
    if (BLEN_adapter == null) {
      return;
    }

    final boolean[] send_as_verified = {true};
    Spinner spinner = findViewById(R.id.spinner_display);
    String display_type = spinner.getSelectedItem().toString();



    if(display_type.equals("RANK")){

      View dialog_view = View.inflate(this, R.layout.rating_dialogue, null);







      if (BLEN_adapter.cert_setup) {
        ((SwitchCompat)dialog_view.findViewById(R.id.message_verified_switch)).setOnCheckedChangeListener(
                (compoundButton, b) -> send_as_verified[0] = b
          );
        }
      else {
          dialog_view.findViewById(R.id.message_verified_switch).setVisibility(View.GONE);
        }



      new MaterialAlertDialogBuilder(this)
              .setTitle("Broadcast:")
              .setNeutralButton("Cancel", null)
              .setPositiveButton("Send", (dialogInterface, i) -> {
                String game_type = ((Spinner)dialog_view.findViewById(R.id.spinner_type_game)).getSelectedItem().toString();
                String spinner_score = ((Spinner)dialog_view.findViewById(R.id.spinner_score)).getSelectedItem().toString();
                int rating_game_int  = Integer.parseInt(spinner_score);

                if(game_type.equals("roller_coaster")){
                  GlobalVariables.sum_rating_roller_coaster_int = GlobalVariables.sum_rating_roller_coaster_int + rating_game_int;
                  GlobalVariables.sum_rater_roller_coaster++; //评分人数+1
                  GlobalVariables.average_rating_roller_coaster_int = (double)GlobalVariables.sum_rating_roller_coaster_int/(double)GlobalVariables.sum_rater_roller_coaster;
                  GlobalVariables.average_rating_roller_coaster =String.valueOf(GlobalVariables.average_rating_roller_coaster_int); //int类型转换为String类型
                  GlobalVariables.compound = "The rating was successful！\n\n" + "rating of roller_coaster： " + GlobalVariables.average_rating_roller_coaster + "\n" + "rating of carousel: " + GlobalVariables.average_rating_carousel + "\n" + "rating of kart_racing: " + GlobalVariables.average_rating_kart_racing;

                }
                else if (game_type.equals("carousel")) {
                  GlobalVariables.sum_rating_carousel_int = GlobalVariables.sum_rating_carousel_int + rating_game_int;
                  GlobalVariables.sum_rater_carousel++; //评分人数+1
                  GlobalVariables.average_rating_carousel_int = (double)GlobalVariables.sum_rating_carousel_int/(double)GlobalVariables.sum_rater_carousel;
                  GlobalVariables.average_rating_carousel =String.valueOf(GlobalVariables.average_rating_carousel_int); //int类型转换为String类型
                  GlobalVariables.compound = "The rating was successful! \n\n" + "rating of roller_coaster： " + GlobalVariables.average_rating_roller_coaster + "\n" + "rating of carousel: " + GlobalVariables.average_rating_carousel + "\n" + "rating of kart_racing: " + GlobalVariables.average_rating_kart_racing;

                }
                else if (game_type.equals("kart_racing")) {
                  GlobalVariables.sum_rating_kart_racing_int = GlobalVariables.sum_rating_kart_racing_int + rating_game_int;
                  GlobalVariables.sum_rater_kart_racing++; //评分人数+1
                  GlobalVariables.average_rating_kart_racing_int = (double)GlobalVariables.sum_rating_kart_racing_int/(double)GlobalVariables.sum_rater_kart_racing;
                  GlobalVariables.average_rating_kart_racing =String.valueOf(GlobalVariables.average_rating_kart_racing_int); //int类型转换为String类型
                  GlobalVariables.compound = "The rating was successful! \n\n" + "rating of roller_coaster： " + GlobalVariables.average_rating_roller_coaster + "\n" + "rating of carousel: " + GlobalVariables.average_rating_carousel + "\n" + "rating of kart_racing: " + GlobalVariables.average_rating_kart_racing;
                }



                if (send_as_verified[0]) {
                  BLEN_adapter.broadcastVerifiedMessage(
                          display_type + "_" +
                                  "Others" + "/" +
                                  game_type + "\n" + spinner_score+ "\n\n"  + GlobalVariables.compound);
                } else {
                  BLEN_adapter.broadcastMessage(
                          display_type + "_" +
                                  "Others" + "/" +
                                  game_type + "\n" + spinner_score+ "\n\n" +GlobalVariables.compound);
                }
              })
              .setView(dialog_view)
              .show();

    }

    else{
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
                String message_type = ((Spinner)dialog_view.findViewById(R.id.spinner_rank)).getSelectedItem().toString();
                String receiving_department = ((Spinner)dialog_view.findViewById(R.id.spinner_department)).getSelectedItem().toString();
//                Spinner spinner = findViewById(R.id.spinner_display);
//                String display_type = spinner.getSelectedItem().toString();
                if (send_as_verified[0]) {
                  BLEN_adapter.broadcastVerifiedMessage(
                          display_type + "_" +
                                  receiving_department + "/" +
                                  message_type + "\n" + receiving_department + ":\n" + message_text);
                } else {
                  BLEN_adapter.broadcastMessage(
                          display_type + "_" +
                                  receiving_department + "/" +
                                  message_type + "\n" + "To " + receiving_department + ":\n" + message_text);
                }
              })
              .setView(dialog_view)
              .show();
    }


  }

  private void changeUserAdmin(CompoundButton compoundButton, boolean isChecked){
    iUserAdimin = isChecked;
    Spinner spinner = findViewById(R.id.spinner_display);
    List<String> listForSpinner = new ArrayList<>();
    ArrayAdapter<String> adapterForSpinner;
    if (iUserAdimin == false)
      listForSpinner = List.of(getResources().getStringArray(R.array.type_user));
    else
      listForSpinner = List.of(getResources().getStringArray(R.array.type_admin));
    adapterForSpinner = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, listForSpinner);
    adapterForSpinner.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    spinner.setAdapter(adapterForSpinner);
  }

  RecyclerView rv_message;
  ArrayList<BLENMessage> message_list = new ArrayList<>();
  ArrayList<BLENMessage> message_pool = new ArrayList<>();

  Map<String, ArrayList<BLENMessage> > message_dictionary = new HashMap<String, ArrayList<BLENMessage>>();
  MSRVAdapter MSRV_adapter;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final TreeMap<Long, Long> records = new TreeMap<>();
  private final TreeMap<Long, Long> latency = new TreeMap<>();
  private Timer timer;

//  //3种游戏设备各自的int类型的总和分数
//  int sum_rating_roller_coaster_int = 0;
//  int sum_rating_carousel_int = 0;
//  int sum_rating_kart_racing_int = 0;
//
//  //3种游戏各自的评分人数
//  int sum_rater_roller_coaster=0;
//  int sum_rater_carousel=0;
//  int sum_rater_kart_racing=0;
//
//  //3种游戏各自int类型的平均分数
//  double average_rating_roller_coaster_int = 0;
//  double average_rating_carousel_int = 0;
//  double average_rating_kart_racing_int = 0;
//
//  String compound = "0";
//  String average_rating_roller_coaster = "0";
//  String average_rating_carousel = "0";
//  String average_rating_kart_racing = "0";

  private final BLENService.BLENServiceListener addMessage = mes -> {
    message_pool.add(mes);
    message_dictionary.get("ALL").add(mes);

    String type = mes.getTextDepartment();
    String type_1 = mes.getTextType();
    String message_text = mes.getText();

    if (message_dictionary.containsKey(type)) {
      message_dictionary.get(type).add(mes);
    }
    if ((type.equals(type_department)  && type_1.equals(type_display)) || type_department.equals("ALL") || type_display.equals("ALL")){
      Log.d("add", type_department);
      display_message.add(mes);
    }

    mes.time_received = new Date().getTime(); // For debug only


    // Update UI
    runOnUiThread(() -> {
      MSRV_adapter.notifyDataSetChanged();
      rv_message.scrollToPosition(message_list.size() - 1);
      if (type_1.equals("ALERT")) {Log.d ("type", type_1);
        this.showAlertDialogue(message_text);
      }
    });

    String type_game = "aaa"; //游戏设备的名字
    String rating_game = "aaa"; //该游戏设备的得分
  };
//    if(type_1.equals("RANK")){
//      //求得游戏设备的名字
//      int pivot = 0;
//      int pivot_end = 0;
//      while (pivot < message_text.length() && message_text.charAt(pivot) != '+') {
//        pivot += 1;
//      }
//      type_game = message_text.substring(0,pivot);
//
//      //求得该游戏设备的评分
//      pivot += 1;
//      pivot_end = pivot + 1;
//      while (pivot_end < message_text.length() && message_text.charAt(pivot_end) != '#') {
//        pivot_end += 1;
//      }
//      rating_game = message_text.substring(pivot,pivot_end);
//    }

    //deal with different message types

//        if(message_type.equals("Rating")){
//          //计算该游戏设备的平均分数
//          int rating_game_int = Integer.parseInt(rating_game);  //将该游戏的评分转为int类型
//          if(type_game.equals("roller_coaster")){
//            sum_rating_roller_coaster_int = sum_rating_roller_coaster_int + rating_game_int;
//            sum_rater_roller_coaster++; //评分人数+1
//            average_rating_roller_coaster_int = (double)sum_rating_roller_coaster_int/(double)sum_rater_roller_coaster;
//            average_rating_roller_coaster =String.valueOf(average_rating_roller_coaster_int); //int类型转换为String类型
//            compound = "The rating was successful！\n\n" + "rating of roller_coaster： " + average_rating_roller_coaster + "\n" + "rating of carousel: " + average_rating_carousel + "\n" + "rating of kart_racing: " + average_rating_kart_racing;
//            showRank(compound);
//            break;
//          }
//          else if (type_game.equals("carousel")) {
//            sum_rating_carousel_int = sum_rating_carousel_int + rating_game_int;
//            sum_rater_carousel++; //评分人数+1
//            average_rating_carousel_int = (double)sum_rating_carousel_int/(double)sum_rater_carousel;
//            average_rating_carousel =String.valueOf(average_rating_carousel_int); //int类型转换为String类型
//            compound = "The rating was successful! \n\n" + "rating of roller_coaster： " + average_rating_roller_coaster + "\n" + "rating of carousel: " + average_rating_carousel + "\n" + "rating of kart_racing: " + average_rating_kart_racing;
//            showRank(compound);
//            break;
//          }
//          else if (type_game.equals("kart_racing")) {
//            sum_rating_kart_racing_int = sum_rating_kart_racing_int + rating_game_int;
//            sum_rater_kart_racing++; //评分人数+1
//            average_rating_kart_racing_int = (double)sum_rating_kart_racing_int/(double)sum_rater_kart_racing;
//            average_rating_kart_racing =String.valueOf(average_rating_kart_racing_int); //int类型转换为String类型
//            compound = "The rating was successful! \n\n" + "rating of roller_coaster： " + average_rating_roller_coaster + "\n" + "rating of carousel: " + average_rating_carousel + "\n" + "rating of kart_racing: " + average_rating_kart_racing;
//            showRank(compound);
//            break;
//          }
//          else showRank(compound);
//          break;
//          }
      /*
   String text = "";
    // Automatic reply
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
*/

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

  private void showAlertDialogue(String text) {
    View view = LayoutInflater.from(this).inflate(R.layout.alert_dialogue, null);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(text)
            .setTitle(R.string.alert_title)
            .setView(view)
            .create()
            .show();
  }

  private void updateMessage(String department, String display){


    ArrayList<BLENMessage> origin_message;

    origin_message = message_dictionary.get(department);

    if (display == "") {
      display_message = new ArrayList<BLENMessage>();
      for (int index = 0; index < origin_message.size(); index += 1) {
        Log.d("list", display);
        display_message.add(origin_message.get(index));
      }
    }
    else {
        display_message = new ArrayList<BLENMessage>();

      for (int index = 0; index < origin_message.size(); index += 1) {

        if (origin_message.get(index).getTextType().equals(display) || display.equals("ALL") ) {

          display_message.add(origin_message.get(index));
        }
      }
      rv_message.setAdapter(MSRV_adapter = new MSRVAdapter(display_message));
    }
    type_department = department;
    type_display = display;
    changeSumText(department);
  }

//  private void showRank(String text) {
//    View view = LayoutInflater.from(this).inflate(R.layout.rank_layout, null);
//    AlertDialog.Builder builder_rank = new AlertDialog.Builder(this);
//    builder_rank.setMessage(text)
//            .setTitle("Ranking of Gaming Facilities")
//            .setView(view)
//            .create()
//            .show();
//  }

  private void changeSumText(String selection){
    TextView sum_up = findViewById(R.id.sum_message);
    String sum;
    if (message_dictionary.get(selection).size() <= 1)
      sum = "Total " + message_dictionary.get(selection).size() + " message";
    else
      sum = "Total " + message_dictionary.get(selection).size() + " messages";
    sum_up.setText(sum);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    message_dictionary.put("Mayor's Office", new ArrayList<BLENMessage>());
    message_dictionary.put("City Council", new ArrayList<BLENMessage>());
    message_dictionary.put("Others", new ArrayList<BLENMessage>());
    message_dictionary.put("ALL", new ArrayList<BLENMessage>());

    rv_message = findViewById(R.id.recycler_view_message);
    rv_message.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
  //  rv_message.setAdapter(MSRV_adapter = new MSRVAdapter(message_dictionary.get("ALL")));
    rv_message.setAdapter(MSRV_adapter = new MSRVAdapter(display_message));

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
          runOnUiThread(() -> {
            MSRV_adapter.notifyDataSetChanged();
            Spinner spinner_select = findViewById(R.id.spinner_select);
            String selection = spinner_select.getSelectedItem().toString();
            changeSumText(selection);
          });
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

    SwitchCompat switchCompat = findViewById(R.id.user_admin_switch);
    // 为 switchCompat 添加点击事件监听器
    switchCompat.setOnCheckedChangeListener((CompoundButton compoundButton, boolean isChecked) -> changeUserAdmin(compoundButton, isChecked));

    Spinner spinner_select = findViewById(R.id.spinner_select);
    Spinner spinner_select_display = findViewById(R.id.spinner_select_type);

    spinner_select_display.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner_select_display = findViewById(R.id.spinner_select);
        String department = spinner_select_display.getSelectedItem().toString();

        List<String> listForSpinner = new ArrayList<>();
        listForSpinner = List.of(getResources().getStringArray(R.array.display_type_all));
        String selection = listForSpinner.get(pos);
        updateMessage(department, selection);
      }
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    spinner_select.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner_select_display = findViewById(R.id.spinner_select_type);
        String selection = spinner_select_display.getSelectedItem().toString();

        List<String> listForSpinner = new ArrayList<>();
        listForSpinner = List.of(getResources().getStringArray(R.array.type_all));
        String department = listForSpinner.get(pos);
        updateMessage(department, selection);
      }
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    stopService(BLEN_intent);
  }
}