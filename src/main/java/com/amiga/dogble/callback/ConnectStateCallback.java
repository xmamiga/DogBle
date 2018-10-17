package com.amiga.dogble.callback;

/**
 * Date: 2018/10/12-19:30
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public interface ConnectStateCallback {
    /**
     * @param @param addr  macAddress
     * @param @param state
     * @return void
     * @throws
     * @Title: onConnectStateCallback
     * @Description: TODO
     */
    void onConnectStateCallback(String addr, int state);
}
