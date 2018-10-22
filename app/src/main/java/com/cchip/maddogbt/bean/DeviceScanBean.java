package com.cchip.maddogbt.bean;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Date: 2018/10/17-11:29
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class DeviceScanBean implements Parcelable {

    String macAddress;
    String deviceName;
    String serialNumber = "";
    int rssi;
    ScanResult scanResult;

    public DeviceScanBean(BluetoothDevice bluetoothDevice, int rssi, ScanResult scanResult) {
        // TODO Auto-generated constructor stub
        this.macAddress = bluetoothDevice.getAddress();
        this.deviceName = bluetoothDevice.getName();
        this.rssi = rssi;
        this.scanResult = scanResult;
    }

    public DeviceScanBean(Parcel source) {
        // TODO Auto-generated constructor stub
        this.macAddress = source.readString();
        this.deviceName = source.readString();
        this.rssi = source.readInt();
        this.serialNumber = source.readString();
    }


    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    @Override
    public boolean equals(Object o) {
        // TODO Auto-generated method stub
        return macAddress.equals(((DeviceScanBean)o).getMacAddress());
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // TODO Auto-generated method stub
        dest.writeString(macAddress);
        dest.writeString(deviceName);
        dest.writeInt(rssi);
        dest.writeString(serialNumber);
    }

    public static final Creator<DeviceScanBean> CREATOR = new Creator<DeviceScanBean>() {

        @Override
        public DeviceScanBean[] newArray(int size) {
            // TODO Auto-generated method stub
            return new DeviceScanBean[size];
        }

        @Override
        public DeviceScanBean createFromParcel(Parcel source) {
            // TODO Auto-generated method stub
            return new DeviceScanBean(source);
        }
    };
}
