package com.cchip.maddogbt.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.util.Log;

import com.cchip.maddogbt.utils.BlockUtils;
import com.cchip.maddogbt.utils.Constants;
import com.cchip.maddogbt.utils.Conversion;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Date: 2018/10/16-16:04
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class ProtocolConfig {
    private static final String TAG = "Protocol";
    public static final UUID otaService_UUID = UUID
            .fromString("0000F668-0000-1000-8000-00805f9b34fb");
    public static final UUID otaBlockRequest_UUID = UUID
            .fromString("0000F698-0000-1000-8000-00805f9b34fb");// second
    public static final UUID otaImageNotify_UUID = UUID
            .fromString("0000F699-0000-1000-8000-00805f9b34fb");// first
//    public static final UUID oadService_UUID = UUID
//            .fromString("0000F668-d102-11e1-9b23-00025b00a5a5");
//    public static final UUID otaBlockRequest_UUID = UUID
//            .fromString("0000F698-d102-11e1-9b23-00025b00a5a5");// second
//    public static final UUID otaImageNotify_UUID = UUID
//            .fromString("0000F699-d102-11e1-9b23-00025b00a5a5");// first

    public static final byte HEAD_CONFIG_SET = (byte) 0xA5;
    public static final byte WRITE_FIELD_SSID = (byte) 0x01;
    public static final byte WRITE_FIELD_PASSWORD = (byte) 0x02;
    public static final byte READ_FIELD_SSID = (byte) 0x11;
    public static final byte READ_FIELD_PASSWORD = (byte) 0x12;

    public static final int CMD_SEND_SUCCESS = 0;
    public static final int CMD_SEND_FAIL = 1;
    public static final int CMD_SEND_PAREMETER_ERROR = 2;
    /**
     * 状态特征 协议头
     */
    public static final byte PROTOCOL_HEADER = (byte) 0xa0;

    /**
     * 启动指令
     */
    public static final byte STARTUP = (byte) 0xb6;
    public static final byte STARTUP_REPLY = (byte) 0xD0;
    public static final byte DEVICE_PREPARE_COMPLETE = (byte) 0xD1;
    public static final byte PACKET_STATUS = (byte) 0xD2;

    public static final byte UPGRADE_RESULT = (byte) 0xD7;//设备回复升级结果:00失败 01成功

    public static final int DATA_LENGTH = 16;
    /**
     * 连接间隔
     */
    public static final byte CONNECTION_PREPARE_COMPLETE = (byte) 0xb7;

    BleApiConfig mBleApi;

    public ProtocolConfig(BleApiConfig bleApi) {
        mBleApi = bleApi;
    }

    private class DelaySendCmdAfterConnected extends TimerTask {

        String addr;

        public DelaySendCmdAfterConnected(String macAddr) {
            // TODO Auto-generated constructor stub
            this.addr = macAddr;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            boolean notify = mBleApi.setCharateristicNotification(addr,
                    ProtocolConfig.otaService_UUID,
                    ProtocolConfig.otaBlockRequest_UUID);
            Log.e(TAG, "notify" + notify);
        }
    }

    void sendCmdAfterConnected(String addr) {
        new Timer().schedule(new DelaySendCmdAfterConnected(addr), 500);
    }

    void sendBroadcast(Intent intent) {
        mBleApi.sendBroadcast(intent);
    }

    public void prasedata(String macAddr,
                          BluetoothGattCharacteristic characteristic) {
        Log.e(TAG, "prasedata:" + characteristic.getUuid() + "; value:"
                + byteArrayToString(characteristic.getValue()));
        byte[] data = characteristic.getValue();

        BluetoothGattCharacteristic mCharIdentify = mBleApi
                .getCharIdentify(macAddr);
        BluetoothGattCharacteristic mCharBlock = mBleApi.getCharBlock(macAddr);
        if (mCharIdentify == null || mCharBlock == null) {
            return;
        }
        Intent intent = new Intent();
        if (mCharIdentify.getUuid().toString()
                .equals(characteristic.getUuid().toString())) {
            if (data[0] == PROTOCOL_HEADER) {
                intent.setAction(Constants.ACTION_SETTING);
                intent.putExtra(Constants.INTENT_SETTING, data);
            }
        } else if (mCharBlock.getUuid().toString()
                .equals(characteristic.getUuid().toString())) {
            if (data.length == 2) {
                intent.setAction(Constants.ACTION_BLOCK_STATUS);
                intent.putExtra(Constants.INTENT_BLOCK_STATUS, data);
            } else if (data[0] == 4 && data[1] == (byte) 0xa2 && data[2] == 1) {
                if (data[3] == 0) {
                    intent.setAction(Constants.ACTION_VERSION);
                    intent.putExtra(Constants.INTENT_VERSION, true);
                } else {
                    intent.setAction(Constants.ACTION_BLOCK);
                    intent.putExtra(Constants.INTENT_BLOCK, false);
                }
            }
        }
        sendBroadcast(intent);
    }


    private String byteArrayToString(byte[] bytes) {
        String b = "";
        for (int i = 0; i < bytes.length; i++) {
            b += Integer.toHexString(bytes[i] & 0xff) + "  ";
        }
        return b;
    }

    private void upgradeWriteData(String macAddr, ArrayList<byte[]> cmd) {
        BluetoothGattCharacteristic btGattCharacteristic = getUpgradeOnBLEDataTransferGatt(macAddr);
        if (btGattCharacteristic == null) {
            return;
        }
        mBleApi.writeData(macAddr, btGattCharacteristic, cmd);
    }

    private void upgradeWriteDataStatus(String macAddr, ArrayList<byte[]> cmd) {
        BluetoothGattCharacteristic btGattCharacteristic = getUpgradeOnBLEStatusGatt(macAddr);
        if (btGattCharacteristic == null) {
            return;
        }
        mBleApi.writeData(macAddr, btGattCharacteristic, cmd);
    }

    public void sendStartUp(String mac, short packets) {
        byte[] datas = startUp(packets);
        ArrayList<byte[]> cmd = new ArrayList<byte[]>();
        cmd.add(datas);
        upgradeWriteDataStatus(mac, cmd);
    }

    public void getConnectionInterval(String mac) {
        byte[] bytes = {PROTOCOL_HEADER, CONNECTION_PREPARE_COMPLETE, (byte) 0x03, (byte) 0x01, 0};
        sum(bytes);
        ArrayList<byte[]> cmd = new ArrayList<byte[]>();
        cmd.add(bytes);
        upgradeWriteDataStatus(mac, cmd);
    }

    public void setStarConnectionInterval(String mac, short time) {
        byte[] bytes1 = shortToBytes(time);
        byte[] bytes = {PROTOCOL_HEADER, (byte) 0xb8, (byte) 0x04, bytes1[0], bytes1[1], 0};
        sum(bytes);
        ArrayList<byte[]> cmd = new ArrayList<byte[]>();
        cmd.add(bytes);
        upgradeWriteDataStatus(mac, cmd);
    }

    private void sum(byte[] bytes) {
        byte sum = 0;
        for (int index = 0; index < bytes.length - 1; index++) {
            sum += bytes[index];
        }
        bytes[bytes.length - 1] = sum;
    }

    public void dataTransfer(String mac, int packetNum, byte[] datas) {
        byte[] bytes = dataBean((short) packetNum, datas);
        ArrayList<byte[]> cmd = new ArrayList<byte[]>();
        cmd.add(bytes);
        upgradeWriteData(mac, cmd);
    }

    public boolean supportNewBleUpgrade(String macAddr) {
        BluetoothGattCharacteristic upgradeOnBLEStatusGatt = getUpgradeOnBLEStatusGatt(macAddr);
        BluetoothGattCharacteristic upgradeOnBLEDataTransferGatt = getUpgradeOnBLEDataTransferGatt(macAddr);
        return upgradeOnBLEStatusGatt != null && upgradeOnBLEDataTransferGatt != null;
    }

    private BluetoothGattCharacteristic getUpgradeOnBLEStatusGatt(String macAddr) {
        BluetoothGattCharacteristic writeCharacteristic = mBleApi.getWriteCharateristic(macAddr,
                otaService_UUID, otaImageNotify_UUID);

        return writeCharacteristic;
    }

    private BluetoothGattCharacteristic getUpgradeOnBLEDataTransferGatt(String macAddr) {
        BluetoothGattCharacteristic writeCharacteristic = mBleApi.getWriteCharateristic(macAddr,
                otaService_UUID, otaBlockRequest_UUID);

        return writeCharacteristic;
    }

    public static byte[] startUp(short packets) {
        byte[] data = new byte[6];
        data[0] = PROTOCOL_HEADER;
        data[1] = STARTUP;
        data[2] = 4;
        byte[] packets_bytes = shortToBytes(packets);

        data[3] = packets_bytes[0];
        data[4] = packets_bytes[1];

        byte sum = 0;
        for (int index = 0; index < data.length - 1; index++) {
            sum += data[index];
        }
        data[5] = sum;

        return data;
    }

    public static byte[] dataBean(short packetNum, byte[] desData) {
        byte[] data = new byte[2 + desData.length + 2];
        data[1] = Conversion.loUint16(packetNum);
        data[0] = Conversion.hiUint16(packetNum);
        System.arraycopy(desData, 0, data, 2, desData.length);
        int crc = BlockUtils.calc_crc16(data, data.length - 2);

        data[data.length - 1] = Conversion.loUint16(crc);
        data[data.length - 2] = Conversion.hiUint16(crc);

        return data;
    }

    //    onCharacteristic
    public static byte[] shortToBytes(short n) {
        byte[] b = new byte[2];
        b[1] = (byte) (n & 0xff);
        b[0] = (byte) ((n >> 8) & 0xff);
        return b;
    }

    /**
     * 2个字节 转换为 short
     *
     * @param b
     * @return
     */
    public static short bytesToShort(byte[] b) {
        short s = 0;
        short s0 = (short) (b[1] & 0xff);
        short s1 = (short) (b[0] & 0xff);
        s1 <<= 8;
        s = (short) (s0 | s1);
        return s;
    }
}
