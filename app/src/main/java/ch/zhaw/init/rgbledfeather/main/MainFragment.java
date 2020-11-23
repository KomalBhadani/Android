package ch.zhaw.init.rgbledfeather.main;

import android.app.Dialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import ch.zhaw.init.rgbledfeather.BaseFragment;
import ch.zhaw.init.rgbledfeather.R;
import ch.zhaw.init.rgbledfeather.ble.BleDevice;
import ch.zhaw.init.rgbledfeather.ble.BleManager;
import ch.zhaw.init.rgbledfeather.ble.BleUtils;
import ch.zhaw.init.rgbledfeather.databinding.FragmentMainBinding;
import ch.zhaw.init.rgbledfeather.utils.AppLog;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

public class MainFragment extends BaseFragment implements View.OnClickListener {//, BleManager.BleManagerListener

    private static final String TAG = "MainFragment";
    FragmentMainBinding binding;
    private static boolean online = false;
    private int[] mColor = { 128, 128, 128 };
    DatabaseReference myRef;
    FirebaseDatabase database;
    int selectedcolor;
    // Data
    protected BluetoothGattService mUartService;
    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final int kTxMaxCharacters = 20;
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";



    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance(Bundle bundle) {
        MainFragment fragment = new MainFragment();
        if (bundle != null) {
            fragment.setArguments(bundle);
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentMainBinding.inflate(inflater,container,false);

        init();

        return binding.getRoot();
    }

    private void init() {

        mBleManager = BleManager.getInstance(context);
        mBleManager.setBleListener(this);
        // Start services
        onServicesDiscovered();

        Bundle bundle = getArguments();
        if (bundle != null) {

            Type type = new TypeToken<BleDevice>() {
            }.getType();
            BleDevice bleDevice = new Gson().fromJson(bundle.getString("connectedDevice"),type);
            AppLog.e(TAG,"BLE device"+bleDevice);
        }

        binding.btDial.setOnClickListener(this);
        binding.btAction.setOnClickListener(this);
        binding.btLed.setOnClickListener(this);


// Write a message to the database
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();



        //myRef.setValue("Hello, World!");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_dial:{

                break;
            }
            case R.id.bt_led:{
                openColorPickerDialog();
                break;
            }
            case R.id.bt_action:{

                break;
            }
        }
    }

    private void openColorPickerDialog() {
        final Dialog dialog=new Dialog(context,R.style.DialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_colorpicker);
        dialog.setCancelable(false);
        dialog.findViewById(R.id.bt_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              // callhttpmethodforupdate data
                updateRGBColor();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updateRGBColor() {


        //write to database
        RGBColor rgbColor = new RGBColor();
        rgbColor.setRed(mColor[0]);
        rgbColor.setGreen(mColor[1]);
        rgbColor.setBlue(mColor[2]);

        //update to database
        myRef.child("rgb-led-6de8a").setValue(rgbColor);

      /*  byte r = (byte) ((selectedcolor >> 16) & 0xFF);
        byte g = (byte) ((selectedcolor >> 8) & 0xFF);
        byte b = (byte) ((selectedcolor >> 0) & 0xFF);

        ByteBuffer buffer = ByteBuffer.allocate(2 + 3 * 1).order(java.nio.ByteOrder.LITTLE_ENDIAN);

        // prefix
        String prefix = "!C";
        buffer.put(prefix.getBytes());

        // values
        buffer.put(r);
        buffer.put(g);
        buffer.put(b);

        byte[] result = buffer.array();*/

        byte red = (byte) mColor[0];
        byte green = (byte) mColor[1];
        byte blue = (byte) mColor[2];

        byte[] data = {0x43, red, green, blue};
        sendData(data);//result

/*// Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                AppLog.e(TAG, "Value is: " + value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });*/
    }

    // Returns true if host is reachable
    public static synchronized boolean isOnline() {
        return online;
    }

    public void onColorSelected(int[] color, int mcolor) {
        mColor = color;
        selectedcolor = mcolor;
        AppLog.e(TAG,"selected color: "+color);
    }




/*
    @Override
    public void onConnected() {

    }

    @Override
    public void onConnecting() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onServicesDiscovered() {
        AppLog.e(TAG,"getGattService: uuid service herererere11");
        mUartService = mBleManager.getGattService(UUID_SERVICE);

    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor) {

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }*/


}