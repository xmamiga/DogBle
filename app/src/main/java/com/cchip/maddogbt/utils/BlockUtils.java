package com.cchip.maddogbt.utils;

import android.text.Html;
import android.widget.TextView;

import com.cchip.maddogbt.bean.ImgHdr;

/**
 * Date: 2018/10/16-16:21
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class BlockUtils {
    public static void displayImageInfo(TextView v, ImgHdr h) {
        long imgVer = (h.ver) >> 1;
        long imgSize = h.len * 4;
        String s = String.format("Type: %c Ver.: %d Size: %d", h.imgType,
                imgVer, imgSize);
        v.setText(Html.fromHtml(s));
    }

    public static int calc_crc16(byte[] data, int len) {
        int i, j;
        byte ds;
        int crc = 0xffff;
        int poly[] = { 0, 0xa001 };

        for (j = 0; j < len; j++) {
            ds = data[j];
            for (i = 0; i < 8; i++) {
                crc = (crc >> 1) ^ poly[(crc ^ ds) & 1];
                ds = (byte) (ds >> 1);
            }
        }
        return crc;
    }

    public static boolean waitIdle2(int timeout) {
        timeout /= 10;
        while (--timeout > 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return timeout > 0;
    }
}
