package com.cchip.maddogbt.utils;

import android.content.Context;
import android.widget.Toast;

import com.cchip.maddogbt.MadDogBTAplication;

/**
 * Date: 2018/10/17-14:07
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class ToastUI {
    public static void showShort(int resId) {
        Context mContext = MadDogBTAplication.getInstance()
                .getApplicationContext();
        Toast.makeText(mContext, mContext.getResources().getString(resId),
                Toast.LENGTH_SHORT).show();
    }

    public static void showLong(int resId) {
        Context mContext = MadDogBTAplication.getInstance()
                .getApplicationContext();
        Toast.makeText(mContext, mContext.getResources().getString(resId),
                Toast.LENGTH_SHORT).show();
    }
}
