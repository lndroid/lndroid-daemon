package org.lndroid.lnd.daemon;

public interface ILightningCallback {
    void onCall(int code, Object object);
}

interface ILightningCallbackSafe<ResponseType> {
    void onResponse(ResponseType r);
    void onError(int code, String error);
}
