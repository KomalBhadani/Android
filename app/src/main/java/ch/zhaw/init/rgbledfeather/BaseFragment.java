package ch.zhaw.init.rgbledfeather;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.nio.charset.Charset;
import java.util.Arrays;

import ch.zhaw.init.rgbledfeather.ble.BleManager;
import ch.zhaw.init.rgbledfeather.ble.BleUtils;
import ch.zhaw.init.rgbledfeather.utils.AppLog;
import ch.zhaw.init.rgbledfeather.utils.AppPref;

public class BaseFragment extends Fragment implements BleManager.BleManagerListener {
    private static final String TAG = "BaseFragment";
    public Context context;
    public AppPref appPref;

    Dialog dialog;
    //data for ble
    // Service Constants
    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_DFU = "00001530-1212-EFDE-1523-785FEABCD123";
    public static final int kTxMaxCharacters = 20;

    // Data
    protected BleManager mBleManager;
    protected BluetoothGattService mUartService;
    private boolean isRxNotificationEnabled = false;

    final private Handler sendDataTimeoutHandler = new Handler();
    private Runnable sendDataRunnable = null;
    private SendDataCompletionHandler sendDataCompletionHandler = null;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appPref=AppPref.getInstance(context);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context =context;
    }

    public void hideKeyboard() {
        View view = ((Activity) context).getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
    public void gotoActivity(Class className, Bundle bundle, boolean isClearStack)
    {
        Intent intent=new Intent(context,className);

        if(bundle!=null)
            intent.putExtras(bundle);

        if(isClearStack)
        {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }
    public boolean hasPermission(String[] permissions) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        }
        return true;
    }


    public void showToast(String msg)
    {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }



    public  void showLoading()
    {
        if(dialog!=null)
            hideLoading();

        if(dialog==null)
        {
            dialog=new Dialog(context);
            if(dialog.getWindow()!=null)
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.loading_bar);
        }
        if(!dialog.isShowing())
            dialog.show();
    }
    public  void hideLoading()
    {
        if(dialog!=null && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }

    public void changeFrag(Fragment fragment, boolean isBackStack, boolean isPopBack)
    {
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        if(isPopBack)
        {
            fm.popBackStack();
        }
        if(isBackStack)
        {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.replace(R.id.fragment,fragment);
        fragmentTransaction.commit();
    }



    // region Send Data to UART
    protected void sendData(String text) {
        final byte[] value = text.getBytes(Charset.forName("UTF-8"));
        sendData(value);
    }

    protected void  sendData(byte[] data) {
        if (mUartService != null) {
            // Split the value into chunks (UART service has a maximum number of characters that can be written )
            for (int i = 0; i < data.length; i += kTxMaxCharacters) {
                final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + kTxMaxCharacters, data.length));
                mBleManager.writeService(mUartService, UUID_TX, chunk);
            }
        } else {
            Log.e(TAG, "Uart Service not discovered. Unable to send data");
        }
    }

    // Send data to UART and add a byte with a custom CRC
    protected void sendDataWithCRC(byte[] data) {

        // Calculate checksum
        byte checksum = 0;
        for (byte aData : data) {
            checksum += aData;
        }
        checksum = (byte) (~checksum);       // Invert

        // Add crc to data
        byte dataCrc[] = new byte[data.length + 1];
        System.arraycopy(data, 0, dataCrc, 0, data.length);
        dataCrc[data.length] = checksum;

        // Send it
        Log.d(TAG, "Send to UART: " + BleUtils.bytesToHexWithSpaces(dataCrc));
        sendData(dataCrc);
    }
    // endregion

    // region SendDataWithCompletionHandler
    protected interface SendDataCompletionHandler {
        void sendDataResponse(String data);
    }



    public void sendData(byte[] data, SendDataCompletionHandler completionHandler) {

        if (completionHandler == null) {
            sendData(data);
            return;
        }

        if (!isRxNotificationEnabled) {
            AppLog.e(TAG, "sendData warning: RX notification not enabled. completionHandler will not be executed");
        }

        if (sendDataRunnable != null ||     sendDataCompletionHandler != null) {
            AppLog.e(TAG, "sendData error: waiting for a previous response");
            return;
        }

        AppLog.e(TAG, "sendData");
        sendDataCompletionHandler = completionHandler;
        sendDataRunnable = new Runnable() {
            @Override
            public void run() {
                AppLog.e(TAG, "sendData timeout");
                final SendDataCompletionHandler dataCompletionHandler = sendDataCompletionHandler;

                sendDataRunnable = null;
                sendDataCompletionHandler = null;

                dataCompletionHandler.sendDataResponse(null);
            }
        };

        sendDataTimeoutHandler.postDelayed(sendDataRunnable, 2 * 1000);
        sendData(data);

    }

    protected boolean isWaitingForSendDataResponse() {
        return sendDataRunnable != null;
    }

    // endregion

    // region BleManagerListener  (used to implement sendData with completionHandler)

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

        mUartService = mBleManager.getGattService(UUID_SERVICE);
    }

    protected void enableRxNotifications() {
        isRxNotificationEnabled = true;
        mBleManager.enableNotification(mUartService, UUID_RX, true);
    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {
        // Check if there is a pending sendDataRunnable
        if (sendDataRunnable != null) {
            if (characteristic.getService().getUuid().toString().equalsIgnoreCase(UUID_SERVICE)) {
                if (characteristic.getUuid().toString().equalsIgnoreCase(UUID_RX)) {

                    AppLog.e(TAG, "sendData received data");
                    sendDataTimeoutHandler.removeCallbacks(sendDataRunnable);
                    sendDataRunnable = null;

                    if (sendDataCompletionHandler != null) {
                        final byte[] bytes = characteristic.getValue();
                        final String data = new String(bytes, Charset.forName("UTF-8"));

                        final SendDataCompletionHandler dataCompletionHandler = sendDataCompletionHandler;
                        sendDataCompletionHandler = null;
                        dataCompletionHandler.sendDataResponse(data);
                    }
                }
            }
        }
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor) {

    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }

    @Override
    public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout, int status) {
        AppLog.e(TAG,"onConnectionUpdated :::: "+interval);
        if(interval>0){
            mBleManager.connect(context,gatt.getDevice().getAddress());
        }
    }

    // endregion
}
