package com.amiga.dogble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.util.Log;

import com.amiga.dogble.callback.BleScanCallback;
import com.amiga.dogble.util.Constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Date: 2018/10/12-20:00
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class BleScanner {
    private static final String TAG = "BleSdkScan";


    BluetoothAdapter mBluetoothAdapter;
    int mScanState = Constant.SCAN_IDLE;
    BleScanCallback mScanCallback;
    BleManager mBleManager;

    ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (mScanCallback == null) {
                Log.e(TAG, "mScanCallback is null");
                return;
            }
            mScanCallback.onScanCallback(callbackType, result);
        }
    };


    public BleScanner(BleManager bleManager) {
        mBleManager = bleManager;
    }


    public void init(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = bluetoothAdapter;
        mScanState = Constant.SCAN_IDLE;
    }

    public int getScanState() {
        return mScanState;
    }

    public void setScanState(int state) {
        mScanState = state;

    }

    protected void setScanCallback(BleScanCallback scanCallback) {
        mScanCallback = scanCallback;
    }


    public synchronized int startScan(UUID[] serviceUUID) {

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "FAIL_ADAPTER");
            mScanState = Constant.SCAN_IDLE;
            return Constant.FAIL_ADAPTER;
        }

        if (mScanState == Constant.SCANNING) {
            Log.e(TAG, "ALREADY_SCAN_START");
            return Constant.ALREADY_SCAN_START;
        }

        Log.e(TAG, "scan uuid: " + Arrays.toString(serviceUUID));
        if (serviceUUID != null) {
             List<ScanFilter> bleScanFilters = new ArrayList<>();
            //添加过滤规则
            bleScanFilters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUUID[0])).build());
            //bleScanFilters.add(new ScanFilter.Builder().setDeviceName(DEVICE_NAME_STRING).build());
            ScanSettings bleScanSettings = new ScanSettings.Builder().build();
            mBluetoothAdapter.getBluetoothLeScanner().startScan(bleScanFilters, bleScanSettings, mLeScanCallback);
        } else {
            mBluetoothAdapter.getBluetoothLeScanner().startScan(mLeScanCallback);
        }
        mScanState = Constant.SCANNING;
        return Constant.SUCCESS;
    }


    public int stopScan() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "FAIL_ADAPTER");
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "mBluetoothAdapter == null");
            }
            return Constant.FAIL_ADAPTER;
        }

        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
        mScanState = Constant.SCAN_IDLE;
        return Constant.SUCCESS;
    }


    public String parseFromBytes(byte[] scanRecord) {

        String result = "";
        if (scanRecord == null) {
            return "";
        }

        int currentPos = 0;

        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                int length = scanRecord[currentPos++] & 0xFF;
                if (length == 0) {
                    break;
                }
                // Note the length includes the length of the field type itself.
                int dataLength = length - 1;
                // fieldType is unsigned int.
                int fieldType = scanRecord[currentPos++] & 0xFF;

                if (fieldType == (0xff)) {
                    int tempCurrentPos = currentPos;

                    while (tempCurrentPos + 3 < dataLength) {
                        if (scanRecord[tempCurrentPos] == 0x0F
                                && scanRecord[tempCurrentPos + 1] == (byte) 0xfe
                                && scanRecord[tempCurrentPos + 2] == 0x53
                                && scanRecord[tempCurrentPos + 3] == 0x4e) {
                            for (int i = 0; i < scanRecord[tempCurrentPos] - 1; i++) {
                                result += Integer.toHexString(scanRecord[tempCurrentPos + 2 + i] & 0xff);
                            }
                            Log.e(TAG, "sn:" + result);
                            break;
                        }
                        tempCurrentPos++;
                    }

                }
                currentPos += dataLength;
            }
            return result;

        } catch (Exception e) {
            Log.e(TAG, "unable to parse scan record: " + Arrays.toString(scanRecord));
            // As the record is invalid, ignore all the parsed results for this packet
            // and return an empty record with raw scanRecord bytes in results
            return "";
        }
    }
}
