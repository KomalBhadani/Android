package ch.zhaw.init.rgbledfeather.ble;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ch.zhaw.init.rgbledfeather.R;
import ch.zhaw.init.rgbledfeather.databinding.DeviceListItemBinding;
import ch.zhaw.init.rgbledfeather.utils.AppLog;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private static final String TAG = "DeviceAdapter";
    List<BleDevice> devices = new ArrayList<>();
    Context context;
    Connectlistenre listenrs;
    public DeviceAdapter() {
        
    }

    public void setlisteners(Connectlistenre connectlistener){
        this.listenrs = connectlistener;

    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context=parent.getContext();
        /*DeviceListItemBinding binding =  DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.device_list_item, parent, false);
        return new ViewHolder(binding);*/

        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
       // deviceName.setText(currentDevice.getDeviceName());
//        deviceAddress.setText(currentDevice.getDeviceAddress());

        holder.tvaddress.setText(devices.get(position).getDeviceAddress());
        holder.tvname.setText(devices.get(position).getDeviceName());
    }

    public void addDevice(ArrayList<BleDevice> devices){
        AppLog.e(TAG,"devices: "+devices);
        this.devices = devices;
        notifyDataSetChanged();
    }
    @Override
    public int getItemCount() {
        AppLog.e(TAG,"devicesiye: "+devices.size());
        return devices.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvaddress, tvname;
        Button btconnect;
        public ViewHolder(View itemView) {
            super(itemView);

            tvaddress = itemView.findViewById(R.id.deviceAddress);
            tvname = itemView.findViewById(R.id.deviceName);
            btconnect = itemView.findViewById(R.id.bt_connect);

            btconnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                listenrs.clicktoconnect(devices.get(getAdapterPosition()));

                }
            });
        }
    }

    public interface Connectlistenre{
        public void clicktoconnect(BleDevice device);
    }
}
