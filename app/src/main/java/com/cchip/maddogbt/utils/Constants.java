package com.cchip.maddogbt.utils;

import com.cchip.maddogbt.MadDogBTAplication;

/**
 * Date: 2018/10/16-16:19
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class Constants {
    private final static String PACKAGE = MadDogBTAplication.getInstance()
            .getPackageName();

    public static final boolean MISS_RESEND = true;

    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_EAP = 3;
    public static final int SECURITY_WAPI_PSK = 4;
    public static final int SECURITY_WAPI_CERT = 5;

    public static final String INTENT_SSID = PACKAGE + ".intent.ssid";
    public static final String INTENT_PWD = PACKAGE + ".intent.pwd";
    public static final String INTENT_SETTING = PACKAGE + ".intent.setting";
    public static final String INTENT_BLOCK = PACKAGE + ".intent.block";
    public static final String INTENT_BLOCK_STATUS = PACKAGE + ".intent.block.status";
    public static final String INTENT_VERSION = PACKAGE + ".intent.version";
    public static final String INTENT_UPDATE = PACKAGE + ".intent.update";
    public static final String INTENT_TYPE = PACKAGE + ".intent.type";

    public static final int RESULTCODE_fILE = 15;
    public static final int REQUESTCODE_FILE = 16;
    public static final int RESULTCODE_OPEN_BLE = 17;
    public static final int RESULTCODE_CLOSE_BLE = 18;
    public static final int REQUESTCODE_OPEN_BLE = 19;

    public static final String ACTION_SETTING = PACKAGE + ".action.setting";
    public static final String ACTION_BLOCK = PACKAGE + ".action.block";
    public static final String ACTION_BLOCK_STATUS = PACKAGE + ".action.block.status";
    public static final String ACTION_VERSION = PACKAGE + ".action.version";
    public static final String ACTION_UPDATE = PACKAGE + ".action.update";

    public static final int BLOCK_SUCCESS = 0;
    public static final int BLOCK_ERROR = 1;
    public static final int BLOCK_MISS = 2;
    public static final int BLOCK_INVALID = 3;
    public static final int BLOCK_FIREWARE_INVALID = 4;

    public static final int OAD_BLOCK_SIZE = 16;
    public static final int FILE_BUFFER_SIZE = 0x40000;
    public static final int OAD_BUFFER_SIZE = 4 + OAD_BLOCK_SIZE;
    public static final int HAL_FLASH_WORD_SIZE = 4;
    public static final int OAD_IMG_HDR_SIZE = 8;
    public static final short OAD_CONN_INTERVAL = 12;
    public static final short OAD_SUPERVISION_TIMEOUT = 50;


    public static final int BATTERY_LEVEL_MAX = 4200;
    public static final String DATE_FORMAT = "dd/MM/yyyy kk:mm";
    public static final boolean DEBUG = true;
    public static final String IS_EQ_SHOW = "is_eq_show";
    public static final String IS_PRESETS_STATE = "is_presets_state";
    public static final String PERCENTAGE_CHARACTER = "%";
    public static final String SP_NAME = "QICHANG";
    public static final String UNIT_FILE_SIZE = " KB";
    public static final int UPDATE_TABS_NUMBER = 1;
    public static final String VM_UPDATE_FOLDER = "/VMUPGRADE";

    public static final String SUPPORT_APP = "http://www.sakar.com/altec/speakera";
    public static final String SUPPORT_DEVICE = "http://www.sakar.com/altec/speakera";

    public static final String SP_DEVICENAME = "sp_deviecname";

    //
    public static final int CONNECT_TYPE_BLE = 0;
    public static final int CONNECT_TYPE_SPP = 1;
    public static int CONNECT_TYPE = -1;
}
