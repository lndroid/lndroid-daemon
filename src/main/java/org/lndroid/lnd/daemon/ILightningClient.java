package org.lndroid.lnd.daemon;

import org.lndroid.lnd.data.Data;

public interface ILightningClient {
    // call when client is supposed to be destroyed
    // to release connections with the daemon
    void onDestroy();

    void unlockWallet(Data.UnlockWalletRequest r, ILightningCallback cb);
    void initWallet(Data.InitWalletRequest r, ILightningCallback cb);
    void genSeed(Data.GenSeedRequest r, ILightningCallback cb);

    void newAddress(Data.NewAddressRequest r, ILightningCallback cb);
    void getInfo(Data.GetInfoRequest r, ILightningCallback cb);
    void walletBalance(Data.WalletBalanceRequest r, ILightningCallback cb);

    void connectPeer(Data.ConnectPeerRequest r, ILightningCallback cb);

    void listChannels(Data.ListChannelsRequest r, ILightningCallback cb);
    void openChannel(Data.OpenChannelRequest r, ILightningCallback cb);
    void openChannelSync(Data.OpenChannelRequest r, ILightningCallback cb);
    void closeChannel(Data.CloseChannelRequest r, ILightningCallback cb);

    void subscribeTransactions(Data.GetTransactionsRequest r, ILightningCallback cb);
    void getTransactions(Data.GetTransactionsRequest r, ILightningCallback cb);
    void estimateFee(Data.EstimateFeeRequest r, ILightningCallback cb);
    void sendCoins(Data.SendCoinsRequest r, ILightningCallback cb);

    void addInvoice(Data.Invoice r, ILightningCallback cb);
    ILightningStream<Data.SendRequest> sendPaymentsStream();
    // overloaded variant for simple cases where cb can be defined before stream is created
    ILightningStream<Data.SendRequest> sendPaymentsStream(ILightningCallback cb);
    // send single payment
    void sendPayment(Data.SendRequest r, ILightningCallback cb);

}
