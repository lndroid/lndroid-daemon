Lndroid.Daemon library
======================

Lndroid.Daemon is a wrapper around [Lightning Network Daemon (lnd)](https://github.com/lightningnetwork/lnd). It makes using **lnd** in native android apps easier.

Lnd mobile API requires a lot or work:
1. All API calls are asynchronous, with result callbacks called from another thread. Which means that for the callback code you provide to interact with your UI thread, your code must be thread-safe. 
2. All API calls accept byte[] and return byte[], which are serialized protobuf objects. Your code must do the job of encoding and decoding them.
3. Interacting with the API from a UI thread requires care, as the lnd daemon is a single global object per process. If a callback is created in a UI thread and holds referenes to UI objects, then the daemon might hold those references for long time, even after UI objects have been closed by user and subject to GC. 

Lndroid.Daemon provides:

1. Several variants to access every API method: Handler-based - for UI threads, synchronous - for worker threads that are OK with blocking, future-based - to allow for some parallelism, and "multi-threaded" - for those who know what they're doing.
2. POJO classes for all lnd data types, to free clients from messing with protobuf.
3. Careful architecture, making memory leaks and threading issues far less likely.

Example usage:

```
import org.lndroid.lnd.daemon.LightningDaemon;
import org.lndroid.lnd.daemon.LightningException;
import org.lndroid.lnd.daemon.ILightningClient;
import org.lndroid.lnd.daemon.ILightningCallback;
import org.lndroid.lnd.daemon.ILightningStream;
import org.lndroid.lnd.data.Data;

public class MainActivity extends AppCompatActivity {

    .....

    // create a client object, responsible for dispatching
    // callbacks through this UI thread's Looper
    private ILightningClient client_ = LightningDaemon.createClient();

    private void startDaemon(){
        File file = this.getApplicationContext().getFilesDir();
        Log.i(TAG, "app dir " + file.getAbsoluteFile());

        LightningDaemon.Init init = new LightningDaemon.Init();
        init.dir = file.getAbsoluteFile() + "/.lnd";
        init.debugLevel = "debug";

        try {
            // Ensure the Daemon is started, does nothing
            // if it's already been called within current process.
            LightningDaemon.start(init);
        } catch (LightningException e) {
            Log.e(TAG, "start failed " + e.errorMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	
	.........

        // Make sure the daemon is started
        startDaemon();
    }

    @Override
    protected void onDestroy() {
        // Make sure client closes all send-streams to
        // release resources of the daemon
        client_.onDestroy();
        super.onDestroy();
    }

    private void initWallet(String password, List<String> cipherSeedMnemonic) {
        Data.InitWalletRequest req = new Data.InitWalletRequest();
        req.cipherSeedMnemonic = cipherSeedMnemonic;
        req.walletPassword = password;

        // simple API call, callback will be called once,
        // or never if this Activity is closed by user
        client_.initWallet(req, new ILightningCallback<Data.InitWalletResponse>() {
            @Override
            public void onResponse(Data.InitWalletResponse rep) {
                // accessing UI components is OK, as this is executed
                // in UI thread
                text_.setText("init wallet ok");
            }

            @Override
            public void onError(int code, String message) {
                text_.setText("init wallet error " + code +" error "+message);
            }
        });
    }

    private void subscribeTransactions() {
        Data.GetTransactionsRequest req = new Data.GetTransactionsRequest();

        // uni-directional streaming call,
        // callback will be called every time a new transaction is received,
        // until an error is returned
        client_.subscribeTransactionsStream(req, new ILightningCallback<Data.Transaction>() {
            @Override
            public void onResponse(Data.Transaction rep) {
                Log.i(TAG, "sub transactions result "+rep);
            }

            @Override
            public void onError(int i, String s) {
                Log.e(TAG, "sub transactions error, resubscribe! "+i+" str "+s);
            }
        });
    }

    private void getInfoSync() {
        Data.GetInfoRequest req = new Data.GetInfoRequest();
        // synchronous call variant, DON'T do it in UI thread,
        // sync calls don't require a 'client' as current thread is blocked 
        Data.GetInfoResponse rep = LightningDaemon.getInfoSync(req);
        Log.i(TAG, "info.pubkey "+rep.identityPubkey);
    }

    private void getInfoFuture() {
        Data.GetInfoRequest req = new Data.GetInfoRequest();
        // future-based call variant, still not very good for UI threads,
        // future calls don't require a 'client' too, as future value
        // is set in daemon thread and retrieved safely in current thread 
        Future<Data.GetInfoResponse> f = LightningDaemon.getInfoFuture(req);
        // maybe do some work here
        f.wait(); // FIXME handle interrupts
        Data.GetInfoResponse rep = f.get();
        Log.i(TAG, "info.pubkey "+rep.identityPubkey);
    }

    private void getInfoMT() {
        Data.GetInfoRequest req = new Data.GetInfoRequest();
        // MT-way: callback is executed in another thread,
        // make sure you understand what you're doing
        LightningDaemon.getInfoMT(req, new ILightningCallbackMT() {
	    
            @Override
            public void onResponse(Object o) {
                // we're not in UI thread now!
                Data.GetInfoResponse rep = (Data.GetInfoResponse)object;
                Log.i(TAG, "info.pubkey "+rep.identityPubkey);
            }
	    
            @Override
            public void onError(int code, String message) {
                Log.e(TAG, "info error "+code+" message "+message);
            }
        });
    }

    private void sendPaymentsStream() {
        Data.SendRequest req1 = new Data.SendRequest();
        req1.paymentRequest = "...";
        Data.SendRequest req2 = new Data.SendRequest();
        req2.paymentRequest = "...";

        // create bi-directional stream to pipeline
        // payment requests and replies
        ILightningStream<Data.SendRequest, Data.SendResponse> stream =
                client_.sendPaymentsStream(new ILightningCallback<Data.SendResponse>() {
                    @Override
                    public void onResponse(Data.SendResponse rep) {
                        Log.i(TAG, "send payment result " + rep);
                    }

                    @Override
                    public void onError(int code, String s) {
                        if ("EOF".equals(s))
                            Log.i(TAG, "send payment done " + code + " err " + s);
                        else
                            Log.i(TAG, "send payment error " + code + " err " + s);
                    }
        });

        try {
            // send several requests
            stream.send(req1);
            stream.send(req2);
            // tell stream that it should terminate when
            // replies to all requests are received
            stream.done();
        } catch (LightningException e) {
            Log.e(TAG, "send payment send failed "+e.getMessage());
        }
    }
}
```

# Dependencies

To compile lndroid-daemon library with Android Studio:
1. Lnd [compiled for android](https://github.com/lightningnetwork/lnd/tree/master/mobile), .AAR placed at Lndmobile dir.

To use lndroid-daemon in your project:
1. minSdkVersion 16 or higher
2. Lndmobile.aar added as a module to the Android Studio project.
3. lndroid-daemon.aar added as a module.
4. Protobuf libraries as dependencies:
   - implementation 'com.google.protobuf:protobuf-java:3.4.0'
   - implementation 'com.google.api.grpc:proto-google-common-protos:1.12.0'
5. Multidex enabled (lnd protobuf rpc generates a lot of methods):
   - implementation 'androidx.multidex:multidex:2.0.1'
   - android { defaultConfig { multiDexEnabled true } }
   - <application android:name="androidx.multidex.MultiDexApplication" in manifest
   - read more here https://developer.android.com/studio/build/multidex
6. Permission ```<uses-permission android:name="android.permission.INTERNET" /> ``` 

# TODO

Not all API methods are implemented, simply because the author didn't bother yet. Adding absent methods is rather trivial.

Unit tests are absent.

A UI to explore the API is absent too.

Your contributions are very welcome.

# License

MIT

# Author

Artur Brugeman, brugeman.artur@gmail.com
