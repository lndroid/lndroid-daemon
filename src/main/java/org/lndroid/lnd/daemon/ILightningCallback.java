package org.lndroid.lnd.daemon;

public interface ILightningCallback<ResponseType> {
    void onResponse(ResponseType r);
    void onError(int code, String error);
}
