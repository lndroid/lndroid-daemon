package org.lndroid.lnd.daemon;

import org.lndroid.lnd.data.Data;

public interface ILightningClient {
    // call when client is supposed to be destroyed
    // to release connections with the daemon
    void onDestroy();

    void unlockWallet(Data.UnlockWalletRequest r, ILightningCallback<Data.UnlockWalletResponse> cb);
    void initWallet(Data.InitWalletRequest r, ILightningCallback<Data.InitWalletResponse> cb);
    void genSeed(Data.GenSeedRequest r, ILightningCallback<Data.GenSeedResponse> cb);

    void newAddress(Data.NewAddressRequest r, ILightningCallback<Data.NewAddressResponse> cb);
    void getInfo(Data.GetInfoRequest r, ILightningCallback<Data.GetInfoResponse> cb);
    void walletBalance(Data.WalletBalanceRequest r, ILightningCallback<Data.WalletBalanceResponse> cb);
    void channelBalance(Data.ChannelBalanceRequest r, ILightningCallback<Data.ChannelBalanceResponse> cb);

    void connectPeer(Data.ConnectPeerRequest r, ILightningCallback<Data.ConnectPeerResponse> cb);

    void listChannels(Data.ListChannelsRequest r, ILightningCallback<Data.ListChannelsResponse> cb);
    void openChannelStream(Data.OpenChannelRequest r, ILightningCallback<Data.OpenStatusUpdate> cb);
    void openChannel(Data.OpenChannelRequest r, ILightningCallback<Data.ChannelPoint> cb);
    void closeChannelStream(Data.CloseChannelRequest r, ILightningCallback<Data.CloseStatusUpdate> cb);

    void subscribeTransactionsStream(Data.GetTransactionsRequest r, ILightningCallback<Data.Transaction> cb);
    void getTransactions(Data.GetTransactionsRequest r, ILightningCallback<Data.TransactionDetails> cb);
    void estimateFee(Data.EstimateFeeRequest r, ILightningCallback<Data.EstimateFeeResponse> cb);
    void sendCoins(Data.SendCoinsRequest r, ILightningCallback<Data.SendCoinsResponse> cb);

    void addInvoice(Data.Invoice r, ILightningCallback<Data.AddInvoiceResponse> cb);
    ILightningStream<Data.SendRequest, Data.SendResponse> sendPaymentsStream();
    // overloaded variant for simple cases where cb can be defined before stream is created
    ILightningStream<Data.SendRequest, Data.SendResponse> sendPaymentsStream(
            ILightningCallback<Data.SendResponse> cb);
    // send single payment
    void sendPayment(Data.SendRequest r, ILightningCallback<Data.SendResponse> cb);

}
