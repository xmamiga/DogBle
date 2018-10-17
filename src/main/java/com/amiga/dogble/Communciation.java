package com.amiga.dogble;

/**
 * Date: 2018/10/13-12:09
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public interface Communciation<T> {
    public T getCommunicationChannel(String addr);

    public void commucateInitAall();

    public void commucateInit(String addr);

    public boolean getCommunication(String addr);

    public boolean isCommunicte(String macAddr);
}
