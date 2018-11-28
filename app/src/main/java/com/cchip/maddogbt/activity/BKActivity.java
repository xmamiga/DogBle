package com.cchip.maddogbt.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amiga.dogble.util.Constant;
import com.cchip.maddogbt.R;
import com.cchip.maddogbt.bean.ImgHdr;
import com.cchip.maddogbt.bean.UpgradePacket;
import com.cchip.maddogbt.ble.ProtocolConfig;
import com.cchip.maddogbt.utils.BlockUtils;
import com.cchip.maddogbt.utils.Constants;
import com.cchip.maddogbt.utils.Conversion;
import com.cchip.maddogbt.utils.ToastUI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Date: 2018/10/17-14:55
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class BKActivity extends Activity implements View.OnClickListener {

    private static final String TAG = BKActivity.class.getSimpleName();

    private BluetoothGattCharacteristic mCharBlock;
    private final byte[] mOadBuffer = new byte[Constants.OAD_BUFFER_SIZE];
    private final byte[] mFileBuffer = new byte[Constants.FILE_BUFFER_SIZE];
    private ImgHdr mFileImgHdr = new ImgHdr();
    private ImgHdr mTargImgHdr = new ImgHdr();
    private ProgInfo mProgInfo = new ProgInfo();
    private boolean mProgramming;
    private boolean mgetTarget;
    private String mac;
    private TextView tvTarget, tvFileInfo, tvError, tvFile, tvStart, tvPencent,
            tvBlocks, tvSetting, tvRefresh, tvTime, tvBlockError;
    private Button btnBack;
    private LinearLayout layNoServer, layHasServer;
    private BluetoothGattCharacteristic mCharIdentify;
    private EditText etWaitMin, etWaitMax, etWaitTime;
    private Thread thread;
    private BTOtaReceiver mReceiver;
    private int settingCount = 0;
    private ProgressBar proBar;
    private long beginTime = 0;
    private boolean blocking = false;
    private static final long BLOCKINGMILL = 1500;
    private int againCount = 0;
    private int resendBlock = 0;
    private boolean isMiss = false;
    private byte[] data = new byte[]{};
    private boolean disConnect = false;

    private static final int MSG_SETTING_SUCCESS = 10001;
    private static final int MSG_SETTING_TIMEOUT = 10002;
    private static final int MSG_READ_TIMEOUT = 10003;
    private static final int MSG_START_FILE = 10004;
    private static final int MSG_UPDATE_SUCCESS = 10005;
    private static final int MSG_UPDATE_FAIL = 10006;
    private static final int MSG_UPDATE_MISS = 10009;
    private static final int MSG_UPDATE_ERROR = 10010;
    private static final int MSG_UPDATE_INVALID = 10011;
    private static final int MSG_UPDATE_FIRMWARE_INVALID = 10014;
    private static final int MSG_UPDATE_BTN = 10007;
    private static final int MSG_UPDATE_TV = 10008;
    private static final int MSG_DIS_CONNECT = 100012;
    private static final int MSG_BLOCK_TIMEOUT = 100013;
    private static final int MSG_BLOCK_ERROR = 100015;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (0 == msg.what) {//数据正在传输
                float arg1 = msg.arg1;
                float arg2 = msg.arg2;
                float percentage = arg2 / arg1 * 100;
                if (percentage < 0) {
                    percentage = 0;
                } else if (percentage > 100) {
                    percentage = 100;
                }
                proBar.setProgress((int) percentage);
                tvPencent.setText(percentage + "%");
                tvBlocks.setText("(" + arg2 * 16 + "/"
                        + arg1 * 16 + ")");
                long time = System.currentTimeMillis();
                Log.e("time", GetStringFromLong(time - beginTime - 28800000));
                tvTime.setText(GetStringFromLong(time - beginTime - 28800000));
                if (arg1 == arg2) {
                    setAllEnable(true);
                }
            }
            switch (msg.what) {
                case MSG_SETTING_SUCCESS:
                    ToastUI.showShort(R.string.success);
                    break;
                case MSG_SETTING_TIMEOUT:
                    logshow("MSG_SETTING_TIMEOUT");
                    break;
                case MSG_READ_TIMEOUT:
                    logshow("MSG_READ_TIMEOUT");
                    break;
                case MSG_START_FILE:
//                    Intent intent = new Intent(BKActivity.this,
//                            FileActivity.class);
//                    intent.putExtra(Constants.INTENT_TYPE, mTargImgHdr.imgType);
//                    logshow("mTargImgHdr.imgType: " + mTargImgHdr.imgType);
//                    startActivityForResult(intent, Constants.REQUESTCODE_FILE);
                    break;
                case MSG_UPDATE_SUCCESS:
                    logshow("MSG_UPDATE_SUCCESS");
                    if (thread != null) {
                        thread.interrupt();
                    }
                    removeCallbacks(runnable);
                    removeCallbacks(blockAgain);
                    byte[] buf = new byte[]{0x04, (byte) 0xa4, 0x01, 0};
                    MainActivity.mBleService.writeData(mac, mCharIdentify, buf);
                    ToastUI.showShort(R.string.success);
                    break;
                case MSG_UPDATE_FAIL:
                    logshow("MSG_UPDATE_FAIL");
                    refreshUI();
                    tvError.setText(R.string.fail);
                    break;
                case MSG_BLOCK_TIMEOUT:
                    logshow("MSG_BLOCK_TIMEOUT");
                    refreshUI();
                    tvError.setText(R.string.block_timeout);
                    break;
                case MSG_UPDATE_ERROR:
                    logshow("MSG_UPDATE_ERROR");
                    refreshUI();
                    tvError.setText(R.string.fail_error);
                    break;
                case MSG_UPDATE_MISS:
                    logshow("MSG_UPDATE_MISS");
                    refreshUI();
                    tvError.setText(R.string.fail_miss);
                    break;
                case MSG_UPDATE_INVALID:
                    logshow("MSG_UPDATE_INVALID");
                    refreshUI();
                    tvError.setText(R.string.fail_invalid);
                    break;
                case MSG_UPDATE_FIRMWARE_INVALID:
                    logshow("MSG_UPDATE_FIRMWARE_INVALID");
                    refreshUI();
                    tvError.setText(R.string.fail_firmware_invalid);
                    break;
                case MSG_UPDATE_BTN:
                    logshow("MSG_UPDATE_BTN");
                    setAllEnable(true);
                    break;
                case MSG_UPDATE_TV:
                    logshow("MSG_UPDATE_TV");
                    refreshUI();
                    tvError.setText(R.string.block_finish);
                    proBar.setProgress(100);
                    tvPencent.setText(100 + "%");
                    tvBlocks.setText("(" + mProgInfo.nBlocks * 16 + "/"
                            + mProgInfo.nBlocks * 16 + ")");
                    break;
                case MSG_DIS_CONNECT:
                    logshow("MSG_DIS_CONNECT");
                    if (thread != null) {
                        thread.interrupt();
                    }
                    removeCallbacks(runnable);
                    removeCallbacks(blockAgain);
                    mProgramming = false;
                    blocking = true;
                    etWaitTime.setEnabled(false);
                    tvSetting.setEnabled(false);
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setText(R.string.disconnected);
                    break;
                case MSG_BLOCK_ERROR:
                    logshow("MSG_BLOCK_ERROR");
                    if (data != null && data.length != 0) {
                        tvBlockError.setVisibility(View.VISIBLE);
                        tvBlockError.setText(Arrays.toString(data));
                    }
                    break;
                default:
                    break;
            }
        }

    };

    private boolean isLoss = true;
    private boolean isUpdating;
    private byte[] mBytesFile;
    private int mNumber = -1;
    private String mFilename;
    private Thread mThread;

    private void refreshUI() {
        if (thread != null) {
            thread.interrupt();
        }
        mHandler.removeCallbacks(runnable);
        mHandler.removeCallbacks(blockAgain);
        mProgramming = false;
        mgetTarget = false;
        blocking = true;
        setAllEnable(true);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setAllEnable(boolean b) {
        tvStart.setEnabled(b);
        tvFile.setEnabled(b);
        etWaitTime.setEnabled(b);
        tvSetting.setEnabled(b);
    }

    private void updateUI() {
        if (mProgramming
                || mFileImgHdr == null
                || mTargImgHdr == null
                || mTargImgHdr.imgType == null
                || mFileImgHdr.imgType == null
                || mFileImgHdr.ver == 0
                || mTargImgHdr.ver == 0
                || mFileImgHdr.imgType.toString().equals(
                mTargImgHdr.imgType.toString())) {
            tvSetting.setEnabled(true);
        } else {
            tvSetting.setEnabled(false);
            etWaitTime.setEnabled(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bk);
        initUI();
        mac = getIntent().getStringExtra("mac");
        logshow("mac:" + mac);
        registReceiver();
        tvStart = findView(R.id.tv_start);

        tvStart.setEnabled(mFilename == null ? false : true);
        tvStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startUpdate(mFilename);
                logshow("mProgInfo.iBlocks:" + mProgInfo.iBlocks);
            }
        });
        mCharBlock = MainActivity.mBleService.getCharBlock(mac);
        mCharIdentify = MainActivity.mBleService.getCharIdentify(mac);
        if (mCharBlock == null) {
            layNoServer.setVisibility(View.VISIBLE);
            layHasServer.setVisibility(View.GONE);
        } else {
            layNoServer.setVisibility(View.GONE);
            layHasServer.setVisibility(View.VISIBLE);
        }
        Log.e(TAG, "mCharBlock:" + mCharBlock);
        Log.e(TAG, "mCharIdentify:" + mCharIdentify);// c2
    }

    private void initUI() {
        TextView tvTitle = findView(R.id.tv_title);
        tvTitle.setText(R.string.title_block);
        tvError = findView(R.id.tv_error);
        tvError.setVisibility(View.GONE);
        tvTarget = findView(R.id.tv_target);
        tvFileInfo = findView(R.id.tv_file_info);
        btnBack = findView(R.id.btn_back);
        btnBack.setOnClickListener(this);
        tvFile = findView(R.id.tv_file);
        tvFile.setOnClickListener(this);
        etWaitMin = findView(R.id.et_wait_min);
        etWaitMax = findView(R.id.et_wait_max);
        etWaitTime = findView(R.id.et_wait_time);
        layNoServer = findView(R.id.lay_no_server);
        layHasServer = findView(R.id.lay_has_server);
        proBar = findView(R.id.pro_bar);
        proBar.setProgress(0);
        tvPencent = findView(R.id.tv_pencent);
        tvBlocks = findView(R.id.tv_blocks);
        tvSetting = findView(R.id.tv_setting);
        tvSetting.setOnClickListener(this);
        tvRefresh = findView(R.id.tv_refresh);
        tvRefresh.setOnClickListener(this);
        tvTime = findView(R.id.tv_time);
        tvBlockError = findView(R.id.tv_block_error);
        tvBlockError.setVisibility(View.GONE);
    }

    private void startBlock() {
        mProgramming = true;
        blocking = false;
        mProgInfo.reset();
        thread = new Thread(new OadTask());
        updatePro();
        thread.start();
        beginTime = System.currentTimeMillis();
        tvSetting.setEnabled(false);
        tvError.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void registReceiver() {
        mReceiver = new BTOtaReceiver();
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Constants.ACTION_BLOCK);
        iFilter.addAction(Constants.ACTION_BLOCK_STATUS);
        iFilter.addAction(Constants.ACTION_SETTING);
        iFilter.addAction(Constants.ACTION_VERSION);
        iFilter.addAction(Constants.ACTION_UPDATE);
        iFilter.addAction(Constant.ACTION_DEVICE_CONNECT_STATUS);
        registerReceiver(mReceiver, iFilter);
    }

    private void updatePro() {
        mHandler.postDelayed(runnable, 500);
    }

    private Runnable runnable = new Runnable() {

        @Override
        public void run() {
            updatePro();
            if (mProgramming) {
                int progress = (int) (mProgInfo.iBlocks * 100 / mProgInfo.nBlocks);
                proBar.setProgress(progress);
                tvPencent.setText(progress + "%");
                tvBlocks.setText("(" + mProgInfo.iBlocks * 16 + "/"
                        + mProgInfo.nBlocks * 16 + ")");
                long time = System.currentTimeMillis();
                Log.e(TAG,"time:" +  GetStringFromLong(time - beginTime - 28800000));
                tvTime.setText(GetStringFromLong(time - beginTime - 28800000));
            }
        }
    };

    private String GetStringFromLong(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        Date dt = new Date((time));
        return sdf.format(dt);
    }

    private class OadTask implements Runnable {

        @Override
        public void run() {
            int count = 0;
            while (mProgramming) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!blocking || Constants.MISS_RESEND) {
                    count = 0;
                    for (int i = 0; i < 1 & mProgramming; i++) {
                        startPro();
                    }
                } else {
                    count++;
                    if (count > 1000) {
                        logshow("timeout");
                        mHandler.sendEmptyMessage(MSG_BLOCK_TIMEOUT);
                        mProgramming = false;
                        blocking = true;
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private void startPro() {
        if (mProgInfo.iBlocks < mProgInfo.nBlocks) {
            if (Constants.MISS_RESEND && isMiss && resendBlock >= 0) {
                mProgInfo.iBytes = mProgInfo.iBytes
                        - (mProgInfo.iBlocks - resendBlock) * 16;
                mProgInfo.iBlocks = resendBlock;
                isMiss = false;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logshow("mProgInfo.iBlocks:" + mProgInfo.iBlocks);
            logshow("mProgInfo.nBlocks:" + mProgInfo.nBlocks);
            logshow("mProgInfo.iBytes:" + mProgInfo.iBytes);
            mOadBuffer[0] = Conversion.loUint16(mProgInfo.iBlocks);
            mOadBuffer[1] = Conversion.hiUint16(mProgInfo.iBlocks);
            System.arraycopy(mFileBuffer, (int) mProgInfo.iBytes, mOadBuffer,
                    2, Constants.OAD_BLOCK_SIZE);
            int crc = BlockUtils.calc_crc16(mOadBuffer, 18);
            mOadBuffer[18] = Conversion.loUint16(crc);
            mOadBuffer[19] = Conversion.hiUint16(crc);
            mCharBlock.setValue(mOadBuffer);
            boolean success = false;
            for (int i = 0; i < 3; i++) {
                success = MainActivity.mBleService.writeData(mac, mCharBlock,
                        mOadBuffer);
                Log.e(TAG, "success:" + success);
                if (success) {
                    break;
                } else {
                    Log.e(TAG, "error: " + i);
                }
                int delaytime = 0;
                switch (i) {
                    case 0:
                        delaytime = 100;
                        break;
                    case 1:
                        delaytime = 500;
                        break;
                    case 2:
                        delaytime = 1000;
                        break;
                }
                try {
                    Thread.sleep(delaytime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (i == 2)
                    success = MainActivity.mBleService.writeData(mac,
                            mCharBlock, mOadBuffer);
            }

            if (success) {
                if (Constants.MISS_RESEND) {
                    if (!BlockUtils.waitIdle2(20)) {
                        success = false;
                        mProgInfo.iBlocks++;
                        mProgInfo.iBytes += Constants.OAD_BLOCK_SIZE;
                    }
                } else {
                    blocking = true;
                    mHandler.postDelayed(blockAgain, BLOCKINGMILL);
                }
            } else {
                if (mProgramming) {
                    mHandler.sendEmptyMessage(MSG_UPDATE_FAIL);
                }
            }
        } else {
            mProgramming = false;
            mHandler.sendEmptyMessage(MSG_UPDATE_TV);
        }
    }

    Runnable blockAgain = new Runnable() {

        @Override
        public void run() {
            logshow("blockAgain");
            if (againCount < 2) {
                MainActivity.mBleService.writeData(mac, mCharBlock, mOadBuffer);
            } else {
                againCount = 0;
                mHandler.sendEmptyMessage(MSG_BLOCK_TIMEOUT);
            }
        }
    };

    private void loadFile(String filepath, boolean isAsset) {
        try {
            InputStream stream;
            if (isAsset) {
                stream = getAssets().open(filepath);
            } else {
                File f = new File(filepath);
                stream = new FileInputStream(f);
            }
            stream.read(mFileBuffer, 0, mFileBuffer.length);
            stream.close();
        } catch (IOException e) {
            logshow("loadFile:IOException");
        }

        mFileImgHdr.ver = Conversion
                .buildUint16(mFileBuffer[5], mFileBuffer[4]);
        mFileImgHdr.len = Conversion
                .buildUint16(mFileBuffer[7], mFileBuffer[6]);
        mFileImgHdr.imgType = ((mFileImgHdr.ver & 1) == 1) ? 'B' : 'A';
        System.arraycopy(mFileBuffer, 8, mFileImgHdr.uid, 0, 4);
        BlockUtils.displayImageInfo(tvFileInfo, mFileImgHdr);
        Log.e("jiang", "mFileBuffer:" + mFileBuffer.length);
        updateUI();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                finish();
                break;
            case R.id.tv_file:
                tvError.setVisibility(View.GONE);
                mHandler.sendEmptyMessage(MSG_START_FILE);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgramming = false;
        if (thread != null) {
            thread.interrupt();
        }
        if (mThread != null) {
            mThread.interrupt();
        }
        if (runnable != null) {

            mHandler.removeCallbacks(runnable);
        }
        if (blockAgain != null) {

            mHandler.removeCallbacks(blockAgain);
        }
        if (mReceiver != null) {

            unregisterReceiver(mReceiver);
        }
        if (!disConnect) {
            MainActivity.mBleService.disconnect(mac);
        }
    }

    class BTOtaReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e("cxj", "onReceive: ");
            if (action.equals(Constants.ACTION_SETTING)) {
                byte[] data = intent.getByteArrayExtra(Constants.INTENT_SETTING);
                UpgradePacket packet = new UpgradePacket(data, data.length);
                handleBleUpgradePacket(packet);
            }

            if (action.equals(Constants.ACTION_BLOCK)) {
                int state = intent.getIntExtra(Constants.INTENT_BLOCK, 0);
                logshow("state:" + state);
                if (state == Constants.BLOCK_ERROR) {
                    mHandler.sendEmptyMessage(MSG_UPDATE_ERROR);
                } else if (state == Constants.BLOCK_MISS) {
                    mHandler.sendEmptyMessage(MSG_UPDATE_MISS);
                } else if (state == Constants.BLOCK_INVALID) {
                    mHandler.sendEmptyMessage(MSG_UPDATE_INVALID);
                } else if (state == Constants.BLOCK_FIREWARE_INVALID) {
                    mHandler.sendEmptyMessage(MSG_UPDATE_FIRMWARE_INVALID);
                } else if (state == Constants.BLOCK_SUCCESS) {
                    mHandler.sendEmptyMessage(MSG_UPDATE_SUCCESS);
                }
            } else if (action.equals(Constants.ACTION_VERSION)) {
                byte[] value = intent
                        .getByteArrayExtra(Constants.INTENT_VERSION);
                logshow("value:" + Arrays.toString(value));
                if (value.length < 3) {
                    return;
                }
                mTargImgHdr.ver = Conversion.buildUint16(value[1], value[0]);
                mTargImgHdr.imgType = ((mTargImgHdr.ver & 1) == 1) ? 'B' : 'A';
                mTargImgHdr.len = Conversion.buildUint16(value[3], value[2]);
                if (mTargImgHdr != null && mTargImgHdr.imgType != null) {
                    tvRefresh.setVisibility(View.GONE);
                }
                tvFile.setEnabled(true);
                BlockUtils.displayImageInfo(tvTarget, mTargImgHdr);
                updateUI();
            } else if (action.equals(Constant.ACTION_DEVICE_CONNECT_STATUS)) {
                String address = intent
                        .getStringExtra(Constant.EXTRA_DEVICE_CONNECT_STATUS_ADDRESS);
                int state = intent.getIntExtra(
                        Constant.EXTRA_DEVICE_CONNECT_STATUS, 0);
                if (address.equals(mac)) {
                    if (state == Constant.CONNECTED) {
                        tvError.setVisibility(View.GONE);
                        disConnect = false;
                    } else if (state == Constant.DISCONNECTED) {
                        tvError.setVisibility(View.VISIBLE);
                        tvError.setText(R.string.disconnected);
                        mHandler.sendEmptyMessage(MSG_DIS_CONNECT);
                        disConnect = true;
                    }
                }
            } else if (action.equals(Constants.ACTION_UPDATE)) {
                mHandler.sendEmptyMessage(MSG_UPDATE_SUCCESS);
            } else if (action.equals(Constants.ACTION_BLOCK_STATUS)) {
                if (!mProgramming) {
                    return;
                }
                byte[] data = intent
                        .getByteArrayExtra(Constants.INTENT_BLOCK_STATUS);
                logshow("ACTION_BLOCK_STATUS>>>data: " + (data[0] & 0x00ff)
                        + "  " + (data[1] & 0x00ff));
                if (data.length != 2) {
                    return;
                }
                if (Constants.MISS_RESEND) {
                    isMiss = true;
                    resendBlock = (data[0] & 0x00ff)
                            + ((data[1] & 0x00ff) * 256);
                    Log.e(TAG, "resendBlock: " + resendBlock);
                    if (resendBlock < 0 || resendBlock > mProgInfo.nBlocks) {
                        BKActivity.this.data = data;
                        mHandler.sendEmptyMessage(MSG_BLOCK_ERROR);
                    }
                } else {
                    if (mOadBuffer[0] == data[0] && mOadBuffer[1] == data[1]) {
                        mProgInfo.iBlocks++;
                        againCount = 0;
                        mProgInfo.iBytes += Constants.OAD_BLOCK_SIZE;
                        blocking = false;
                        mHandler.removeCallbacks(blockAgain);
                    } else {
                        mHandler.removeCallbacks(blockAgain);
                        againCount++;
                        mHandler.post(blockAgain);
                    }
                }

            }
        }
    }

    public void onUpdateActivated() {

        isLoss = true;
        mNumber = -1;

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mBytesFile == null) {
                        return;
                    }
                    int tmpPackets = 0;
                    int packets = tmpPackets = mBytesFile.length / ProtocolConfig.DATA_LENGTH;
                    int remain = mBytesFile.length % ProtocolConfig.DATA_LENGTH;

                    if (remain != 0) {
                        tmpPackets += 1;
                    }


                    byte[] datas = new byte[ProtocolConfig.DATA_LENGTH];
                    long i = 0;
                    while (isLoss) {
                        if (mNumber != -1) {
                            i = mNumber;
                            mNumber = -1;
                        }
                        Log.e(TAG, "run: " + i);
                        if (i < packets) {
                            if (isUpdating) {
                                System.arraycopy(mBytesFile, (int) (i * ProtocolConfig.DATA_LENGTH), datas, 0, ProtocolConfig.DATA_LENGTH);
                                MainActivity.mBleService.mProtocol.dataTransfer(mac, (int) i, datas);
                                Message msg = mHandler.obtainMessage();
                                msg.what = 0;
                                msg.arg1 = tmpPackets;
                                msg.arg2 = (int) (i + 1);
                                msg.sendToTarget();
                                Thread.sleep(20);
                            }
                        } else if (i == packets) {
                            if (remain != 0 && isUpdating) {
                                datas = new byte[ProtocolConfig.DATA_LENGTH];
                                System.arraycopy(mBytesFile, packets * ProtocolConfig.DATA_LENGTH, datas, 0, remain);
                                MainActivity.mBleService.mProtocol.dataTransfer(mac, packets, datas);

                            }
                        }
                        if (i > packets * 5) {
                            isLoss = false;
                        }
                        i++;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//            }
        }, "UpdateOnBLEVMFragment.FilesActivity");
        mThread.start();
    }

    private void handleBleUpgradePacket(UpgradePacket packet) {
        boolean validate;
        Log.e(TAG, "handleBleUpgradePacket: " + packet.getCommandId());
        switch (packet.getCommandId()) {
            case ProtocolConfig.STARTUP_REPLY:
                break;
            case ProtocolConfig.DEVICE_PREPARE_COMPLETE:
                setAllEnable(false);
                if (!isLoss) {
                    onUpdateActivated();
                }
                break;
            case ProtocolConfig.PACKET_STATUS:
                byte[] payload = packet.getPayload();//2byte包号	1byte状态
                byte[] nos = new byte[2];
                nos[0] = payload[0];
                nos[1] = payload[1];
                int number = ProtocolConfig.bytesToShort(nos);
                byte status = payload[2];
                Log.e(TAG, "number: " + number);
                if (status == 0x00) {
                    isLoss = false;
                    int packets = mBytesFile.length / ProtocolConfig.DATA_LENGTH;
                    int remain = mBytesFile.length % ProtocolConfig.DATA_LENGTH;

                    if (remain != 0) {
                        packets += 1;
                    }
                    Message msg = mHandler.obtainMessage();
                    msg.what = 0;
                    msg.arg1 = packets;
                    msg.arg2 = packets;
                    msg.sendToTarget();
                } else if (status == 0x02) {
                    mNumber = number;
                }
                break;
            case ProtocolConfig.UPGRADE_RESULT:
                payload = packet.getPayload();
                if (payload[0] == 0x01) {//00失败 01成功

                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUESTCODE_FILE
                && resultCode == Constants.RESULTCODE_fILE) {
//            mFilename = data.getStringExtra(FileActivity.EXTRA_FILENAME);
            tvStart.setEnabled(mFilename == null ? false : true);
            loadFile(mFilename, false);


        }
    }

    private void startUpdate(String filename) {
        beginTime = System.currentTimeMillis();
        isLoss = false;
        isUpdating = true;

//        if (!TextUtils.isEmpty(filename)) {
//            File f = new File(filename);
//            mBytesFile = Utils.getBytesFromFile(f);
//            int packets = mBytesFile.length / 16;
//            int remain = mBytesFile.length % 16;
//            if (remain != 0) {
//                packets += 1;
//            }
//
//            MainActivity.mBleService.mProtocol.sendStartUp(mac, (short) packets);
//        }
        Log.e(TAG, "startUpdate: " + mac);
        Log.e(TAG, "startUpdate: " + mBytesFile);
    }

    public class ProgInfo {

        long iBytes = 0; // Number of bytes programmed
        long iBlocks = 0; // Number of blocks programmed
        long nBlocks = 0; // Total number of blocks

        void reset() {
            iBytes = 0;
            iBlocks = 0;
            nBlocks = (short) (mFileImgHdr.len / (Constants.OAD_BLOCK_SIZE / Constants.HAL_FLASH_WORD_SIZE));
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }

    private void logshow(String str) {
        Log.e(TAG, str);
    }

}
