package com.cchip.maddogbt.utils;

import android.support.v4.internal.view.SupportMenu;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Formatter;

/**
 * Date: 2018/10/16-16:13
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class Conversion {
    public static byte loUint16(long v) {
        return (byte) (v & 0xFF);
    }

    public static byte hiUint16(long v) {
        return (byte) (v >> 8);
    }

    public static long buildUint16(byte hi, byte lo) {
        if ((hi & 0x80) == 0) {
            return ((((long) hi) << 8) | (((long) lo) & 0xff));
        } else {
            long temphi = (long) hi & 0x7f;
            long returnvalue = (temphi << 8) | (lo & 0xff) | 0x8000;
            return returnvalue;
        }
    }

    public static String BytetohexString(byte[] b, int len) {
        StringBuilder sb = new StringBuilder(b.length * (2 + 1));
        Formatter formatter = new Formatter(sb);

        for (int i = 0; i < len; i++) {
            if (i < len - 1)
                formatter.format("%02X:", b[i]);
            else
                formatter.format("%02X", b[i]);

        }
        formatter.close();

        return sb.toString();
    }

    static String BytetohexString(byte[] b, boolean reverse) {
        StringBuilder sb = new StringBuilder(b.length * (2 + 1));
        Formatter formatter = new Formatter(sb);

        if (!reverse) {
            for (int i = 0; i < b.length; i++) {
                if (i < b.length - 1)
                    formatter.format("%02X:", b[i]);
                else
                    formatter.format("%02X", b[i]);

            }
        } else {
            for (int i = (b.length - 1); i >= 0; i--) {
                if (i > 0)
                    formatter.format("%02X:", b[i]);
                else
                    formatter.format("%02X", b[i]);

            }
        }
        formatter.close();

        return sb.toString();
    }

    public static int hexStringtoByte(String sb, byte[] results) {

        int i = 0;
        boolean j = false;

        if (sb != null) {
            for (int k = 0; k < sb.length(); k++) {
                if (((sb.charAt(k)) >= '0' && (sb.charAt(k) <= '9'))
                        || ((sb.charAt(k)) >= 'a' && (sb.charAt(k) <= 'f'))
                        || ((sb.charAt(k)) >= 'A' && (sb.charAt(k) <= 'F'))) {
                    if (j) {
                        results[i] += (byte) (Character.digit(sb.charAt(k), 16));
                        i++;
                    } else {
                        results[i] = (byte) (Character.digit(sb.charAt(k), 16) << 4);
                    }
                    j = !j;
                }
            }
        }
        return i;
    }

    public static boolean isAsciiPrintable(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (isAsciiPrintable(str.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiPrintable(char ch) {
        return ch >= 32 && ch < 127;
    }


    public static int extractIntField(byte[] source, int offset, int length, boolean reverse) {
        int i = 1;
        int i2 = length < 0 ? 1 : 0;
        if (length <= 4) {
            i = 0;
        }
        if ((i | i2) != 0) {
            throw new IndexOutOfBoundsException("Length must be between 0 and 4");
        }
        int result = 0;
        int shift = (length - 1) * 8;
        int i3;
        if (reverse) {
            for (i3 = (offset + length) - 1; i3 >= offset; i3--) {
                result |= (source[i3] & 255) << shift;
                shift -= 8;
            }
        } else {
            for (i3 = offset; i3 < offset + length; i3++) {
                result |= (source[i3] & 255) << shift;
                shift -= 8;
            }
        }
        return result;
    }

    public static String getStringFromBytes(byte[] value) {
        if (value == null) {
            return "null";
        }
        String out = "";
        for (byte b : value) {
            out = out + String.format("0x%02x ", new Object[] { Byte.valueOf(b) });
        }
        return out;
    }

    public static void putArrayField(byte[] source, int sourceOffset, byte[] target, int targetOffset, int length,
                                     boolean reverse) {
        if (reverse) {
            int j = (sourceOffset + length) - 1;
            for (int i = targetOffset; i < targetOffset + length; i++) {
                target[i] = source[j];
                j--;
            }
            return;
        }
        System.arraycopy(source, sourceOffset, target, targetOffset, length);
    }

    public static byte[] getMD5FromFile(File file) {
        byte[] digest = new byte[] {};
        Throwable th;
        InputStream inputStream = null;
        try {
            InputStream inputStream2 = new FileInputStream(file);
            try {
                byte[] buffer = new byte[1024];
                MessageDigest digest2 = MessageDigest.getInstance("MD5");
                int numRead = 0;
                while (numRead != -1) {
                    numRead = inputStream2.read(buffer);
                    if (numRead > 0) {
                        digest2.update(buffer, 0, numRead);
                    }
                }
                digest = digest2.digest();
                if (inputStream2 != null) {
                    try {
                        inputStream2.close();
                    } catch (Exception e) {
                    }
                }
                inputStream = inputStream2;
            } catch (Exception e2) {
                inputStream = inputStream2;
                digest = null;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e3) {
                    }
                }
                return digest;
            } catch (Throwable th2) {
                th = th2;
                inputStream = inputStream2;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            digest = null;
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return digest;
        } catch (Throwable th3) {
            th = th3;
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                throw th;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return digest;
    }

    public static byte[] getBytesFromFile(File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
            byte[] result = new byte[Long.valueOf(file.length()).intValue()];
            inputStream.read(result);
            inputStream.close();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getIntToHexadecimal(int i) {
        return String.format("%04X", new Object[] { Integer.valueOf(SupportMenu.USER_MASK & i) });
    }

    /**
     * To retrieve a time format from a long in millisecond.
     *
     * @param time
     *              the time in ms.
     *
     * @return
     *          The time with a format like --h or --m or --s depending on which biggest kind is different of 0.
     */
    public static String getStringFromTime(long time) {
        long seconds = time / 1000;

        if (seconds > 60) {
            long minutes = seconds / 60;

            if (minutes > 60) {
                long hours = minutes / 60;
                return hours + "h";
            }
            else {
                return minutes + "min";
            }
        }
        else {
            return seconds + "s";
        }
    }
}
