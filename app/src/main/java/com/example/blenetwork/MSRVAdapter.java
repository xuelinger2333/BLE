package com.example.blenetwork;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;



class MessageViewHolder extends RecyclerView.ViewHolder {
  TextView content, sender, time, type;
  String department;
  CardView container;
  Spinner content_word;

  Map<String, Integer> menuMap = new HashMap<String, Integer>();
//
//  //2种调查问卷各自的int类型的总和分数
//  int sum_survey_num_int = 0;
//
//  //2种问卷各自的参与人数
//  int sum_rater_survey_num=0;
//
//  int sum_rater_lower2_survey_num_int = 0;
//
//
//  String sum_rater_lower2_survey_num = "0";
//
//
//  //3种游戏各自int类型的平均分数
//  double average_rating_survey_num_int = 0;
//  String compound_survey = "0";
//
//  String average_rating_survey_num = "0";
//
//  String type_survey = "aaa"; //问卷类型
//
//  int sum_rater_very_good =0;
//  int sum_rater_good =0;
//  int sum_rater_normal =0;
//  int sum_rater_bad =0;

  MessageViewHolder(View view) {
    super(view);

    content = view.findViewById(R.id.message_content);
    sender = view.findViewById(R.id.message_sender);
    time = view.findViewById(R.id.message_time);
    container = view.findViewById(R.id.message_container);
    type = view.findViewById(R.id.message_type);
    content_word = view.findViewById((R.id.spinner_reply_word));
  }

  public void setClickListener() {
      View view = this.itemView;
      String type = (String) this.type.getText();
      if (menuMap.containsKey(type) == true){
        Log.d("message", "yes");
      view.setOnClickListener(view1 -> {
        PopupMenu popupMenu = new PopupMenu(view1.getContext(), view1);
        popupMenu.getMenuInflater().inflate(menuMap.get(type), popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            if (item.getItemId() == R.id.rate) {
              rate_message(view1.getContext());
              return true;
            } else if (item.getItemId() == R.id.reply && type.equals("SURVEY")) {
              reply_message(view1.getContext());
              return true;
            }else if(item.getItemId() == R.id.reply && type.equals("SURVEYWORD")){
              reply_message_survey_word(view1.getContext());
            } else if (item.getItemId() == R.id.complain) {
              reply_message(view1.getContext());
              return true;
            }else if (item.getItemId() == R.id.complete) {
              complete_message(view1.getContext());
              return true;
            }
            return true;
          }
        });
        popupMenu.show();});
        }else{
          view.setClickable(false);
        }

    }

  private void reply_message(Context context) {
    BLENAdapter BLEN_adapter = MainActivity.BLEN_adapter;
      if (BLEN_adapter == null) {
        return;
      }

      final boolean[] send_as_verified = {true};
      View dialog_view = View.inflate(context, R.layout.reply_dialogue, null);
      if (BLEN_adapter.cert_setup) {
        ((SwitchCompat)dialog_view.findViewById(R.id.message_verified_switch)).setOnCheckedChangeListener(
                (compoundButton, b) -> send_as_verified[0] = b
        );
      } else {
        dialog_view.findViewById(R.id.message_verified_switch).setVisibility(View.GONE);
      }

    //求得问卷类型
    int pivot = 0;
    String str = this.content.getText().toString();
    while (pivot < str.length() && str.charAt(pivot) != '\n') {
      pivot += 1;
    }
    GlobalVariables.type_survey = str.substring(0,pivot);

      new MaterialAlertDialogBuilder(context)
              .setTitle("Reply:")
              .setNeutralButton("Cancel", null)
              .setPositiveButton("Send", (dialogInterface, i) -> {
                String message_text = ((EditText)dialog_view.findViewById(R.id.message_text)).getText().toString();
                //String message_type = ((Spinner)dialog_view.findViewById(R.id.spinner_rank)).getSelectedItem().toString();
                String department = this.department;
                String message_type = "Reply to : " + this.type.getText() + this.content.getText();
                String display_type = "MESSAGE";

                if(GlobalVariables.type_survey.equals("Survey_num") && (Integer.parseInt(message_text))>=1 && (Integer.parseInt(message_text))<=10){
                  int message_text_int = Integer.parseInt(message_text);
                  GlobalVariables.sum_survey_num_int = GlobalVariables.sum_survey_num_int + message_text_int;
                  GlobalVariables.sum_rater_survey_num++; //评分人数+1
                  if(message_text_int <= 2)GlobalVariables.sum_rater_lower2_survey_num_int++;
                  GlobalVariables.average_rating_survey_num_int = (double)GlobalVariables.sum_survey_num_int/(double)GlobalVariables.sum_rater_survey_num;

                  //int类型转换为String类型
                  GlobalVariables.average_rating_survey_num =String.valueOf(GlobalVariables.average_rating_survey_num_int);
                  //average_rating_survey_num =String.format("%.1f",average_rating_survey_num_int);

                  GlobalVariables.sum_rater_lower2_survey_num =String.valueOf(GlobalVariables.sum_rater_lower2_survey_num_int);

                  GlobalVariables.compound_survey = "The rating was successful！\n\n" +
                          "rating of survey_num： " + GlobalVariables.average_rating_survey_num + "\n" +
                          "Sum of participants： " + GlobalVariables.sum_rater_survey_num + "\n" +
                          "Sum of ratings below two: " + GlobalVariables.sum_rater_lower2_survey_num + "\n";
                }
                else{
                  GlobalVariables.compound_survey = "The rating was unsuccessful！Please retry. \n\n" +
                          "rating of survey_word： " + GlobalVariables.average_rating_survey_num + "\n" +
                          "Number of participants： " + GlobalVariables.sum_rater_survey_num + "\n" +
                          "Sum of ratings below tow: " + GlobalVariables.sum_rater_lower2_survey_num + "\n";
                }



                if (send_as_verified[0]) {
                  BLEN_adapter.broadcastVerifiedMessage(
                          display_type + "_" + department + "/" +
                                  message_type + "\n" + message_text+ "\n\n" + GlobalVariables.compound_survey);
                } else {
                  BLEN_adapter.broadcastMessage(
                          display_type + "_" +  department + "/" +
                                  message_type + "\n" + message_text+ "\n\n" + GlobalVariables.compound_survey);
                }
              })
              .setView(dialog_view)
              .show();
  }

  private void reply_message_survey_word(Context context) {

    BLENAdapter BLEN_adapter = MainActivity.BLEN_adapter;
    if (BLEN_adapter == null) {
      return;
    }
    final boolean[] send_as_verified = {true};
    View dialog_view = View.inflate(context, R.layout.reply_word_dialague, null);
    if (BLEN_adapter.cert_setup) {
      ((SwitchCompat)dialog_view.findViewById(R.id.message_verified_switch)).setOnCheckedChangeListener(
              (compoundButton, b) -> send_as_verified[0] = b
      );
    } else {
      dialog_view.findViewById(R.id.message_verified_switch).setVisibility(View.GONE);
    }

    new MaterialAlertDialogBuilder(context)
            .setTitle("Reply:")
            .setNeutralButton("Cancel", null)
            .setPositiveButton("Send", (dialogInterface, i) -> {



//              String message_text = ((EditText)dialog_view.findViewById(R.id.message_text)).getText().toString();
              String message_word = ((Spinner)dialog_view.findViewById(R.id.spinner_reply_word)).getSelectedItem().toString();
              String message_type = "Reply to : " + this.type.getText() + "\n" + this.content.getText();
              String display_type = "MESSAGE";


              switch(message_word) {
                case "VERY_GOOD":
                  ++GlobalVariables.sum_rater_very_good;
                  break;
                case "GOOD":
                  ++GlobalVariables.sum_rater_good;
                  break;
                case "NORMAL":
                  ++GlobalVariables.sum_rater_normal;
                  break;
                case "BAD":
                  ++GlobalVariables.sum_rater_bad;
                  break;
              }
              int sum_max=0;


              String compound_word_details = "Number of people who chose each option:\n" +
                      "VERY_GOOD: " + GlobalVariables.sum_rater_very_good + "\n" +
                      "GOOD: " + GlobalVariables.sum_rater_good + "\n" +
                      "NORMAL: " + GlobalVariables.sum_rater_normal + "\n" +
                      "BAD: " + GlobalVariables.sum_rater_bad + "\n";


              String compound_word_popular_option = "The most popular option is:\n";
              sum_max = Math.max(GlobalVariables.sum_rater_very_good,Math.max(GlobalVariables.sum_rater_good,Math.max(GlobalVariables.sum_rater_normal,GlobalVariables.sum_rater_bad)));

              if(sum_max == GlobalVariables.sum_rater_very_good) compound_word_popular_option = compound_word_popular_option + " VERY_GOOD";
              if(sum_max == GlobalVariables.sum_rater_good) compound_word_popular_option = compound_word_popular_option + " GOOD";
              if(sum_max == GlobalVariables.sum_rater_normal) compound_word_popular_option = compound_word_popular_option + " NORMAL";
              if(sum_max == GlobalVariables.sum_rater_bad) compound_word_popular_option = compound_word_popular_option + " BAD";

              if (send_as_verified[0]) {
                BLEN_adapter.broadcastVerifiedMessage(
                        display_type + "_" +
                                message_type   +"\n"+ message_word + "\n\n" + compound_word_details+ compound_word_popular_option);
              } else {
                BLEN_adapter.broadcastMessage(
                        display_type + "_" +
                                message_type  +"\n"+ message_word + "\n\n" + compound_word_details +"\n" + compound_word_popular_option);
              }
            })
            .setView(dialog_view)
            .show();
  }

  private void rate_message(Context context) {

  }
  private void complete_message(Context context) {
    BLENAdapter BLEN_adapter = MainActivity.BLEN_adapter;
    if (BLEN_adapter == null) {
      return;
    }
    String display_type = "NULL";
    String department = "NULL";
    String message_type = "Reply to : " + this.type.getText() + "·" + this.content.getText() ;
    String message_text = "Completed";
    BLEN_adapter.broadcastVerifiedMessage(
            display_type + "_" + department + "/" + message_type + "\n" + message_text);
  }

  public void setClick(boolean flag){
    this.itemView.setClickable(flag);
    }
  }


public class MSRVAdapter extends RecyclerView.Adapter<MessageViewHolder> {
  ArrayList<BLENMessage> message_list;

  @NonNull
  @Override
  public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.message_layout, parent, false);
    return new MessageViewHolder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {

    holder.menuMap.put("SURVEY", R.menu.survey_menu);
    holder.menuMap.put("SURVEYWORD", R.menu.survey_menu);
    holder.menuMap.put("RANK", R.menu.survey_menu);
    if (MainActivity.iUserAdimin == true)
      holder.menuMap.put("MESSAGE", R.menu.message_menu);
    else
      holder.menuMap.remove("MESSAGE");
   // holder.menuMap.put("ALERT", R.menu.survey_menu);

    BLENMessage message = message_list.get(position);
    String text = message.getText();
    String type = message.getTextType();
    holder.content.setText(text);
    holder.sender.setText(EmojiName.getName(message.sender_uuid));
    holder.type.setText(type);
    holder.department = message.getTextDepartment();

    holder.setClickListener();

    if (message.isVerified()) {
      holder.sender.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_verified_12, 0);
    } else {
      holder.sender.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    long sec_diff = new Date().getTime() / 1000 - message.timestamp;
    String time_text = "";
    if (sec_diff < 20) time_text = "Just now";
    else if (sec_diff < 60) time_text = "Less than 1 min ago";
    else if (sec_diff < 120) time_text = "1 min ago";
    else time_text = (sec_diff / 60) + " mins ago";

    int distance = BLENMessage.MAX_TTL - message.ttl;
    String distance_text = "";
    if (distance == 0) distance_text = "Sent by you";
    else if (distance == 1) distance_text = "From connected device";
    else distance_text = "Through " + distance + " devices";

    String display_text = "·" + distance_text + "·" + time_text;
    holder.time.setText(display_text);

    Random generator = new Random(message.sender_uuid);
    holder.container.setCardBackgroundColor(Color.argb(255, 80 + generator.nextInt(100),
        80 + generator.nextInt(100), 80 + generator.nextInt(100)));
  }

  @Override
  public int getItemCount() {
    return message_list.size();
  }

  MSRVAdapter(ArrayList<BLENMessage> message_list) {
    this.message_list = message_list;
  }

}
