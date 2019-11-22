package org.lndroid.lnd.daemon;

public interface ILightningSendStream<RequestType> {
    void send(RequestType r) throws LightningException;
    void stop() throws LightningException;
}

