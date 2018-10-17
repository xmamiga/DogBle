package com.amiga.dogble.util;

/**
 * Date: 2018/10/12-19:34
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class Constant {
    public static final int MAX_CONNECT_SIZE = 8;

    public static final int MAX_RECONNECT_COUNTS = 5;
    public static final int DEFAULT_MTU = 23;

    //scan state
    public static final int SCAN_IDLE = 0;
    public static final int SCANNING = 1;

    //connect state
    public static final int CONNECT_IDLE = 0;
    public static final int CONNECTING = 1;
    public static final int CONNECTED = 2;
    public static final int CONNECT_TIMEOUT = 3;
    public static final int DISCONNECTING = 4;
    public static final int DISCONNECTED = 5;
    public static final int DISCONNECT_TIMEOUT = 6;
    public static final int DISCOVERY_SERVICE_ING = 7;
    public static final int DISCOVERY_SERVICE_OK = 8;
    public static final int DISCOVERY_SERVICE_FAIL = 9;
    public static final int COMMUNICATE_SUCCESS = 10;
    public static final int COMMUNICATE_FAIL = 11;
    public static final int CONNECT_ERROR_NEEDTO_CLOSE_BLE = 12;
    public static final int CONNECT_SCAN_NOT_FOUND = 13;
    public static final int OTHER = 14;

    public static final int SUCCESS = 0;
    public static final int FAIL = 1;
    public static final int FAIL_ADAPTER = 2;
    public static final int FAIL_PARAMETER = 3; // PARAMETER error
    public static final int ALREADY_SCAN_START = 4;
    public static final int ALREADY_SCAN_STOP = 5;
    public static final int ALREDY_CONNECT = 6;
    public static final int ALREADY_DISCONNECT = 7;
    public static final int MAX_CONNECT = 8;


    public static final int BLUETHOOTH_STATE_ON = 0;
    public static final int BLUETHOOTH_STATE_OFF = 1;


    public static final int ADD_LIST_SUCCESS = 0;
    public static final int ADD_LIST_FAIL_MAX_SIZE = 1;
    public static final int ADD_LIST_FAIL_EXIST_CONNECTINOF = 2;

    public static final String ACTION_BLERECONNECT_MAX = "ACTION_BLERECONNECT_MAX";
    public static final String ACTION_BLUETHOOTH_STATE_CHANGE = "ACTION_BLUETHOOTH_STATE_CHANGE";
    public static final String ACTION_DEVICE_CONNECT_STATUS = "ACTION_DEVICE_CONNECT_STATUS";

    public static final String EXTRA_BLUETHOOTH_STATE_CHANGE = "EXTRA_BLUETHOOTH_STATE_CHANGE";
    public static final String EXTRA_DEVICE_CONNECT_STATUS = "EXTRA_DEVICE_CONNECT_STATUS";
    public static final String EXTRA_DEVICE_CONNECT_STATUS_ADDRESS = "EXTRA_DEVICE_CONNECT_STATUS_ADDRESS";


}
