package org.lndroid.lnd.daemon;

public interface ILightningCallbackMT {

    void onError(int code, String message);
    void onResponse(Object o);
}