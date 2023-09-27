package com.example.blenetwork;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Date;

class DLRVAdapter extends RecyclerView.Adapter<DLRVAdapter.DeviceViewHolder> {
  public static class DeviceViewHolder extends RecyclerView.ViewHolder {
    TextView address, rid, status;
    DeviceViewHolder(View view) {
      super(view);
      this.address = (TextView) view.findViewById(R.id.device_address);
      this.rid = (TextView) view.findViewById(R.id.device_rid);
      this.status = (TextView) view.findViewById(R.id.device_connection_status);
    }
  }

  private ArrayList<BLENDevice> devices;
  private BLENAdapter BLEN_adapter;

  @Override
  public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
  }

  @Override
  public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_card, parent, false);
    return new DeviceViewHolder(v);
  }

  static private final String[] gatt_state_text = {
    "Disconnected", "Connecting", "Connected", "Disconnecting"
  };

  @Override
  public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
    BLENDevice target = devices.get(position);
    holder.address.setText(target.MAC);
    holder.rid.setText(EmojiName.getName(target.RID));

    if (target.gatt_status == 0) {
      long last_seen_sec = (new Date().getTime() - target.last_seen) / 15000;
      String last_seen_text = "";
      if (last_seen_sec == 0) last_seen_text = "Nearby";
      else last_seen_text = "Last seen " + (last_seen_sec * 15) + "s ago";
      holder.status.setText(last_seen_text);
    } else {
      holder.status.setText(gatt_state_text[target.gatt_status]);
    }
  }

  @Override
  public int getItemCount() {
    return devices.size();
  }

  DLRVAdapter(ArrayList<BLENDevice> devices, BLENAdapter adapter) {
    this.devices = devices;
    this.BLEN_adapter = adapter;
  }
}

public class DeviceList extends AppCompatActivity {

  private BLENAdapter BLEN_adapter;
  private DLRVAdapter DLRV_adapter;

  private void updateList() {
    if (BLEN_adapter.devicesList.size() == 0) {
      findViewById(R.id.label_no_device_found).setVisibility(View.VISIBLE);
    } else {
      findViewById(R.id.label_no_device_found).setVisibility(View.INVISIBLE);
    }
    DLRV_adapter.notifyDataSetChanged();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_device_list);
    BLEN_adapter = ((BLENetworkApplication) getApplicationContext()).adapter;

    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.device_list_rv);
    RecyclerView.LayoutManager llm = new GridLayoutManager(getApplicationContext(), 2);
    recyclerView.setLayoutManager(llm);
    DLRV_adapter = new DLRVAdapter(BLEN_adapter.devicesList, BLEN_adapter);
    recyclerView.setAdapter(DLRV_adapter);
    if (BLEN_adapter.devicesList.size() == 0) {
      findViewById(R.id.label_no_device_found).setVisibility(View.VISIBLE);
    } else {
      findViewById(R.id.label_no_device_found).setVisibility(View.INVISIBLE);
    }

    if (getSupportActionBar() != null)
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    FloatingActionButton fab_refresh = (FloatingActionButton) findViewById(R.id.fab_refresh);
    fab_refresh.setOnClickListener(v -> updateList());
  }

  @Override
  public boolean onSupportNavigateUp() {
    finish();
    return true;
  }
}