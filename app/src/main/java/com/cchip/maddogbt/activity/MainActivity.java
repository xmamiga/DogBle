package com.cchip.maddogbt.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.amiga.dogble.util.BleUtils;
import com.cchip.maddogbt.R;
import com.cchip.maddogbt.adapter.BleDeviceAdapter;
import com.cchip.maddogbt.bean.DeviceScanBean;
import com.cchip.maddogbt.ble.BleApiConfig;
import com.cchip.maddogbt.presenter.DeviceConnectPresenter;
import com.cchip.maddogbt.utils.Constants;
import com.cchip.maddogbt.utils.ToastUI;

import java.util.ArrayList;

/**
 * Date: 2018/10/17-11:26
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Context mContext;
    private ListView lvDevices;
    private ProgressDialog mProgressDialog;
    public static BleApiConfig mBleService;
    ArrayList<DeviceScanBean> lists = new ArrayList<DeviceScanBean>();
    private DeviceConnectPresenter mDeviceConnectPresenter;
    private BleDeviceAdapter mAdapter;

    private static final int MSG_START_SCAN = 1001;
    private static final int MSG_STOP_SCAN = 1002;
    private static final int MSG_CLOSE_DIALOG = 1003;
    private static final int MSG_START_ACTIVITY = 1004;
    private static final int MSG_CONNECT_FAIL = 1005;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_SCAN:
                    startScan();
                    break;
                case MSG_STOP_SCAN:
                    dismissDialog();
                    if (mDeviceConnectPresenter != null) {
                        mDeviceConnectPresenter.stopScan();
                    }
                case MSG_CLOSE_DIALOG:
                    dismissDialog();
                    break;
                case MSG_START_ACTIVITY:
                    String addr = (String) msg.obj;
                    if (mBleService.getCharBlock(addr) != null
                            && mBleService.getCharIdentify(addr) != null) {
                        Intent intent = new Intent(MainActivity.this,
                                BKActivity.class);
                        intent.putExtra("mac", addr);
                        startActivity(intent);
                    } else {
                        ToastUI.showLong(R.string.no_server);
                        mBleService.disconnect(addr);
                    }
                    break;
                case MSG_CONNECT_FAIL:
                    ToastUI.showShort(R.string.ble_failed);
                    break;
                default:
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        if (!BleUtils.isBluetoothOpen(mContext)) {
            startActivityForResult((new Intent(this, OpenBleActivity.class)),
                    Constants.REQUESTCODE_OPEN_BLE);
        }
        initBleService();
        initUI();
    }

    private void initUI() {
        TextView tvTitle = findView(R.id.tv_title);
        tvTitle.setText(R.string.title_search);
        mAdapter = new BleDeviceAdapter(mContext);
        lvDevices = findView(R.id.lv_devices);
        lvDevices.setAdapter(mAdapter);
        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                showDialog();
                String mac = lists.get((int) id).getMacAddress();
                if (mac == null) {
                    logshow("mac is null");
                } else {
                    logshow("mac:" + mac);

                    mDeviceConnectPresenter.connectDevice(mac);
                }
            }
        });
        findView(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startScan();
            }
        });
    }

    public void startScan() {
        showDialog();
        if (mDeviceConnectPresenter != null) {
            mDeviceConnectPresenter.startScan();
        }
        mHandler.sendEmptyMessageDelayed(MSG_STOP_SCAN, 5000);
    }

    public void connectDeviceSuccess(final String addr) {
        logshow("connectDeviceSuccess");
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                dismissDialog();
                Message msg = mHandler.obtainMessage();
                msg.obj = addr;
                msg.what = MSG_START_ACTIVITY;
                mHandler.sendMessage(msg);
            }
        });
    }

    public void connectDeviceFail() {
        logshow("connectDeviceFail");
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                dismissDialog();
                mHandler.sendEmptyMessage(MSG_CONNECT_FAIL);
            }
        });
    }

    public void notifyScanAdapterDataChange(final ArrayList<DeviceScanBean> list) {

        runOnUiThread(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                lists = (ArrayList<DeviceScanBean>) list.clone();
                mAdapter.setDataChange(list);
            }
        });
    }

    private ServiceConnection onServicecon = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder rawBinder) {
            logshow("ServiceConnection success");
            mBleService = ((BleApiConfig.LocalBinder) rawBinder).getService();
            mDeviceConnectPresenter = new DeviceConnectPresenter(MainActivity.this, mBleService);
            startScan();
        }

        public void onServiceDisconnected(ComponentName classname) {
            mBleService = null;
            logshow("ServiceConnection fail");
        }
    };

    private void initBleService() {
        Intent bindIntent = new Intent(this, BleApiConfig.class);
        if (bindService(bindIntent, onServicecon, Context.BIND_AUTO_CREATE)) {
            logshow("bind service success");
        } else {
            logshow("bind service error");

        }
    }

    private void showDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(mContext, "",
                    getString(R.string.please_wait), true);
        }
        mHandler.sendEmptyMessageDelayed(MSG_CLOSE_DIALOG, 10000);
    }

    public void dismissDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUESTCODE_OPEN_BLE) {
            if (resultCode == Constants.RESULTCODE_OPEN_BLE) {
                showDialog();
                mBleService.openBle();
            } else if (resultCode == Constants.RESULTCODE_CLOSE_BLE) {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_START_SCAN);
        mHandler.removeMessages(MSG_STOP_SCAN);
        mHandler.removeMessages(MSG_CLOSE_DIALOG);
        unbindService(onServicecon);
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }

    private void logshow(String str) {
        Log.e(TAG, str);
    }

}
