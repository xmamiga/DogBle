package com.amiga.dogble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.amiga.dogble.bean.CmdBean;
import com.amiga.dogble.util.BleUtils;
import com.amiga.dogble.util.ConnectInfo;
import com.amiga.dogble.util.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Date: 2018/10/12-20:27
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class BleDataTransition {
    private static final String TAG = "BleSdkDataTransition";

    BluetoothAdapter mBluetoothAdapter;
    BleManager mBleManager;
    List<ConnectInfo> mConnectInfoList;

    BlockingDeque<CmdBean> cmdQueue = new LinkedBlockingDeque<CmdBean>();

    Thread sendThread;
    BluetoothGatt mCurBluetoothGatt;
    BluetoothGattCharacteristic mCurBluetoothGattCharacteristic;
    boolean isSuccess = false;
    boolean isInterrupted = false;

    public BleDataTransition(BleManager bleManager){
        mBleManager = bleManager;
        startCmdThread();
    }

    protected  void init(BluetoothAdapter bluetoothAdapter,
                         List< ConnectInfo > connectInfoList){
        mBluetoothAdapter = bluetoothAdapter;
        mConnectInfoList = connectInfoList;
    }


    protected  synchronized void clearSend(){
        cmdQueue.clear();
    }

    protected synchronized void clearSend(String macAddr){
        for(CmdBean cmdBean : cmdQueue){
            if(cmdBean.getMacAddr().equals(macAddr)){
                cmdQueue.remove(cmdBean);
            }
        }
    }

    public boolean readData(String macAddr, BluetoothGattCharacteristic readCharacteristic){
        ConnectInfo connectInfo = BleManager.getConnectInfo(mConnectInfoList, macAddr);
        if(connectInfo == null){
            Log.e(TAG,"connectInfoList not cotain "+macAddr);
            return false;
        }

        if(connectInfo.getState()!= Constant.DISCOVERY_SERVICE_OK){
            Log.e(TAG,"connectInfoList connect state is not DISCOVERY_SERVICE_OK"+macAddr);
            return false;
        }

        return connectInfo.getmBluetoothGatt().readCharacteristic(readCharacteristic);
    }

    public boolean writeData(String macAddr, BluetoothGattCharacteristic writeCharacteristic,ArrayList<byte[]> data){
        ConnectInfo connectInfo = BleManager.getConnectInfo(mConnectInfoList, macAddr);
        if(connectInfo == null){
            Log.e(TAG,"connectInfoList not cotain "+macAddr);
            return false;
        }

        if(connectInfo.getState()!= Constant.DISCOVERY_SERVICE_OK){
            Log.e(TAG,"connectInfoList connect state is not DISCOVERY_SERVICE_OK"+macAddr);
            return false;
        }

        CmdBean cmdBean = new CmdBean(macAddr, connectInfo.getmBluetoothGatt(), writeCharacteristic, data);
        boolean result = false;
        try {
            result = cmdQueue.add(cmdBean);
//			Log.e(TAG, "add cmdbean ="+cmdBean.toString());
        } catch (Exception e) {
            // TODO: handle exception
        }
        return result;
    }


    class SendThread extends Thread{
        long id ;
        BluetoothGatt bluetoothGatt;
        String macAddr;
        BluetoothGattCharacteristic writeCharacteristic;
        ArrayList<byte[]> data;

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            for(int i = 0;i<data.size();i++){
                byte[] tempdata = data.get(i);
                if(writeCharacteristic !=null && bluetoothGatt !=null){
                    writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    writeCharacteristic.setValue(tempdata);

                    boolean state = bluetoothGatt.writeCharacteristic(writeCharacteristic);
                }
            }
        }

        public SendThread(BluetoothGatt bluetoothGatt,
                          String macAddr,
                          BluetoothGattCharacteristic writeCharacteristic,
                          ArrayList<byte[]> data,long id) {
            // TODO Auto-generated constructor stub
            this.bluetoothGatt = bluetoothGatt;
            this.macAddr = macAddr;
            this.writeCharacteristic = writeCharacteristic;
            this.data = data;
            this.id = id;
        }

        public void startThread(){
            this.start();
        }

        @Override
        public boolean equals(Object o) {
            // TODO Auto-generated method stub
            if(o == null)
                return false;
            if(o instanceof SendThread){
                if(this.id == ((SendThread)o).id);
                return true;
            }
            return false;
        }
    }

    private void startCmdThread(){
        sendThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                while(true){
                    try {
                        Log.e(TAG, "before take");
                        CmdBean cmdBean = cmdQueue.take();
                        ArrayList<byte[]> data = cmdBean.getData();
                        BluetoothGatt bluetoothGatt = cmdBean.getBluetoothGatt();
                        BluetoothGattCharacteristic writeCharacteristic = cmdBean.getWriteCharacteristic();
                        Log.e(TAG, "data: "+data.size());
                        for(int i = 0;i<data.size();i++){
                            byte[] tempdata = data.get(i);
                            if(writeCharacteristic !=null && bluetoothGatt !=null){

                                initBeforeOneSend(bluetoothGatt,writeCharacteristic);

                                writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                                writeCharacteristic.setValue(tempdata);
                                boolean state =bluetoothGatt.writeCharacteristic(writeCharacteristic);

                                if(state)
                                    Log.e(TAG, "write byte:"+ BleUtils.byteArrayToString(tempdata));
                                else
                                    Log.e(TAG, "write byte error");
                            }
                        }
                        Log.e(TAG, "after take");
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
        sendThread.start();
    }

    public boolean isWriteDataSame(byte[] data){
        byte[] older = mCurBluetoothGattCharacteristic.getValue();
        if(older == null || data == null){
            return false;
        }

        if(older.length != data.length){
            return false;
        }

        for(int i=0;i<data.length;i++){
            if(data[i] != older[i]){
                return false;
            }
        }

        return true;
    }

    public void interruptThread(String addr,boolean success){
        if(mCurBluetoothGatt == null){
            return ;
        }
        if(addr.equals(mCurBluetoothGatt.getDevice().getAddress())){
            isSuccess = success;
            sendThread.interrupt();
            isInterrupted = true;
        }
    }

    private void initBeforeOneSend(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){
        mCurBluetoothGatt = gatt;
        mCurBluetoothGattCharacteristic = characteristic;
        isSuccess = false;
        isInterrupted = false;
    }

    private void initafterOneSend(){
        mCurBluetoothGatt = null;
        mCurBluetoothGattCharacteristic = null;
        isSuccess = false;
        isInterrupted = false;
    }
}
