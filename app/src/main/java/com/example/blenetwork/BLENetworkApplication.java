package com.example.blenetwork;

import android.app.Application;

import java.util.ArrayList;

public class BLENetworkApplication extends Application {
  public BLENAdapter adapter;
  public ConsoleActivity console;
  public ArrayList<String> app_logs = new ArrayList<>();
}
