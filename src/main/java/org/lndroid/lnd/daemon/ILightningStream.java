package org.lndroid.lnd.daemon;

public interface ILightningStream<RequestType, ResponseType> {
    // set the reply callback
    void setRecvCallback(ILightningCallback<ResponseType> cb);
    // send request
    void send(RequestType r) throws LightningException;
    // call after all requests were sent,
    // to signal that no more requests will come
    void done() throws LightningException;
    // call to immediately terminate all in-flight requests
    void cancel() throws LightningException;
    // if error reply was received or stop was called (including
    // after last receive when stopWhenDone was called),
    // this will return 'false'
    boolean isValid ();
    // number of sends
    int requestCount();
    // number of non-error replies
    int replyCount();
    // number of requests in flight
    int activeCount();
}
