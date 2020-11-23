package ch.zhaw.init.rgbledfeather.main;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListAdapter;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import ch.zhaw.init.rgbledfeather.BaseFragment;
import ch.zhaw.init.rgbledfeather.R;
import ch.zhaw.init.rgbledfeather.ble.BleDevice;
import ch.zhaw.init.rgbledfeather.ble.BleDevicesScanner;
import ch.zhaw.init.rgbledfeather.ble.BleManager;
import ch.zhaw.init.rgbledfeather.ble.BleUtils;
import ch.zhaw.init.rgbledfeather.ble.DeviceAdapter;
import ch.zhaw.init.rgbledfeather.databinding.FragmentScannerBinding;
import ch.zhaw.init.rgbledfeather.utils.AppLog;
import ch.zhaw.init.rgbledfeather.utils.ExpandableHeightExpandableListView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ScannerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ScannerFragment extends BaseFragment implements View.OnClickListener, BleUtils.ResetBluetoothAdapterListener, DeviceAdapter.Connectlistenre {// BleManager.BleManagerListener,

    private static final String TAG = "ScannerFragment";
    FragmentScannerBinding binding;

    private final static long kMinDelayToUpdateUI = 200;    // in milliseconds
    private static final String kGenericAttributeService = "00001801-0000-1000-8000-00805F9B34FB";
    private static final String kServiceChangedCharacteristic = "00002A05-0000-1000-8000-00805F9B34FB";

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    DeviceAdapter deviceAdapter;
    ArrayList<BleDevice> bleDevices = new ArrayList<>();

    private boolean mIsScanPaused = true;
    private BleDevicesScanner mScanner;

    BleDevice selectedbledevice;

    private long mLastUpdateMillis;
    private final static int kComponentsNameIds[] = {
            R.string.scan_connectservice_info,
            R.string.scan_connectservice_uart,
            R.string.scan_connectservice_pinio,
            R.string.scan_connectservice_controller,
            R.string.scan_connectservice_beacon,
            R.string.scan_connectservice_neopixel,
    };

    public ScannerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *

     * @return A new instance of fragment ScannerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ScannerFragment newInstance() {
        ScannerFragment fragment = new ScannerFragment();

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
       // return inflater.inflate(R.layout.fragment_scanner, container, false);
        binding = FragmentScannerBinding.inflate(inflater,container,false);

        init();
        listeners();

        return binding.getRoot();
    }

    private void listeners() {
        binding.scanButton.setOnClickListener(this);
    }

    private void init() {

        // Init variables
        mBleManager = BleManager.getInstance(context);


        deviceAdapter = new DeviceAdapter();
        deviceAdapter.setlisteners(this);
        binding.list.setAdapter(deviceAdapter);

        // Initialize BluetoothAdapter through BluetoothManager
        /*final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Initialize Handler
        mHandler = new Handler();




        // Connect to device onClick
        binding.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BleDevice device = mAdapter.getItem(position);
                AppLog.e(TAG, "ADDRESS: " + device.getDeviceAddress());

                Bundle bundle = new Bundle();
                bundle.putString("connectedDevice",new Gson().toJson(device));

                changeFrag(MainFragment.newInstance(bundle),true,false);


                *//*Intent messageIntent = new Intent(this, MessageActivity.class);
                AppLog.e(TAG, "ADDRESS: " + device.getDeviceAddress());
                messageIntent.putExtra("connectedDevice", device);
                startActivity(messageIntent);*//*
            }
        });

*/       binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                bleDevices.clear();
                stopScanning();
                startScan(null);

                binding.swipeRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                }, 500);
            }
        });

        requestLocationPermissionIfNeeded();
        onServicesDiscovered();

    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (!hasPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION})) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can scan for Bluetooth peripherals");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.scanButton:{
                onClickScan();
                break;
            }
        }
    }

    public void onClickScan() {
        startScan(null);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set listener
        mBleManager.setBleListener(this);

        // Autostart scan
        autostartScan();

        // Update UI
        updateUI();
    }

    private void autostartScan() {
        if (BleUtils.getBleStatus(context) == BleUtils.STATUS_BLE_ENABLED) {
            // If was connected, disconnect
            mBleManager.disconnect();

            // Force restart scanning
            if (bleDevices != null) {      // Fixed a weird bug when resuming the app (this was null on very rare occasions even if it should not be)
                bleDevices.clear();
            }
            startScan(null);
        }
    }

    @Override
    public void onPause() {
        // Stop scanning
        if (mScanner != null && mScanner.isScanning()) {
            mIsScanPaused = true;
            stopScanning();
        }

        super.onPause();
    }




    private void startScan(final UUID[] servicesToScan) {
        Log.d(TAG, "startScan");

        // Stop current scanning (if needed)
        stopScanning();

        // Configure scanning
        BluetoothAdapter bluetoothAdapter = BleUtils.getBluetoothAdapter(getActivity().getApplicationContext());
        if (BleUtils.getBleStatus(context) != BleUtils.STATUS_BLE_ENABLED) {
            Log.w(TAG, "startScan: BluetoothAdapter not initialized or unspecified address.");
        } else {
            mScanner = new BleDevicesScanner(bluetoothAdapter, servicesToScan, new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    AppLog.e(TAG,"ble device: "+device + ",name: "+device.getName());


                    // Check that the device was not previously found
                    Boolean iscontain = false;
                    for (BleDevice deviceData : bleDevices) {
                        if (deviceData.getDeviceAddress().equals(""+device)) {
                            iscontain = true;
                          /*  BleDevice bleDevice = new BleDevice(device,device.getAddress(),device.getName());
                            bleDevices.add(bleDevice);*/
                            break;
                        }
                    }

                    if(!iscontain){
                        BleDevice bleDevice = new BleDevice(device,device.getAddress(),device.getName());
                        bleDevices.add(bleDevice);
                        deviceAdapter.addDevice(bleDevices);

                    }
                   // decodeScanRecords(deviceData);

                    // Update device data
                    long currentMillis = SystemClock.uptimeMillis();
                    if (bleDevices == null || currentMillis - mLastUpdateMillis > kMinDelayToUpdateUI) {          // Avoid updating when not a new device has been found and the time from the last update is really short to avoid updating UI so fast that it will become unresponsive
                        mLastUpdateMillis = currentMillis;

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateUI();
                            }
                        });
                    }

                }
            });

            // Start scanning
            mScanner.start();
        }

        // Update UI
        updateUI();
    }


    private void stopScanning() {
        // Stop scanning
        if (mScanner != null) {
            mScanner.stop();
            mScanner = null;
        }

        updateUI();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    AppLog.e(TAG, "Location permission granted");
                    // Autostart scan
                    autostartScan();
                    // Update UI
                    updateUI();
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Bluetooth Scanning not available");
                    builder.setMessage("Since location access has not been granted, the app will not be able to scan for Bluetooth peripherals");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                break;
            }
            default:
                break;
        }
    }

    private void updateUI() {
        // Scan button
        boolean isScanning = mScanner != null && mScanner.isScanning();
        binding.scanButton.setText(getString(isScanning ? R.string.scan_scanbutton_scanning : R.string.scan_scanbutton_scan));

        // Show list and hide "no devices" label
        final boolean isListEmpty = bleDevices == null || bleDevices.size() == 0;
        binding.nodevicesTextView.setVisibility(isListEmpty ? View.VISIBLE : View.GONE);
        binding.devicesScrollView.setVisibility(isListEmpty ? View.GONE : View.VISIBLE);

        // devices list
        deviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onConnected() {
        AppLog.e(TAG,"device connected");

       /* Bundle bundle = new Bundle();
        bundle.putString("connectedDevice: ",new  Gson().toJson(selectedbledevice));
        gotoActivity(MainActivity.class,bundle,false);
*/
       /* Bundle bundle = new Bundle();
        bundle.putString("connectedDevice: ",new  Gson().toJson(selectedbledevice));
        gotoActivity(MainActivity.class,bundle,false);*/
        checkSketchVersion();

    }

    @Override
    public void onConnecting() {

    }

    @Override
    public void onDisconnected() {
        AppLog.e(TAG, " onDisconnected");
        hideLoading();
    }

    @Override
    public void onServicesDiscovered() {
        super.onServicesDiscovered();
        enableRxNotifications();

    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor) {

    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }


    // region ResetBluetoothAdapterListener
    @Override
    public void resetBluetoothCompleted() {
        AppLog.e(TAG, "Reset completed -> Resume scanning");
        resumeScanning();
    }

    private void resumeScanning() {
        if (mIsScanPaused) {
            startScan(null);
            mIsScanPaused = mScanner == null;
        }
    }

    @Override
    public void clicktoconnect(BleDevice device) {
        stopScanning();

         this.selectedbledevice = device;
        BluetoothDevice selecteddevice = device.getBluetothDevice();


      /*  if (selecteddevice.getType() == device.kType_Uart) {      // if is uart, show all the available activities
            showChooseDeviceServiceDialog(selecteddevice);
        } else {                          // if no uart, then go directly to info
            showToast("No UART service found. ");

        }*/

        boolean isConnecting = mBleManager.connect(context, device.getDeviceAddress());
        if (isConnecting) {
            showLoading();
        }


      /*  Bundle bundle = new Bundle();
        bundle.putString("connectedDevice: ",new  Gson().toJson(device));
        gotoActivity(MainActivity.class,bundle,false);
*/

    }

    private void checkSketchVersion() {
        // Send version command and check if returns a valid response
        Log.d(TAG, "Command: get Version");


        byte command = 0x56;
        byte[] data = {command};
        sendData(data,  new SendDataCompletionHandler() {
            @Override
            public void sendDataResponse(String data) {
                AppLog.e(TAG,"response here: "+data);
                boolean isSketchDetected = false;
                if (data != null) {
                    isSketchDetected = data.startsWith("Neopixel");
                }

                Log.d(TAG, "isNeopixelAvailable: " + (isSketchDetected ? "yes" : "no"));
                onSketchDetected(isSketchDetected);
            }
        });
        sendData(data);


    }

    private void onSketchDetected(boolean isSketchDetected) {
        //this.mIsSketchDetected = isSketchDetected;


        if (isSketchDetected) {


            setupNeopixel();
        }else {
            hideLoading();
            showToast("Response not found");
        }
      //  setupNeopixel();
    }

    private void  setupNeopixel() {
        Log.d(TAG, "Command: Setup");
//Command: Setup: type: 82, width: 10 , height: 1 ,stride: 10 ; pixelType. 82, components - 3
        //for line strip size 10
        //Command: Setup: type: 82, width: 20 , height: 1 ,stride: 20device.components. 3 ; pixelType. 82   /for strip 20

        int pixelType = 82;
        byte[] data = {0x53, 20, 1, 3, 20, (byte) pixelType, (byte) ((byte) (pixelType >> 8) & 0xff)};
        sendData(data, new SendDataCompletionHandler() {
            @Override
            public void sendDataResponse(String data) {
                boolean success = false;
                AppLog.e(TAG,"responese: "+ data);
                if (data != null) {
                    success = data.startsWith("OK");
                }
                Log.d(TAG, "setup success: " + (success ? "yes" : "no"));

                onNeopixelSetupFinished(success);
            }
        });

    }

    private void onNeopixelSetupFinished(boolean success) {
        hideLoading();
        if (success) {

            Bundle bundle = new Bundle();
            bundle.putString("connectedDevice: ",new  Gson().toJson(selectedbledevice));
            gotoActivity(MainActivity.class,bundle,false);
           // clearBoard(Color.WHITE);

        }


    }


    private void clearBoard(int color) {
        Log.d(TAG, "Command: Clear");

            byte red = (byte) Color.red(color);
            byte green = (byte) Color.green(color);
            byte blue = (byte) Color.blue(color);

            byte[] data = {0x43, red, green, blue};
            sendData(data);

    }

}
