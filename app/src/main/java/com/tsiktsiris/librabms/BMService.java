package com.tsiktsiris.librabms;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;


public class BMService extends Service {
    private String CONTROL_BATTERY_CAPACITY = "/sys/class/power_supply/battery/capacity";

    private String CONTROL_BMS_CAPACITY_RAW = "/sys/class/power_supply/bms/capacity_raw";
    private String CONTROL_BMS_CHARGE_NOW_RAW = "/sys/class/power_supply/bms/charge_now_raw";
    private String CONTROL_BMS_CHARGE_FULL_DESIGN_RAW = "/sys/class/power_supply/bms/charge_full_design";

    final int EMPTYMAH = 100000; //MHA which battery is considered empty

    private int charge_now_raw;
    private int charge_full_design;
    private int capacity;

    Handler ServiceHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        ServiceHandler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sudo("");
        Toast.makeText(this, "BMService Started", Toast.LENGTH_LONG).show();
        charge_full_design = getControlValue(CONTROL_BMS_CHARGE_FULL_DESIGN_RAW);
        calc_capacity();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        Toast.makeText(this, "BMService Stopped", Toast.LENGTH_LONG).show();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    private void calc_capacity() {

        Log.v("BMS", "charge_full_design=" + charge_full_design);
        int charge_now_raw = getControlValue(CONTROL_BMS_CHARGE_NOW_RAW);
        Log.v("BMS", "charge_now_raw=" + charge_now_raw);
        int capacity = charge_now_raw * 100 / charge_full_design - EMPTYMAH;
        Log.v("BMS", "capacity=" + capacity);

        setControlValue(capacity, CONTROL_BATTERY_CAPACITY);

        ServiceHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                calc_capacity();
            }
        }, 2000);
    }


    private int getControlValue(String CONTROL) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(CONTROL));

            String line = reader.readLine();
            return new Integer(line.toString());
        } catch (IOException e) {
            return -1;
        }
    }

    private void setControlValue(int value, String CONTROL) {
        sudo("echo " + value + ">" + CONTROL);
    }

    private void sudo(String command){
        try {
            Process process = Runtime.getRuntime().exec("su");
            OutputStream os = process.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeBytes(command + "\n");
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


