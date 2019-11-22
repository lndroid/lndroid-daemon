package org.lndroid.lnd.daemon;

public interface ISendStream<RequestType> {
    void send(RequestType r) throws LightningException;
    void stop() throws LightningException;
}

