package com.amiga.dogble;

import android.content.Intent;

import com.amiga.dogble.callback.ConnectStateCallback;
import com.amiga.dogble.util.Constant;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Date: 2018/10/13-12:13
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class AutoReConnect implements ConnectStateCallback {
    private BlePublicApi mBleApi;
    private Set<String> autoConnectSet = new HashSet<String>();


    private int reConnectCounts = 0;

    public AutoReConnect(BlePublicApi blePublicApi) {
        // TODO Auto-generated constructor stub
        mBleApi = blePublicApi;
    }

    @Override
    public void onConnectStateCallback(final String addr, int state) {
        // TODO Auto-generated method stub
        switch (state) {
            case Constant.CONNECTING:

                break;
            case Constant.CONNECTED:
                reConnectCounts = 0;
                break;

            case Constant.CONNECT_TIMEOUT:
                reConnectCounts++;
                if (reConnectCounts == Constant.MAX_RECONNECT_COUNTS) {
                    Intent intent = new Intent();
                    intent.setAction(Constant.ACTION_BLERECONNECT_MAX);
                    mBleApi.sendBroadcast(intent);
                    reConnectCounts = 0;
                }
                if (needAutoConnect(addr)) {
                    mBleApi.connect(addr);
                }
                break;

            case Constant.DISCONNECTING:
                break;

            case Constant.DISCONNECTED:
                reConnectCounts = 0;
                new Timer().schedule(new TimerTask() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if (needAutoConnect(addr)) {
                            mBleApi.connect(addr);
                        }
                    }
                }, 500);
                break;

            case Constant.DISCONNECT_TIMEOUT:
                if (needAutoConnect(addr)) {
                    mBleApi.connect(addr);
                }
                break;

            case Constant.DISCOVERY_SERVICE_ING:
                break;

            case Constant.DISCOVERY_SERVICE_OK:
                break;

            case Constant.DISCOVERY_SERVICE_FAIL:
                break;

            case Constant.CONNECT_ERROR_NEEDTO_CLOSE_BLE:
                break;

            case Constant.CONNECT_SCAN_NOT_FOUND:
                if (needAutoConnect(addr)) {
                    mBleApi.connect(addr);
                }
                break;
            default:
                break;
        }
    }

    boolean needAutoConnect(String addr) {
        return autoConnectSet.contains(addr);
    }

    void autoConnect() {
        if (autoConnectSet.size() > 0) {
            for (String macaddr : autoConnectSet) {
                mBleApi.connect(macaddr);
            }
        }
    }

    void addAutoConnectSet(String addr) {
        autoConnectSet.add(addr);
    }

    void removeAutoConnectSet(String addr) {
        autoConnectSet.remove(addr);
    }

    void clearAutoConnectSet() {
        autoConnectSet.clear();
    }
}
