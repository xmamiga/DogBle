package com.cchip.maddogbt.bean;

/**
 * Date: 2018/10/17-15:09
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class UpgradePacket {
    private int mCommandId = 0;
    private byte[] mPayload = null;

    public UpgradePacket(byte[] source, int source_length) {
        buildPacket(source, source_length);
    }

    //回复协议头	回复字段类型	类型长度	回复数据域	整帧单字节校验
    //  A0	      D0	      03	    01	       1byte
    private void buildPacket(byte[] source, int sourceLength) {
        mCommandId = source[1];

        int payloadLength = sourceLength - 4;
        mPayload = new byte[payloadLength];

        System.arraycopy(source, 3, mPayload, 0, payloadLength);
    }

    public int getCommandId() {
        return mCommandId;
    }

    public byte[] getPayload() {
        return mPayload;
    }
}
