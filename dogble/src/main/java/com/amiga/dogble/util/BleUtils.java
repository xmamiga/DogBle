package com.amiga.dogble.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Date: 2018/10/12-19:33
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class BleUtils {
    public static boolean supportBle(Context context) {
        // Use this check to determine whether BLE is supported on the device.
        // Then you can
        // selectively disable BLE-related features.
        if (!context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }

        // Initializes a Bluetooth adapter. For API level 18 and above, get a
        // reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            return false;
        }

        return true;
    }

    public static boolean isBluetoothOpen(Context context) {
        final BluetoothManager bluetoothManager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.isEnabled();
        } else {
            return false;
        }
    }

    public static BluetoothAdapter getBleAdapter(Context context) {
        final BluetoothManager bluetoothManager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        return bluetoothAdapter;
    }

    public static String byteArrayToString(byte[] bytes) {
        String b = "";
        for (int i = 0; i < bytes.length; i++) {
            b += Integer.toHexString(bytes[i] & 0xff) + "  ";
        }
        return b;
    }

    public static String byteArrayToCharString(byte[] bytes) {
        String b = "";
        for (int i = 0; i < bytes.length; i++) {
            b += (char) bytes[i] + "  ";
        }
        return b;
    }
}
