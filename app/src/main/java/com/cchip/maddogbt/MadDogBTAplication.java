package com.cchip.maddogbt;

import android.app.Application;

/**
 * Date: 2018/10/16-16:19
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class MadDogBTAplication extends Application {
    private static MadDogBTAplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public static synchronized MadDogBTAplication getInstance() {
        return mInstance;
    }
}
