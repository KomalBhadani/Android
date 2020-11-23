package ch.zhaw.init.rgbledfeather.ble;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.UUID;

public class BleDevice  implements Parcelable {
    public static final int kType_Uart = 1;
    private String deviceAddress;
    private String deviceName;
    private BluetoothDevice bluetothDevice;
    private List<UUID> uuids;

    public BleDevice(BluetoothDevice device, String address, String name) {
        this.bluetothDevice = device;
        this.deviceAddress = address;
        this.deviceName = name;
        this.uuids = uuids;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public BluetoothDevice getBluetothDevice() {
        return bluetothDevice;
    }
    //public List<UUID> getUUIDs() { return  uuids; }


    // Implements Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.deviceAddress);
        parcel.writeString(this.deviceName);
        parcel.writeParcelable(this.bluetothDevice, i);
        //parcel.writeParcelable(this.uuids, i);
    }

    public static final Parcelable.Creator<BleDevice> CREATOR
            = new Parcelable.Creator<BleDevice>() {
        public BleDevice createFromParcel(Parcel in) {
            return new BleDevice(in);
        }

        public BleDevice[] newArray(int size) {
            return new BleDevice[size];
        }
    };

    private BleDevice(Parcel in) {
        this.deviceAddress = in.readString();
        this.deviceName = in.readString();
        this.bluetothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
    }
}