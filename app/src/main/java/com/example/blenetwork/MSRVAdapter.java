package com.example.blenetwork;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
  CardView container;

  Map<String, Integer> menuMap = new HashMap<String, Integer>();

  MessageViewHolder(View view) {
    super(view);

    content = view.findViewById(R.id.message_content);
    sender = view.findViewById(R.id.message_sender);
    time = view.findViewById(R.id.message_time);
    container = view.findViewById(R.id.message_container);
    type = view.findViewById(R.id.message_type);
  }

  public void setClickListener() {
      View view = this.itemView;
      view.setOnClickListener(view1 -> {
        PopupMenu popupMenu = new PopupMenu(view1.getContext(), view1);
        //Log.d("message", (String) this.type.getText());
        String type = (String) this.type.getText();
        if (menuMap.containsKey(type) == true){
        popupMenu.getMenuInflater().inflate(menuMap.get(type), popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            if (item.getItemId() == R.id.rate) {
              rate_message(view1.getContext());
              return true;
            } else if (item.getItemId() == R.id.reply) {
              reply_message(view1.getContext());
              return true;
            }else if (item.getItemId() == R.id.complain) {
              reply_message(view1.getContext());
              return true;
            }else if (item.getItemId() == R.id.complete) {
              complete_message(view1.getContext());
              return true;
            }
            return true;
          }
        });
        popupMenu.show();
        }
      });
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

      new MaterialAlertDialogBuilder(context)
              .setTitle("Reply:")
              .setNeutralButton("Cancel", null)
              .setPositiveButton("Send", (dialogInterface, i) -> {
                String message_text = ((EditText)dialog_view.findViewById(R.id.message_text)).getText().toString();
                //String message_type = ((Spinner)dialog_view.findViewById(R.id.spinner_rank)).getSelectedItem().toString();
                String message_type = "Reply to : " + this.type.getText() + this.content.getText();
                String display_type = "MESSAGE";
                if (send_as_verified[0]) {
                  BLEN_adapter.broadcastVerifiedMessage(
                          display_type + "_" +
                                  message_type + "\n" + message_text);
                } else {
                  BLEN_adapter.broadcastMessage(
                          display_type + "_" +
                                  message_type + "\n" + message_text);
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
    String message_type = "Reply to : " + this.type.getText() + this.content.getText() ;
    String message_text = "Completed";
    BLEN_adapter.broadcastVerifiedMessage(
            display_type + "_" + message_type + "\n" + message_text);
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
    holder.menuMap.put("RANK", R.menu.survey_menu);
    holder.menuMap.put("MESSAGE", R.menu.message_menu);
   // holder.menuMap.put("ALERT", R.menu.survey_menu);

    BLENMessage message = message_list.get(position);
    String text = message.getText();
    String type = message.getTextType();
    holder.content.setText(text);
    holder.sender.setText(EmojiName.getName(message.sender_uuid));
    holder.type.setText(type);

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
