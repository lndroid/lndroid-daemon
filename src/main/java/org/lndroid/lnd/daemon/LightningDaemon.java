package org.lndroid.lnd.daemon;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lndroid.lnd.data.Data;
import org.lndroid.lnd.data.Codec;

import lndmobile.Lndmobile;

public final class LightningDaemon {

    public static class Init {
        public String dir;
        public boolean noMacaroons;
        public boolean mainnet;
        public String debugLevel = "";
        public boolean autopilot;
        public boolean acceptKeysend;
        public List<String> connectPeers;
        public List<String> onlyPeers;
        public String banDuration;
        public String tlsCertPath;
        public String tlsKeyPath;
    }

    private static final String TAG = "LightningDaemon";

    private static AtomicBoolean starting_ = new AtomicBoolean(false);
    private static AtomicBoolean started_ = new AtomicBoolean(false);
    private static AtomicBoolean unlocked_ = new AtomicBoolean(false);
    private static AtomicBoolean unlockReady_ = new AtomicBoolean(false);
    private static AtomicBoolean rpcReady_ = new AtomicBoolean(false);

    static class FutureCallback<Response>  extends FutureTask<Response> implements ILightningCallbackMT {

        private static final String TAG = "LightningFuture";

        FutureCallback() {
            // dumb noop callable
            super(new Callable<Response>(){
                @Override
                public Response call() throws Exception {
                    return null;
                }
            });
        }

        @Override
        public void onError(int code, String message) {

            Log.i(TAG, "error "+Thread.currentThread().getId());
            setException(new LightningException(code, message));
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onResponse(Object o) {
            Log.i(TAG, "response "+Thread.currentThread().getId());
            set((Response) o);
        }
    }

    private static void writeConf(String dir) throws LightningException {
        new File(dir).mkdirs();

        File file = new File(dir+"/lnd.conf");
        FileWriter fr = null;

        // these settings are not exposed through command line,
        // so we have to pass them through conf file
        String data =
                "[Application Options]\n" +
                "maxbackoff=2s\n" +
                "[Routing]\n"+
                "routing.assumechanvalid=1\n";
        ;
        try {
            fr = new FileWriter(file);
            fr.write(data);
            fr.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new LightningException(-1, e.getMessage());
        }
    }

    // thread-safe, executed only once per process
    public static void start(Init init,
                             final ILightningCallbackMT unlockReadyCb,
                             final ILightningCallbackMT rpcReadyCb) throws LightningException {

        if (!starting_.compareAndSet(false, true))
            return;

        String cmd = "--bitcoin.active --bitcoin.node=neutrino --nolisten --norest ";

        if (init.dir != null)
            cmd += " --lnddir=" + init.dir;

        if (init.acceptKeysend)
            cmd += " --accept-keysend";

        if (init.noMacaroons)
            cmd += " --no-macaroons";

        if (init.tlsCertPath != null)
            cmd += " --tlscertpath="+init.tlsCertPath;
        if (init.tlsKeyPath != null)
            cmd += " --tlskeypath="+init.tlsKeyPath;

        if (init.mainnet)
            cmd += " --bitcoin.mainnet";
        else
            cmd += " --bitcoin.testnet";

        if (!init.debugLevel.equals(""))
            cmd += " --debuglevel="+init.debugLevel;
        else
            cmd += " --debuglevel=debug";

        if (init.autopilot)
            cmd += " --autopilot.active";

        if (init.banDuration != null)
            cmd += " --neutrino.banduration="+init.banDuration;

        if (init.connectPeers != null) {
            for (String p: init.connectPeers) {
                cmd += " --neutrino.addpeer="+p;
            }
        }

        if (init.onlyPeers != null) {
            for (String p: init.onlyPeers) {
                cmd += " --neutrino.connect="+p;
            }
        }

        Log.i(TAG, "start command " + cmd + " thread " + Thread.currentThread().getId());

        writeConf(init.dir);

        Lndmobile.start(cmd, new lndmobile.Callback() {
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "unlock ready error " + e.getMessage()
                        +" thread "+Thread.currentThread().getId());
                try {
                    throw e;
                } catch (LightningException le) {
                    unlockReadyCb.onError(le.errorCode(), le.errorMessage());
                } catch (Exception ee) {
                    unlockReadyCb.onError(-1, ee.getMessage());
                }
            }

            @Override
            public void onResponse(byte[] bytes) {
                unlockReady_.set(true);
                unlockReadyCb.onResponse(null);
            }
        }, new lndmobile.Callback() {
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "rpc ready error " + e.getMessage()
                        +" thread "+Thread.currentThread().getId());
                try {
                    throw e;
                } catch (LightningException le) {
                    rpcReadyCb.onError(le.errorCode(), le.errorMessage());
                } catch (Exception ee) {
                    rpcReadyCb.onError(-1, ee.getMessage());
                }
            }

            @Override
            public void onResponse(byte[] bytes) {
                rpcReady_.set(true);
                rpcReadyCb.onResponse(null);
            }
        });

        Log.i(TAG, "start initiated");
        started_.set(true);
    }

    public static boolean isStarted() {
        return started_.get();
    }

    public static boolean isUnlocked() {
        return unlocked_.get();
    }

    public static boolean isUnlockReady() {
        return unlockReady_.get();
    }

    public static boolean isRpcReady() {
        return rpcReady_.get();
    }

    // noop at the moment
    public static void stop() throws LightningException {
        // make it noop for now, so that
        // daemon is started once, per process, and can never be
        // stopped until the entire process is destroyed by OS

/*        if (!starting_.get())
            return;

        Log.i(TAG, "stopping");
        Lndmobile.stopDaemon(new byte[0], cb);
        // FIXME stopDaemon never calls us back, and never actually stops
        // until we unlock the wallet
 */
    }

    public static ILightningClient createClient() {
        return new LightningClient();
    }

    static class LndmobileCallback<ResponseType extends com.google.protobuf.Message>
            implements lndmobile.Callback, lndmobile.RecvStream {

        private String label_;
        private com.google.protobuf.Parser<ResponseType> parser_;
        private ILightningCallbackMT mtcb_;

        LndmobileCallback(String label, com.google.protobuf.Parser<ResponseType> parser, ILightningCallbackMT mtcb) {
            label_ = label;
            parser_ = parser;
            mtcb_ = mtcb;
        }

        @Override
        public void onError(Exception e){
            Log.e(TAG, "called " + label_ + " error " + e.getMessage()
                    +" thread "+Thread.currentThread().getId());
            try {
                throw e;
            } catch (LightningException le) {
                mtcb_.onError(le.errorCode(), le.errorMessage());
            } catch (Exception ee) {
                mtcb_.onError(-1, ee.getMessage());
            }
        }

        @Override
        public void onResponse(byte[] bytes){
            Log.i(TAG, "called " + label_ + " ok"+" thread "+Thread.currentThread().getId());

            try {
                ResponseType resp = parser_.parseFrom(bytes != null ? bytes : new byte[0]);
                Log.i(TAG, "resp " + label_ + " "+resp);
                mtcb_.onResponse(resp);
            } catch (Exception e) {
                Log.e(TAG, "bad reply from " + label_ + ": " + e.getMessage());
                mtcb_.onError(-2, e.getMessage());
            }
        }
    }

    private interface CallImpl {
        void onCall(byte[] data, LndmobileCallback cb);
    }

    private static <ResponseType extends com.google.protobuf.Message>
    void callMT(
            final String label, com.google.protobuf.Message req,
            final com.google.protobuf.Parser<ResponseType> parser,
            final ILightningCallbackMT mtcb,
            CallImpl impl) {

        Log.i(TAG, "calling " + label+" thread "+Thread.currentThread().getId()+" req "+req);

        impl.onCall(req.toByteArray(), new LndmobileCallback<ResponseType>(label, parser, mtcb));
    }

    interface FutureCallImpl<RequestType, ResponseType> {
        void onCall(RequestType r, FutureCallback<ResponseType> cb);
    }

    private static <ResponseType, RequestType>
    Future<ResponseType> callFuture(RequestType r, FutureCallImpl<RequestType, ResponseType> impl) {
        FutureCallback<ResponseType> cb = new FutureCallback<>();
        impl.onCall(r, cb);
        return cb;
    }

    interface SyncCallImpl<RequestType, ResponseType> {
        Future<ResponseType> onCall(RequestType r);
    }

    private static <RequestType, ResponseType>
    ResponseType callSync(RequestType r, SyncCallImpl<RequestType, ResponseType> impl) throws LightningException {

        Future<ResponseType> f = impl.onCall(r);
        try {
            return f.get();
        } catch (ExecutionException e) {
            LightningException le = (LightningException)e.getCause();
            if (le != null)
                throw le;
            else
                throw new LightningException(-3, "Unknown execution error");
        } catch (InterruptedException e) {
            throw new LightningException(-2, "Interrupted");
        }
    }

    // ======================
    // UnlockWallet
    public static void unlockWalletMT(Data.UnlockWalletRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.UnlockWalletRequest req = Codec.encode(r);

        callMT("unlockWallet", req, lnrpc.Rpc.UnlockWalletResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                if (message.contains("wallet not found"))
                    mtcb.onError(1, message);
                else if (message.contains("transport is closing")) {
                    // FIXME remove when this is fixed
                    unlocked_.set(true);
                    mtcb.onResponse(new Data.UnlockWalletResponse());
                }
                else
                    mtcb.onError(-1, message);
            }

            @Override
            public void onResponse(Object o) {
                unlocked_.set(true);
                mtcb.onResponse(Codec.decode((lnrpc.Rpc.UnlockWalletResponse)o));
            }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.unlockWallet(data, cb);
            }
        });
    }

    public static Future<Data.UnlockWalletResponse> unlockWalletFuture(Data.UnlockWalletRequest r) {
        return callFuture(r, new FutureCallImpl<Data.UnlockWalletRequest, Data.UnlockWalletResponse> () {
            @Override
            public void onCall(Data.UnlockWalletRequest r, FutureCallback<Data.UnlockWalletResponse> cb) {
                unlockWalletMT(r, cb);
            }
        });
    }

    public static Data.UnlockWalletResponse unlockWalletSync(Data.UnlockWalletRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.UnlockWalletRequest, Data.UnlockWalletResponse> () {
            @Override
            public Future<Data.UnlockWalletResponse> onCall(Data.UnlockWalletRequest r) {
                return unlockWalletFuture(r);
            }
        });
    }

    // ======================
    // GenSeed
    public static void genSeedMT(Data.GenSeedRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.GenSeedRequest req = Codec.encode(r);

        callMT("genSeed", req, lnrpc.Rpc.GenSeedResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.GenSeedResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.genSeed(data, cb);
            }
        });
    }

    public static Future<Data.GenSeedResponse> genSeedFuture(Data.GenSeedRequest r) {
        return callFuture(r, new FutureCallImpl<Data.GenSeedRequest, Data.GenSeedResponse> () {
            @Override
            public void onCall(Data.GenSeedRequest r, FutureCallback<Data.GenSeedResponse> cb) {
                genSeedMT(r, cb);
            }
        });
    }

    public static Data.GenSeedResponse genSeedSync(Data.GenSeedRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.GenSeedRequest, Data.GenSeedResponse> () {
            @Override
            public Future<Data.GenSeedResponse> onCall(Data.GenSeedRequest r) {
                return genSeedFuture(r);
            }
        });
    }

    // ======================
    // InitWallet
    public static void initWalletMT(Data.InitWalletRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.InitWalletRequest req = Codec.encode(r);

        callMT("initWallet", req, lnrpc.Rpc.InitWalletResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) {
                unlocked_.set(true);
                mtcb.onResponse(Codec.decode((lnrpc.Rpc.InitWalletResponse)o));
            }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.initWallet(data, cb);
            }
        });
    }

    public static Future<Data.InitWalletResponse> initWalletFuture(Data.InitWalletRequest r) {
        return callFuture(r, new FutureCallImpl<Data.InitWalletRequest, Data.InitWalletResponse> () {
            @Override
            public void onCall(Data.InitWalletRequest r, FutureCallback<Data.InitWalletResponse> cb) {
                initWalletMT(r, cb);
            }
        });
    }

    public static Data.InitWalletResponse initWalletSync(Data.InitWalletRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.InitWalletRequest, Data.InitWalletResponse> () {
            @Override
            public Future<Data.InitWalletResponse> onCall(Data.InitWalletRequest r) {
                return initWalletFuture(r);
            }
        });
    }

    // ======================
    // NewAddress
    public static void newAddressMT(Data.NewAddressRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.NewAddressRequest req = Codec.encode(r);

        callMT("newAddress", req, lnrpc.Rpc.NewAddressResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.NewAddressResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.newAddress(data, cb);
            }
        });
    }

    public static Future<Data.NewAddressResponse> newAddressFuture(Data.NewAddressRequest r) {
        return callFuture(r, new FutureCallImpl<Data.NewAddressRequest, Data.NewAddressResponse> () {
            @Override
            public void onCall(Data.NewAddressRequest r, FutureCallback<Data.NewAddressResponse> cb) {
                newAddressMT(r, cb);
            }
        });
    }

    public static Data.NewAddressResponse newAddressSync(Data.NewAddressRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.NewAddressRequest, Data.NewAddressResponse> () {
            @Override
            public Future<Data.NewAddressResponse> onCall(Data.NewAddressRequest r) {
                return newAddressFuture(r);
            }
        });
    }

    // ======================
    // GetInfo
    public static void getInfoMT(Data.GetInfoRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.GetInfoRequest req = Codec.encode(r);

        callMT("getInfo", req, lnrpc.Rpc.GetInfoResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.GetInfoResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.getInfo(data, cb);
            }
        });
    }

    public static Future<Data.GetInfoResponse> getInfoFuture(Data.GetInfoRequest r) {
        return callFuture(r, new FutureCallImpl<Data.GetInfoRequest, Data.GetInfoResponse> () {
            @Override
            public void onCall(Data.GetInfoRequest r, FutureCallback<Data.GetInfoResponse> cb) {
                getInfoMT(r, cb);
            }
        });
    }

    public static Data.GetInfoResponse getInfoSync(Data.GetInfoRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.GetInfoRequest, Data.GetInfoResponse> () {
            @Override
            public Future<Data.GetInfoResponse> onCall(Data.GetInfoRequest r) {
                return getInfoFuture(r);
            }
        });
    }

    // ======================
    // WalletBalance
    public static void walletBalanceMT(Data.WalletBalanceRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.WalletBalanceRequest req = Codec.encode(r);

        callMT("walletBalance", req, lnrpc.Rpc.WalletBalanceResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.WalletBalanceResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.walletBalance(data, cb);
            }
        });
    }

    public static Future<Data.WalletBalanceResponse> walletBalanceFuture(Data.WalletBalanceRequest r) {
        return callFuture(r, new FutureCallImpl<Data.WalletBalanceRequest, Data.WalletBalanceResponse> () {
            @Override
            public void onCall(Data.WalletBalanceRequest r, FutureCallback<Data.WalletBalanceResponse> cb) {
                walletBalanceMT(r, cb);
            }
        });
    }

    public static Data.WalletBalanceResponse walletBalanceSync(Data.WalletBalanceRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.WalletBalanceRequest, Data.WalletBalanceResponse> () {
            @Override
            public Future<Data.WalletBalanceResponse> onCall(Data.WalletBalanceRequest r) {
                return walletBalanceFuture(r);
            }
        });
    }

    // ======================
    // ChannelBalance
    public static void channelBalanceMT(Data.ChannelBalanceRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.ChannelBalanceRequest req = Codec.encode(r);

        callMT("channelBalance", req, lnrpc.Rpc.ChannelBalanceResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.ChannelBalanceResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.channelBalance(data, cb);
            }
        });
    }

    public static Future<Data.ChannelBalanceResponse> channelBalanceFuture(Data.ChannelBalanceRequest r) {
        return callFuture(r, new FutureCallImpl<Data.ChannelBalanceRequest, Data.ChannelBalanceResponse> () {
            @Override
            public void onCall(Data.ChannelBalanceRequest r, FutureCallback<Data.ChannelBalanceResponse> cb) {
                channelBalanceMT(r, cb);
            }
        });
    }

    public static Data.ChannelBalanceResponse channelBalanceSync(Data.ChannelBalanceRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.ChannelBalanceRequest, Data.ChannelBalanceResponse> () {
            @Override
            public Future<Data.ChannelBalanceResponse> onCall(Data.ChannelBalanceRequest r) {
                return channelBalanceFuture(r);
            }
        });
    }

    // ======================
    // ConnectPeer
    public static void connectPeerMT(Data.ConnectPeerRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.ConnectPeerRequest req = Codec.encode(r);

        callMT("connectPeer", req, lnrpc.Rpc.ConnectPeerResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.ConnectPeerResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.connectPeer(data, cb);
            }
        });
    }

    public static Future<Data.ConnectPeerResponse> connectPeerFuture(Data.ConnectPeerRequest r) {
        return callFuture(r, new FutureCallImpl<Data.ConnectPeerRequest, Data.ConnectPeerResponse> () {
            @Override
            public void onCall(Data.ConnectPeerRequest r, FutureCallback<Data.ConnectPeerResponse> cb) {
                connectPeerMT(r, cb);
            }
        });
    }

    public static Data.ConnectPeerResponse connectPeerSync(Data.ConnectPeerRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.ConnectPeerRequest, Data.ConnectPeerResponse> () {
            @Override
            public Future<Data.ConnectPeerResponse> onCall(Data.ConnectPeerRequest r) {
                return connectPeerFuture(r);
            }
        });
    }

    // ======================
    // ListChannels
    public static void listChannelsMT(Data.ListChannelsRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.ListChannelsRequest req = Codec.encode(r);

        callMT("listChannels", req, lnrpc.Rpc.ListChannelsResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.ListChannelsResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.listChannels(data, cb);
            }
        });
    }

    public static Future<Data.ListChannelsResponse> listChannelsFuture(Data.ListChannelsRequest r) {
        return callFuture(r, new FutureCallImpl<Data.ListChannelsRequest, Data.ListChannelsResponse> () {
            @Override
            public void onCall(Data.ListChannelsRequest r, FutureCallback<Data.ListChannelsResponse > cb) {
                listChannelsMT(r, cb);
            }
        });
    }

    public static Data.ListChannelsResponse listChannelsSync(Data.ListChannelsRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.ListChannelsRequest, Data.ListChannelsResponse> () {
            @Override
            public Future<Data.ListChannelsResponse> onCall(Data.ListChannelsRequest r) {
                return listChannelsFuture(r);
            }
        });
    }

    // ======================
    // AddInvoice
    public static void addInvoiceMT(Data.Invoice r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.Invoice req = Codec.encode(r);

        callMT("addInvoice", req, lnrpc.Rpc.AddInvoiceResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.AddInvoiceResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.addInvoice(data, cb);
            }
        });
    }

    public static Future<Data.AddInvoiceResponse> addInvoiceFuture(Data.Invoice r) {
        return callFuture(r, new FutureCallImpl<Data.Invoice, Data.AddInvoiceResponse>() {
            @Override
            public void onCall(Data.Invoice r, FutureCallback<Data.AddInvoiceResponse> cb) {
                addInvoiceMT(r, cb);
            }
        });
    }

    public static Data.AddInvoiceResponse addInvoiceSync(Data.Invoice r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.Invoice, Data.AddInvoiceResponse> () {
            @Override
            public Future<Data.AddInvoiceResponse> onCall(Data.Invoice r) {
                return addInvoiceFuture(r);
            }
        });
    }

    // ======================
    // SubscribeTransactions
    public static void subscribeTransactionsMT(Data.GetTransactionsRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.GetTransactionsRequest req = Codec.encode(r);

        callMT("subscribeTransactions", req, lnrpc.Rpc.Transaction.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.Transaction)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.subscribeTransactions(data, cb);
            }
        });
    }

    // ======================
    // GetTransactions
    public static void getTransactionsMT(Data.GetTransactionsRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.GetTransactionsRequest req = Codec.encode(r);

        callMT("getTransactions", req, lnrpc.Rpc.TransactionDetails.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.TransactionDetails)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.getTransactions(data, cb);
            }
        });
    }
    public static Future<Data.TransactionDetails> getTransactionsFuture(Data.GetTransactionsRequest r) {
        return callFuture(r, new FutureCallImpl<Data.GetTransactionsRequest, Data.TransactionDetails>() {
            @Override
            public void onCall(Data.GetTransactionsRequest r, FutureCallback<Data.TransactionDetails> cb) {
                getTransactionsMT(r, cb);
            }
        });
    }
    public static Data.TransactionDetails getTransactionsSync(Data.GetTransactionsRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.GetTransactionsRequest, Data.TransactionDetails> () {
            @Override
            public Future<Data.TransactionDetails> onCall(Data.GetTransactionsRequest r) {
                return getTransactionsFuture(r);
            }
        });
    }

    // ======================
    // OpenChannel
    public static void openChannelMT(Data.OpenChannelRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.OpenChannelRequest req = Codec.encode(r);

        callMT("openChannel", req, lnrpc.Rpc.OpenStatusUpdate.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.OpenStatusUpdate)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.openChannel(data, cb);
            }
        });
    }
    public static void openChannelSyncMT(Data.OpenChannelRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.OpenChannelRequest req = Codec.encode(r);

        callMT("openChannelSync", req, lnrpc.Rpc.ChannelPoint.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.ChannelPoint)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.openChannelSync(data, cb);
            }
        });
    }
    public static Future<Data.ChannelPoint> openChannelFuture(Data.OpenChannelRequest r) {
        return callFuture(r, new FutureCallImpl<Data.OpenChannelRequest, Data.ChannelPoint>() {
            @Override
            public void onCall(Data.OpenChannelRequest r, FutureCallback<Data.ChannelPoint> cb) {
                openChannelSyncMT(r, cb);
            }
        });
    }
    public static Data.ChannelPoint openChannelSync(Data.OpenChannelRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.OpenChannelRequest, Data.ChannelPoint> () {
            @Override
            public Future<Data.ChannelPoint> onCall(Data.OpenChannelRequest r) {
                return openChannelFuture(r);
            }
        });
    }

    // ======================
    // CloseChannel
    public static void closeChannelMT(Data.CloseChannelRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.CloseChannelRequest req = Codec.encode(r);

        callMT("openChannel", req, lnrpc.Rpc.ClosedChannelUpdate.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.CloseStatusUpdate)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.closeChannel(data, cb);
            }
        });
    }

    // ======================
    // EstimateFee
    public static void estimateFeeMT(Data.EstimateFeeRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.EstimateFeeRequest req = Codec.encode(r);

        callMT("estimateFee", req, lnrpc.Rpc.EstimateFeeRequest.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.EstimateFeeResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.estimateFee(data, cb);
            }
        });
    }
    public static Future<Data.EstimateFeeResponse> estimateFeeFuture(Data.EstimateFeeRequest r) {
        return callFuture(r, new FutureCallImpl<Data.EstimateFeeRequest, Data.EstimateFeeResponse>() {
            @Override
            public void onCall(Data.EstimateFeeRequest r, FutureCallback<Data.EstimateFeeResponse> cb) {
                estimateFeeMT(r, cb);
            }
        });
    }
    public static Data.EstimateFeeResponse estimateFeeSync(Data.EstimateFeeRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.EstimateFeeRequest, Data.EstimateFeeResponse> () {
            @Override
            public Future<Data.EstimateFeeResponse> onCall(Data.EstimateFeeRequest r) {
                return estimateFeeFuture(r);
            }
        });
    }

    // ======================
    // SendCoins
    public static void sendCoinsMT(Data.SendCoinsRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.SendCoinsRequest req = Codec.encode(r);

        callMT("sendCoins", req, lnrpc.Rpc.SendCoinsResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.SendCoinsResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.sendCoins(data, cb);
            }
        });
    }
    public static Future<Data.SendCoinsResponse> sendCoinsFuture(Data.SendCoinsRequest r) {
        return callFuture(r, new FutureCallImpl<Data.SendCoinsRequest, Data.SendCoinsResponse>() {
            @Override
            public void onCall(Data.SendCoinsRequest r, FutureCallback<Data.SendCoinsResponse> cb) {
                sendCoinsMT(r, cb);
            }
        });
    }
    public static Data.SendCoinsResponse sendCoinsSync(Data.SendCoinsRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.SendCoinsRequest, Data.SendCoinsResponse> () {
            @Override
            public Future<Data.SendCoinsResponse> onCall(Data.SendCoinsRequest r) {
                return sendCoinsFuture(r);
            }
        });
    }

    // ======================
    // SendPayment
    private static class SendStream<RequestType> implements ILightningSendStream<RequestType> {

        private lndmobile.SendStream stream_;

        SendStream(lndmobile.SendStream s){
            assert s != null;
            stream_ = s;
        }

        @Override
        public void send(RequestType r) throws LightningException {
            assert stream_ != null;

            try {
//                Log.i(TAG, "sending class "+r.getClass()+" sendRequest.class "+Data.SendRequest.class+" object "+r);
                // yeah, I know, this is bullshit
                if (r.getClass() == Data.SendRequest.class) {
                    stream_.send(Codec.encode((Data.SendRequest) r).toByteArray());
                } else if (r.getClass() == Data.SendToRouteRequest.class) {
                        stream_.send(Codec.encode((Data.SendToRouteRequest) r).toByteArray());
                } else {
                    Log.e(TAG, "sending unknown class "+r);
                    throw new LightningException(-1, "Unknown request type");
                }
            } catch (LightningException e) {
                Log.e(TAG, "send lightning error "+e);
                throw e;
            } catch (Exception e) {
                Log.e(TAG, "send error "+e);
                throw new LightningException(-1, e.getMessage());
            }
        }

        @Override
        public void stop() throws LightningException {
            try {
                stream_.stop();
            } catch (LightningException e) {
                throw e;
            } catch (Exception e) {
                throw new LightningException(-1, e.getMessage());
            }
        }
    }
    private interface SendImpl<RequestType> {
        lndmobile.SendStream onCall(LndmobileCallback cb) throws Exception;
    }

    private static <RequestType, ResponseType extends com.google.protobuf.Message>
    ILightningSendStream<RequestType> callStreamMT(
            final String label,
            final com.google.protobuf.Parser<ResponseType> parser,
            final ILightningCallbackMT mtcb,
            SendImpl impl) {

        Log.i(TAG, "calling streaming " + label+" thread "+Thread.currentThread().getId());

        try {
            return new SendStream<RequestType>(
                    impl.onCall(new LndmobileCallback<ResponseType>(label, parser, mtcb)));
        } catch (LightningException e) {
            mtcb.onError(e.errorCode(), e.errorMessage());
        } catch (Exception e) {
            mtcb.onError(-1, e.getMessage());
        }
        return null;
    }
    public static ILightningSendStream<Data.SendRequest> sendPaymentMT(final ILightningCallbackMT mtcb) {

        return callStreamMT("sendPayment", lnrpc.Rpc.SendResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.SendResponse)o)); }

        }, new SendImpl() {
            @Override
            public lndmobile.SendStream onCall(LndmobileCallback cb) throws Exception {
                return Lndmobile.sendPayment(cb);
            }
        });
    }

    // ======================
    // SendPayment
    public static void sendPaymentMT(Data.SendRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.SendRequest req = Codec.encode(r);

        callMT("sendPaymentSync", req, lnrpc.Rpc.SendResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.SendResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.sendPaymentSync(data, cb);
            }
        });
    }
    public static Future<Data.SendResponse> sendPaymentFuture(Data.SendRequest r) {
        return callFuture(r, new FutureCallImpl<Data.SendRequest, Data.SendResponse>() {
            @Override
            public void onCall(Data.SendRequest r, FutureCallback<Data.SendResponse> cb) {
                sendPaymentMT(r, cb);
            }
        });
    }
    public static Data.SendResponse sendPaymentSync(Data.SendRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.SendRequest, Data.SendResponse> () {
            @Override
            public Future<Data.SendResponse> onCall(Data.SendRequest r) {
                return sendPaymentFuture(r);
            }
        });
    }

    // ======================
    // LookupInvoice
    public static void lookupInvoiceMT(Data.PaymentHash r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.PaymentHash req = Codec.encode(r);

        callMT("lookupInvoice", req, lnrpc.Rpc.Invoice.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.Invoice)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.lookupInvoice(data, cb);
            }
        });
    }
    public static Future<Data.Invoice> lookupInvoiceFuture(Data.PaymentHash r) {
        return callFuture(r, new FutureCallImpl<Data.PaymentHash, Data.Invoice>() {
            @Override
            public void onCall(Data.PaymentHash r, FutureCallback<Data.Invoice> cb) {
                lookupInvoiceMT(r, cb);
            }
        });
    }
    public static Data.Invoice lookupInvoiceSync(Data.PaymentHash r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.PaymentHash, Data.Invoice> () {
            @Override
            public Future<Data.Invoice> onCall(Data.PaymentHash r) {
                return lookupInvoiceFuture(r);
            }
        });
    }

    // ======================
    // ListPayments
    public static void listPaymentsMT(Data.ListPaymentsRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.ListPaymentsRequest req = Codec.encode(r);

        callMT("listPayments", req, lnrpc.Rpc.ListPaymentsResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.ListPaymentsResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.listPayments(data, cb);
            }
        });
    }
    public static Future<Data.ListPaymentsResponse> listPaymentsFuture(Data.ListPaymentsRequest r) {
        return callFuture(r, new FutureCallImpl<Data.ListPaymentsRequest, Data.ListPaymentsResponse>() {
            @Override
            public void onCall(Data.ListPaymentsRequest r, FutureCallback<Data.ListPaymentsResponse> cb) {
                listPaymentsMT(r, cb);
            }
        });
    }
    public static Data.ListPaymentsResponse listPaymentsSync(Data.ListPaymentsRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.ListPaymentsRequest, Data.ListPaymentsResponse> () {
            @Override
            public Future<Data.ListPaymentsResponse> onCall(Data.ListPaymentsRequest r) {
                return listPaymentsFuture(r);
            }
        });
    }

    // ======================
    // DeleteAllPayments
    public static void deleteAllPaymentsMT(Data.DeleteAllPaymentsRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.DeleteAllPaymentsRequest req = Codec.encode(r);

        callMT("deleteAllPayments", req, lnrpc.Rpc.DeleteAllPaymentsResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.DeleteAllPaymentsResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.deleteAllPayments(data, cb);
            }
        });
    }
    public static Future<Data.DeleteAllPaymentsResponse> deleteAllPaymentsFuture(Data.DeleteAllPaymentsRequest r) {
        return callFuture(r, new FutureCallImpl<Data.DeleteAllPaymentsRequest, Data.DeleteAllPaymentsResponse>() {
            @Override
            public void onCall(Data.DeleteAllPaymentsRequest r, FutureCallback<Data.DeleteAllPaymentsResponse> cb) {
                deleteAllPaymentsMT(r, cb);
            }
        });
    }
    public static Data.DeleteAllPaymentsResponse deleteAllPaymentsSync(Data.DeleteAllPaymentsRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.DeleteAllPaymentsRequest, Data.DeleteAllPaymentsResponse> () {
            @Override
            public Future<Data.DeleteAllPaymentsResponse> onCall(Data.DeleteAllPaymentsRequest r) {
                return deleteAllPaymentsFuture(r);
            }
        });
    }

    // ======================
    // DecodePayReq
    public static void decodePayReqMT(Data.PayReqString r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.PayReqString req = Codec.encode(r);
        callMT("decodePayReq", req, lnrpc.Rpc.PayReq.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.PayReq)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.decodePayReq(data, cb);
            }
        });
    }
    public static Future<Data.PayReq> decodePayReqFuture(Data.PayReqString r) {
        return callFuture(r, new FutureCallImpl<Data.PayReqString, Data.PayReq>() {
            @Override
            public void onCall(Data.PayReqString r, FutureCallback<Data.PayReq> cb) {
                decodePayReqMT(r, cb);
            }
        });
    }
    public static Data.PayReq decodePayReqSync(Data.PayReqString r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.PayReqString, Data.PayReq> () {
            @Override
            public Future<Data.PayReq> onCall(Data.PayReqString r) {
                return decodePayReqFuture(r);
            }
        });
    }

    // ======================
    // RegisterBlockEpochNtfn
/*    public static void registerBlockEpochNtfnMT(Data.BlockEpoch r, final ILightningCallbackMT mtcb) {

        chainrpc.Chainnotifier.BlockEpoch req = Codec.encode(r);
        callMT("registerBlockEpochNtfn", req, chainrpc.Chainnotifier.BlockEpoch.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((chainrpc.Chainnotifier.BlockEpoch)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.registerBlockEpochNtfn(data, cb);
            }
        });
    }
*/
    // ======================
    // SubscribeInvoices
    public static void subscribeInvoicesMT(Data.InvoiceSubscription r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.InvoiceSubscription req = Codec.encode(r);
        callMT("subscribeInvoices", req, lnrpc.Rpc.Invoice.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.Invoice)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.subscribeInvoices(data, cb);
            }
        });
    }

    // ======================
    // SubscribeChannelEvents
    public static void subscribeChannelEventsMT(Data.ChannelEventSubscription r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.ChannelEventSubscription req = Codec.encode(r);
        callMT("subscribeChannelEvents", req, lnrpc.Rpc.ChannelEventUpdate.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.ChannelEventUpdate)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.subscribeChannelEvents(data, cb);
            }
        });
    }

    // ======================
    // GetNodeInfo
    public static void getNodeInfoMT(Data.NodeInfoRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.NodeInfoRequest req = Codec.encode(r);
        callMT("getNodeInfo", req, lnrpc.Rpc.NodeInfo.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.NodeInfo)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.getNodeInfo(data, cb);
            }
        });
    }
    public static Future<Data.NodeInfo> getNodeInfoFuture(Data.NodeInfoRequest r) {
        return callFuture(r, new FutureCallImpl<Data.NodeInfoRequest, Data.NodeInfo>() {
            @Override
            public void onCall(Data.NodeInfoRequest r, FutureCallback<Data.NodeInfo> cb) {
                getNodeInfoMT(r, cb);
            }
        });
    }
    public static Data.NodeInfo getNodeInfoSync(Data.NodeInfoRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.NodeInfoRequest, Data.NodeInfo> () {
            @Override
            public Future<Data.NodeInfo> onCall(Data.NodeInfoRequest r) {
                return getNodeInfoFuture(r);
            }
        });
    }

    // ======================
    // QueryRoutes
    public static void queryRoutesMT(Data.QueryRoutesRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.QueryRoutesRequest req = Codec.encode(r);
        callMT("queryRoutes", req, lnrpc.Rpc.QueryRoutesResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.QueryRoutesResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.queryRoutes(data, cb);
            }
        });
    }
    public static Future<Data.QueryRoutesResponse> queryRoutesFuture(Data.QueryRoutesRequest r) {
        return callFuture(r, new FutureCallImpl<Data.QueryRoutesRequest, Data.QueryRoutesResponse>
                () {
            @Override
            public void onCall(Data.QueryRoutesRequest r, FutureCallback<Data.QueryRoutesResponse> cb) {
                queryRoutesMT(r, cb);
            }
        });
    }
    public static Data.QueryRoutesResponse queryRoutesSync(Data.QueryRoutesRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.QueryRoutesRequest, Data.QueryRoutesResponse> () {
            @Override
            public Future<Data.QueryRoutesResponse> onCall(Data.QueryRoutesRequest r) {
                return queryRoutesFuture(r);
            }
        });
    }

    // ======================
    // SendToRoute
    public static ILightningSendStream<Data.SendToRouteRequest> sendToRouteMT(final ILightningCallbackMT mtcb) {

        return callStreamMT("sendToRoute", lnrpc.Rpc.SendResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.SendResponse)o)); }

        }, new SendImpl() {
            @Override
            public lndmobile.SendStream onCall(LndmobileCallback cb) throws Exception {
                return Lndmobile.sendToRoute(cb);
            }
        });
    }
    public static void sendToRouteMT(Data.SendToRouteRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.SendToRouteRequest req = Codec.encode(r);
        callMT("sendToRouteSync", req, lnrpc.Rpc.SendResponse.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.SendResponse)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.sendToRouteSync(data, cb);
            }
        });
    }
    public static Future<Data.SendResponse> sendToRouteFuture(Data.SendToRouteRequest r) {
        return callFuture(r, new FutureCallImpl<Data.SendToRouteRequest, Data.SendResponse>() {
            @Override
            public void onCall(Data.SendToRouteRequest r, FutureCallback<Data.SendResponse> cb) {
                sendToRouteMT(r, cb);
            }
        });
    }
    public static Data.SendResponse sendToRouteSync(Data.SendToRouteRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.SendToRouteRequest, Data.SendResponse> () {
            @Override
            public Future<Data.SendResponse> onCall(Data.SendToRouteRequest r) {
                return sendToRouteFuture(r);
            }
        });
    }

    // ======================
    // SubscribeChannelBackups
    public static void subscribeChannelBackupsMT(Data.ChannelBackupSubscription r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.ChannelBackupSubscription req = Codec.encode(r);
        callMT("subscribeChannelBackups", req, lnrpc.Rpc.ChanBackupSnapshot.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.ChanBackupSnapshot)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.subscribeChannelBackups(data, cb);
            }
        });
    }

    // ======================
    // ExportAllChannelBackups
    public static void exportAllChannelBackupsMT(Data.ChanBackupExportRequest r, final ILightningCallbackMT mtcb) {

        lnrpc.Rpc.ChanBackupExportRequest req = Codec.encode(r);
        callMT("exportAllChannelBackups", req, lnrpc.Rpc.ChanBackupSnapshot.parser(), new ILightningCallbackMT() {
            @Override
            public void onError(int code, String message) {
                mtcb.onError(code, message);
            }

            @Override
            public void onResponse(Object o) { mtcb.onResponse(Codec.decode((lnrpc.Rpc.ChanBackupSnapshot)o)); }

        }, new CallImpl() {
            @Override
            public void onCall(byte[] data, LndmobileCallback cb) {
                Lndmobile.exportAllChannelBackups(data, cb);
            }
        });
    }
    public static Future<Data.ChanBackupSnapshot> exportAllChannelBackupsFuture(Data.ChanBackupExportRequest r) {
        return callFuture(r, new FutureCallImpl<Data.ChanBackupExportRequest, Data.ChanBackupSnapshot> () {
            @Override
            public void onCall(Data.ChanBackupExportRequest r, FutureCallback<Data.ChanBackupSnapshot> cb) {
                exportAllChannelBackupsMT(r, cb);
            }
        });
    }
    public static Data.ChanBackupSnapshot exportAllChannelBackupsSync(Data.ChanBackupExportRequest r) throws LightningException {

        return callSync(r, new SyncCallImpl<Data.ChanBackupExportRequest, Data.ChanBackupSnapshot> () {
            @Override
            public Future<Data.ChanBackupSnapshot> onCall(Data.ChanBackupExportRequest r) {
                return exportAllChannelBackupsFuture(r);
            }
        });
    }

}