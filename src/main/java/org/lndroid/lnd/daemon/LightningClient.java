package org.lndroid.lnd.daemon;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.lndroid.lnd.data.Data;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class LightningClient implements ILightningClient {

    private static final String TAG = "LightningClient";

    // dispatcher is owned by caller thread and
    // thus will not leak the inner-class-callbacks
    // that it stores to Daemon thread
    static class Dispatcher extends Handler {

        private static final String TAG = "LightningDispatcher";

        public static class RequestReplyStream<RequestType> implements ILightningStream<RequestType> {

            private Dispatcher parent_;
            private int id_;
            private ILightningCallback callback_;
            private ISendStream<RequestType> sendStream_;
            private int requestCount_;
            private int replyCount_;
            private boolean stream_;
            private boolean done_;
            private boolean error_;

            RequestReplyStream(Dispatcher parent, int id) {
                parent_ = parent;
                id_ = id;
                parent_.streams_.put(id_, this);
            }

            int id() {
                return id_;
            }

            void setSendStream(ISendStream<RequestType> ss) {
                sendStream_ = ss;
            }

            void setRecvCallback(ILightningCallback cb, boolean stream) {
                callback_ = cb;
                stream_ = stream;
            }

            // set the reply callback
            @Override
            public void setRecvCallback(ILightningCallback cb) {
                setRecvCallback(cb, true);
            }

            public void recvCallback(int code, Object obj) {
                // update state so that callback could use it
                if (code != 0)
                    error_ = true;
                else
                    replyCount_++;

                // callback
                callback_.onCall(code, obj);

                // stop if done and not stopped yet
                if (done_ && activeCount() == 0 && isValid() ) {
                    try {
                        cancel();
                    } catch (LightningException e) {
                        error_ = true;
                        callback_.onCall(e.errorCode(), e.errorMessage());
                    }
                }
            }

            // send request.
            @Override
            public void send(RequestType r) throws LightningException {
                if (!isValid())
                    throw new LightningException(-1, "Stream invalid");
                sendStream_.send(r);
                requestCount_++;
            }

            // call after all requests were sent,
            // to signal that no more requests will come.
            @Override
            public void done() throws LightningException {
                if (!isValid())
                    throw new LightningException(-1, "Stream invalid");

                // mark as done
                done_ = true;
                // no in-flight requests? stop immediately
                if (activeCount() == 0)
                    cancel();
            }

            // call to immediately terminate all in-flight requests
            @Override
            public void cancel() throws LightningException {
                if (!isValid())
                    throw new LightningException(-1, "Stream invalid");
                try {
                    sendStream_.stop();
                } catch (LightningException e) {
                    parent_.streams_.remove(id_);
                    throw e;
                } finally {
                    sendStream_ = null;
                }
            }

            // if error reply was received or stop was called (including
            // after last receive when stopWhenDone was called),
            // this will return 'false'
            @Override
            public boolean isValid () {
                return !error_ && sendStream_ != null && callback_ != null;
            }

            // number of sends
            @Override
            public int requestCount() {
                return requestCount_;
            }

            // number of non-error replies
            @Override
            public int replyCount() {
                return replyCount_;
            }

            // number of requests in flight
            @Override
            public int activeCount() {
                return requestCount_ - replyCount_;
            }
        }

        private HashMap<Integer, RequestReplyStream> streams_ = new HashMap<>();
        private int nextId_ = 1;

        @Override
        public void handleMessage(Message msg) {
            RequestReplyStream s = streams_.get(msg.what);
            if (s == null) {
                Log.e(TAG, "Unknown message "+msg.what);
                return;
            }

            if (s.callback_ == null){
                Log.e(TAG, "Empty callback for message "+msg.what);
                return;
            }

            s.recvCallback(msg.arg1, msg.obj);

            // remove stream if it was rpc
            // or when it's got an error (which includes when 'stop' was called)
            if (!s.stream_ || s.error_) {
                streams_.remove(msg.what);
            }
        }

        <RequestType> RequestReplyStream<RequestType> createStream() {
            return new RequestReplyStream<>(this, nextId_++);
        }

        int createCallback(ILightningCallback callback) {
            RequestReplyStream s = createStream();
            s.setRecvCallback(callback, false);
            return s.id();
        }

        int createRecvStream(ILightningCallback callback) {
            RequestReplyStream s = createStream();
            s.setRecvCallback(callback, true);
            return s.id();
        }

        void onDestroy() {
            for(int i: streams_.keySet()) {
                RequestReplyStream s = streams_.get(i);
                if (s == null || !s.isValid())
                    continue;

                try {
                    s.cancel();;
                } catch (LightningException e) {
                    Log.e(TAG, "send stream stop error: "+e.getMessage());
                }
            }

            // ensure
            streams_.clear();
        }
    }

    // These callbacks are passed to the Daemon thread,
    // and are weakly-referencing our handler,
    // so that when daemon calls it back, we can check
    // if handler was GC-ed and not call it, and not
    // block GC from freeing it (bcs essentially
    // our client is owned by to-be-destroyed object, like
    // an Activity that was closed)
    private static class MTCallback implements ILightningCallbackMT {
        private WeakReference<Handler> handler_;
        private int what_;

        MTCallback(Handler handler, int what){
            handler_ = new WeakReference<>(handler);
            what_ = what;
        }

        @Override
        public void onError(int code, String message) {
            if (handler_.get() != null)
                handler_.get().sendMessage(handler_.get().obtainMessage(what_, code, 0, message));
        }

        @Override
        public void onResponse(Object o) {
            if (handler_.get() != null)
                handler_.get().sendMessage(handler_.get().obtainMessage(what_, o));
        }
    }

    private Dispatcher dispatcher_ = new Dispatcher();

    LightningClient() {
    }

    @Override
    public void onDestroy() {
        dispatcher_.onDestroy();
    }

    @Override
    public void unlockWallet(Data.UnlockWalletRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.unlockWalletMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void initWallet(Data.InitWalletRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.initWalletMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void genSeed(Data.GenSeedRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.genSeedMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void newAddress(Data.NewAddressRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.newAddressMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void getInfo(Data.GetInfoRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.getInfoMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void walletBalance(Data.WalletBalanceRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.walletBalanceMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void connectPeer(Data.ConnectPeerRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.connectPeerMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void listChannels(Data.ListChannelsRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.listChannelsMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void addInvoice(Data.Invoice r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.addInvoiceMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void subscribeTransactions(Data.GetTransactionsRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createRecvStream(cb);
        LightningDaemon.subscribeTransactionsMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void getTransactions(Data.GetTransactionsRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.getTransactionsMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void openChannel(Data.OpenChannelRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createRecvStream(cb);
        LightningDaemon.openChannelMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void openChannelSync(Data.OpenChannelRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.openChannelSyncMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void closeChannel(Data.CloseChannelRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createRecvStream(cb);
        LightningDaemon.closeChannelMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void estimateFee(Data.EstimateFeeRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.estimateFeeMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public void sendCoins(Data.SendCoinsRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.sendCoinsMT(r, new MTCallback(dispatcher_, what));
    }

    @Override
    public ILightningStream<Data.SendRequest> sendPaymentsStream() {
        // allocate bi-stream,
        // register it,
        // call MT w/ MTCallback(what)
        // set send stream
        // return bi-stream
        Dispatcher.RequestReplyStream<Data.SendRequest> stream = dispatcher_.createStream();
        ISendStream<Data.SendRequest> ss = LightningDaemon.sendPaymentMT(
                new MTCallback(dispatcher_, stream.id()));
        stream.setSendStream(ss);
        return stream;
    }

    // overloaded variant for simple cases where cb can be defined before stream is created
    @Override
    public ILightningStream<Data.SendRequest> sendPaymentsStream(ILightningCallback cb) {
        ILightningStream<Data.SendRequest> stream = sendPaymentsStream();
        stream.setRecvCallback(cb);
        return stream;
    }

    @Override
    public void sendPayment(Data.SendRequest r, ILightningCallback cb) {
        final int what = dispatcher_.createCallback(cb);
        LightningDaemon.sendPaymentMT(r, new MTCallback(dispatcher_, what));
    }

}
