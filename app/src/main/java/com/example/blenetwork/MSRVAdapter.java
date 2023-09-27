package com.example.blenetwork;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

class MessageViewHolder extends RecyclerView.ViewHolder {
  TextView content, sender, time;
  CardView container;
  MessageViewHolder(View view) {
    super(view);
    content = view.findViewById(R.id.message_content);
    sender = view.findViewById(R.id.message_sender);
    time = view.findViewById(R.id.message_time);
    container = view.findViewById(R.id.message_container);

    view.setOnClickListener(view1 -> {
      PopupMenu popupMenu = new PopupMenu(view1.getContext(), view1);
      popupMenu.getMenuInflater().inflate(R.menu.message_menu, popupMenu.getMenu());
      popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
          if (item.getItemId() == R.id.block_sender) {
            return true;
          } else if (item.getItemId() == R.id.reply) {
            return true;
          }
          return true;
        }
      });
      popupMenu.show();
    });
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
    BLENMessage message = message_list.get(position);
    holder.content.setText(new String(message.payload, StandardCharsets.UTF_8));
    holder.sender.setText(EmojiName.getName(message.sender_uuid));
    if (message.isVerified()) {
      holder.sender.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_verified_12, 0);
    } else {
      holder.sender.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    long sec_diff = new Date().getTime() / 1000 - message.timestamp;
    String time_text = "";
    if (sec_diff < 20) time_text = "Just now";
    else if (sec_diff < 60) time_text = "Less than a minute ago";
    else if (sec_diff < 120) time_text = "1 min ago";
    else time_text = (sec_diff / 60) + " mins ago";

    int distance = BLENMessage.MAX_TTL - message.ttl;
    String distance_text = "";
    if (distance == 0) distance_text = "Sent by you";
    else if (distance == 1) distance_text = "From connected device";
    else distance_text = "Through " + distance + " devices";

    String display_text = distance_text + " · " + time_text;
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
