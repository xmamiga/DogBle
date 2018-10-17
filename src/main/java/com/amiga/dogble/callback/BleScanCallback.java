package com.amiga.dogble.callback;


import android.bluetooth.le.ScanResult;

/**
 * Date: 2018/10/12-19:22
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public interface BleScanCallback {
    void onScanCallback(int callbackType, ScanResult result);
}
