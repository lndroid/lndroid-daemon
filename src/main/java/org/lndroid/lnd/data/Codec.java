package org.lndroid.lnd.data;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lnrpc.Rpc;

public final class Codec {
    public static lnrpc.Rpc.UnlockWalletRequest encode (Data.UnlockWalletRequest r){
        lnrpc.Rpc.UnlockWalletRequest.Builder b = lnrpc.Rpc.UnlockWalletRequest.newBuilder();
        if (r.walletPassword != null)
            b.setWalletPassword(ByteString.copyFromUtf8(r.walletPassword));
        b.setRecoveryWindow(0);

        return b.build();
    }
    public static Data.UnlockWalletResponse decode (lnrpc.Rpc.UnlockWalletResponse resp) {
        assert resp != null;
        return new Data.UnlockWalletResponse();
    }

    public static lnrpc.Rpc.GenSeedRequest encode (Data.GenSeedRequest r) {
        lnrpc.Rpc.GenSeedRequest.Builder b = lnrpc.Rpc.GenSeedRequest.newBuilder();
        if (r.aezeedPassphrase != null)
            b.setAezeedPassphrase(ByteString.copyFromUtf8(r.aezeedPassphrase));
        if (r.seedEntropy != null)
            b.setSeedEntropy(ByteString.copyFrom(r.seedEntropy));
        return b.build();
    }

    public static Data.GenSeedResponse decode (lnrpc.Rpc.GenSeedResponse resp) {
        assert resp != null;

        // copy results
        Data.GenSeedResponse r = new Data.GenSeedResponse();
        r.encipheredSeed = resp.getEncipheredSeed().toByteArray();
        r.cipherSeedMnemonic = new ArrayList<>();
        for (int i = 0; i < resp.getCipherSeedMnemonicCount(); i++)
            r.cipherSeedMnemonic.add(resp.getCipherSeedMnemonic(i));

        return r;
    }

    public static lnrpc.Rpc.InitWalletRequest encode (Data.InitWalletRequest r) {

        lnrpc.Rpc.InitWalletRequest.Builder b = lnrpc.Rpc.InitWalletRequest.newBuilder();
        if (r.walletPassword != null)
            b.setWalletPassword(ByteString.copyFromUtf8(r.walletPassword));
        if (r.aezeedPassphrase != null)
            b.setAezeedPassphrase(ByteString.copyFromUtf8(r.aezeedPassphrase));
        if (r.cipherSeedMnemonic != null) {
            for (String m : r.cipherSeedMnemonic)
                b.addCipherSeedMnemonic(m);
        }

        return b.build();
    }

    public static Data.InitWalletResponse decode (lnrpc.Rpc.InitWalletResponse resp) {
        return new Data.InitWalletResponse();
    }

    public static lnrpc.Rpc.NewAddressRequest encode (Data.NewAddressRequest r) {
        lnrpc.Rpc.NewAddressRequest.Builder b = lnrpc.Rpc.NewAddressRequest.newBuilder();
        b.setType(Rpc.AddressType.forNumber(r.type));
        return b.build();
    }

    public static Data.NewAddressResponse decode (lnrpc.Rpc.NewAddressResponse resp) {
        assert resp != null;

        // copy results
        Data.NewAddressResponse r = new Data.NewAddressResponse();
        r.address = resp.getAddress();
        return r;
    }

    public static lnrpc.Rpc.GetInfoRequest encode (Data.GetInfoRequest r) {
        return lnrpc.Rpc.GetInfoRequest.newBuilder().build();
    }

    public static Data.GetInfoResponse decode (lnrpc.Rpc.GetInfoResponse resp) {
        assert resp != null;

        // copy results
        Data.GetInfoResponse r = new Data.GetInfoResponse();
        r.identityPubkey = resp.getIdentityPubkey();
        r.alias = resp.getAlias();
        r.numPendingChannels = resp.getNumPendingChannels();
        r.numActiveChannels = resp.getNumActiveChannels();
        r.numPeers = resp.getNumPeers();
        r.blockHeight = resp.getBlockHeight();
        r.blockHash = resp.getBlockHash();
        r.syncedToChain = resp.getSyncedToChain();
        r.uris = new ArrayList<>();
        for (String uri : resp.getUrisList())
            r.uris.add(uri);
        r.bestHeaderTimestamp = resp.getBestHeaderTimestamp();
        r.version = resp.getVersion();
        r.numInactiveChannels = resp.getNumInactiveChannels();
        r.chains = new ArrayList<>();
        for (lnrpc.Rpc.Chain pbc : resp.getChainsList()) {
            Data.Chain c = new Data.Chain();
            c.chain = pbc.getChain();
            c.network = pbc.getNetwork();
            r.chains.add(c);
        }
        r.color = resp.getColor();
        r.syncedToGraph = resp.getSyncedToGraph();

        return r;
    }

    public static lnrpc.Rpc.WalletBalanceRequest encode (Data.WalletBalanceRequest r) {
        return lnrpc.Rpc.WalletBalanceRequest.newBuilder().build();
    }

    public static Data.WalletBalanceResponse decode (lnrpc.Rpc.WalletBalanceResponse resp) {
        assert resp != null;

        // copy results
        Data.WalletBalanceResponse r = new Data.WalletBalanceResponse();
        r.totalBalance = resp.getTotalBalance();
        r.confirmedBalance = resp.getConfirmedBalance();
        r.unconfirmedBalance = resp.getUnconfirmedBalance();

        return r;
    }

    public static lnrpc.Rpc.ConnectPeerRequest encode (Data.ConnectPeerRequest r) {
        lnrpc.Rpc.ConnectPeerRequest.Builder b = lnrpc.Rpc.ConnectPeerRequest.newBuilder();
        if (r.addr != null) {
            lnrpc.Rpc.LightningAddress.Builder ba = lnrpc.Rpc.LightningAddress.newBuilder();
            if (r.addr.host != null)
                ba.setHost(r.addr.host);
            if (r.addr.pubkey != null)
                ba.setPubkey(r.addr.pubkey);
            b.setAddr(ba.build());
        }
        b.setPerm(r.perm);
        return b.build();
    }

    public static Data.ConnectPeerResponse decode (lnrpc.Rpc.ConnectPeerResponse resp) {
        return new Data.ConnectPeerResponse();
    }

    public static lnrpc.Rpc.ListChannelsRequest encode (Data.ListChannelsRequest r) {
        lnrpc.Rpc.ListChannelsRequest.Builder b = lnrpc.Rpc.ListChannelsRequest.newBuilder();
        b.setActiveOnly(r.activeOnly);
        b.setInactiveOnly(r.inactiveOnly);
        b.setPrivateOnly(r.privateOnly);
        b.setPublicOnly(r.publicOnly);
        return b.build();
    }

    public static Data.ListChannelsResponse decode (lnrpc.Rpc.ListChannelsResponse resp) {
        assert resp != null;

        Data.ListChannelsResponse r = new Data.ListChannelsResponse();
        r.channels = new ArrayList<>();
        for (lnrpc.Rpc.Channel pbc : resp.getChannelsList()) {
            Data.Channel c = new Data.Channel();
            c.active = pbc.getActive();
            c.remotePubkey = pbc.getRemotePubkey();
            c.channelPoint = pbc.getChannelPoint();
            c.chanId = pbc.getChanId();
            c.capacity = pbc.getCapacity();
            c.localBalance = pbc.getLocalBalance();
            c.remoteBalance = pbc.getRemoteBalance();
            c.commitFee = pbc.getCommitFee();
            c.commitWeight = pbc.getCommitWeight();
            c.feePerKw = pbc.getFeePerKw();
            c.unsettledBalance = pbc.getUnsettledBalance();
            c.totalSatoshisSent = pbc.getTotalSatoshisSent();
            c.totalSatoshisReceived = pbc.getTotalSatoshisReceived();
            c.numUpdates = pbc.getNumUpdates();
            c.pendingHtlcs = new ArrayList<>();
            for (lnrpc.Rpc.HTLC pbHtlc : pbc.getPendingHtlcsList()) {
                Data.HTLC htlc = new Data.HTLC();
                htlc.amount = pbHtlc.getAmount();
                htlc.expirationHeight = pbHtlc.getExpirationHeight();
                htlc.hashLock = pbHtlc.getHashLock().toByteArray();
                htlc.incoming = pbHtlc.getIncoming();
                c.pendingHtlcs.add(htlc);
            }
            c.csvDelay = pbc.getCsvDelay();
            c.isPrivate = pbc.getPrivate();
            c.initiator = pbc.getInitiator();
            c.chanStatusFlags = pbc.getChanStatusFlags();
            c.localChanReserveSat = pbc.getLocalChanReserveSat();
            c.remoteChanReserveSat = pbc.getRemoteChanReserveSat();
            c.staticRemoteKey = pbc.getStaticRemoteKey();
            c.lifetime = pbc.getLifetime();
            c.uptime = pbc.getUptime();

            r.channels.add(c);
        }

        return r;
    }

    public static lnrpc.Rpc.HopHint encode (Data.HopHint r) {
        lnrpc.Rpc.HopHint.Builder b = lnrpc.Rpc.HopHint.newBuilder();
        b.setNodeId(r.nodeId);
        b.setChanId(r.chanId);
        b.setFeeBaseMsat(r.feeBaseMsat);
        b.setFeeProportionalMillionths(r.feeProportionalMillionths);
        b.setCltvExpiryDelta(r.cltvExpiryDelta);
        return b.build();
    }

    public static lnrpc.Rpc.RouteHint encode (Data.RouteHint r) {
        lnrpc.Rpc.RouteHint.Builder b = lnrpc.Rpc.RouteHint.newBuilder();
        for (Data.HopHint hh: r.hopHints) {
            b.addHopHints(encode(hh));
        }
        return b.build();
    }

    public static lnrpc.Rpc.InvoiceHTLC encode (Data.InvoiceHTLC r) {
        lnrpc.Rpc.InvoiceHTLC.Builder b = lnrpc.Rpc.InvoiceHTLC.newBuilder();
        b.setChanId(r.chanId);
        b.setHtlcIndex(r.htlcIndex);
        b.setAmtMsat(r.amtMsat);
        b.setAcceptHeight(r.acceptHeight);
        b.setAcceptTime(r.acceptTime);
        b.setResolveTime(r.resolveTime);
        b.setExpiryHeight(r.expiryHeight);
        b.setState(lnrpc.Rpc.InvoiceHTLCState.forNumber(r.state));
        return b.build();
    }

    public static lnrpc.Rpc.Invoice encode (Data.Invoice r) {
        lnrpc.Rpc.Invoice.Builder b = lnrpc.Rpc.Invoice.newBuilder();
        if (r.memo != null)
            b.setMemo(r.memo);
        if (r.rPreimage != null)
            b.setRPreimage(ByteString.copyFrom(r.rPreimage));
        if (r.rHash != null)
            b.setRHash(ByteString.copyFrom(r.rHash));
        b.setValue(r.value);
        b.setCreationDate(r.creationDate);
        b.setSettleDate(r.settleDate);
        if (r.paymentRequest != null)
            b.setPaymentRequest(r.paymentRequest);
        if (r.descriptionHash != null)
            b.setDescriptionHash(ByteString.copyFrom(r.descriptionHash));
        b.setExpiry(r.expiry);
        if (r.fallbackAddr != null)
            b.setFallbackAddr(r.fallbackAddr);
        b.setCltvExpiry(r.cltvExpiry);
        if (r.routeHints != null) {
            for (Data.RouteHint rh : r.routeHints) {
                b.addRouteHints(encode (rh));
            }
        }
        b.setPrivate(r.isPrivate);
        b.setAddIndex(r.addIndex);
        b.setSettleIndex(r.settleIndex);
        b.setAmtPaidSat(r.amtPaidSat);
        b.setAmtPaidMsat(r.amtPaidMsat);
        b.setState(lnrpc.Rpc.Invoice.InvoiceState.forNumber(r.state));
        if (r.htlcs != null) {
            for (Data.InvoiceHTLC htlc : r.htlcs) {
                b.addHtlcs(encode(htlc));
            }
        }

        return b.build();
    }

    public static Data.AddInvoiceResponse decode (lnrpc.Rpc.AddInvoiceResponse resp) {
        assert resp != null;

        Data.AddInvoiceResponse r = new Data.AddInvoiceResponse();
        r.rHash = resp.getRHash().toByteArray();
        r.paymentRequest = resp.getPaymentRequest();
        r.addIndex = resp.getAddIndex();

        return r;
    }

    public static lnrpc.Rpc.GetTransactionsRequest encode (Data.GetTransactionsRequest r) {
        return lnrpc.Rpc.GetTransactionsRequest.newBuilder().build();
    }

    public static Data.Transaction decode (lnrpc.Rpc.Transaction resp) {
        assert resp != null;

        Data.Transaction r = new Data.Transaction();
        r.txHash = resp.getTxHash();
        r.amount = resp.getAmount();
        r.numConfirmations = resp.getNumConfirmations();
        r.blockHash = resp.getBlockHash();
        r.blockHeight = resp.getBlockHeight();
        r.timeStamp = resp.getTimeStamp();
        r.totalFees = resp.getTotalFees();
        r.destAddresses = new ArrayList<>();
        for(String da: resp.getDestAddressesList())
            r.destAddresses.add(da);
        r.rawTxHex = resp.getRawTxHex();
        return r;
    }

    public static Data.TransactionDetails decode (lnrpc.Rpc.TransactionDetails resp) {
        assert resp != null;

        Data.TransactionDetails r = new Data.TransactionDetails();
        r.transactions = new ArrayList<>();
        for(lnrpc.Rpc.Transaction t: resp.getTransactionsList())
            r.transactions.add(decode(t));

        return r;
    }

    public static lnrpc.Rpc.OpenChannelRequest encode (Data.OpenChannelRequest r) {
        lnrpc.Rpc.OpenChannelRequest.Builder b = lnrpc.Rpc.OpenChannelRequest.newBuilder();

        if (r.nodePubkey != null)
            b.setNodePubkey(ByteString.copyFrom(r.nodePubkey));
        if (r.nodePubkeyString != null)
            b.setNodePubkeyString(r.nodePubkeyString);
        b.setLocalFundingAmount(r.localFundingAmount);
        b.setPushSat(r.pushSat);
        b.setTargetConf(r.targetConf);
        b.setSatPerByte(r.satPerByte);
        b.setPrivate(r.isPrivate);
        b.setMinHtlcMsat(r.minHtlcMsat);
        b.setRemoteCsvDelay(r.remoteCsvDelay);
        b.setMinConfs(r.minConfs);
        b.setSpendUnconfirmed(r.spendUnconfirmed);

        return b.build();
    }
    public static lnrpc.Rpc.ChannelPoint encode (Data.ChannelPoint r) {
        lnrpc.Rpc.ChannelPoint.Builder b = lnrpc.Rpc.ChannelPoint.newBuilder();

        if (r.fundingTxidBytes != null)
            b.setFundingTxidBytes(ByteString.copyFrom(r.fundingTxidBytes));
        if (r.fundingTxidStr != null)
            b.setFundingTxidStr(r.fundingTxidStr);
        b.setOutputIndex(r.outputIndex);
        return b.build();
    }
    public static Data.ChannelPoint decode (lnrpc.Rpc.ChannelPoint resp) {
        assert resp != null;

        Data.ChannelPoint r = new Data.ChannelPoint();
        r.fundingTxidBytes = resp.getFundingTxidBytes().toByteArray();
        r.fundingTxidStr = resp.getFundingTxidStr();
        r.outputIndex = resp.getOutputIndex();
        return r;
    }
    public static Data.PendingUpdate decode (lnrpc.Rpc.PendingUpdate resp) {
        assert resp != null;

        Data.PendingUpdate r = new Data.PendingUpdate();
        r.txid = resp.getTxid().toByteArray();
        r.outputIndex = resp.getOutputIndex();
        return r;
    }
    public static Data.ChannelOpenUpdate decode (lnrpc.Rpc.ChannelOpenUpdate resp) {
        assert resp != null;

        Data.ChannelOpenUpdate r = new Data.ChannelOpenUpdate();
        r.channelPoint = decode(resp.getChannelPoint());
        return r;
    }
    public static Data.OpenStatusUpdate decode (lnrpc.Rpc.OpenStatusUpdate resp) {
        assert resp != null;

        Data.OpenStatusUpdate r = new Data.OpenStatusUpdate();
        r.chanPending = decode(resp.getChanPending());
        r.chanOpen = decode(resp.getChanOpen());
        return r;
    }

    public static lnrpc.Rpc.CloseChannelRequest encode (Data.CloseChannelRequest r) {
        lnrpc.Rpc.CloseChannelRequest.Builder b = lnrpc.Rpc.CloseChannelRequest.newBuilder();
        if (r.channelPoint != null) {
            b.setChannelPoint(encode(r.channelPoint));
        }
        b.setForce(r.force);
        b.setTargetConf(r.targetConf);
        b.setSatPerByte(r.satPerByte);

        return b.build();
    }
    public static Data.ChannelCloseUpdate decode (lnrpc.Rpc.ChannelCloseUpdate resp) {
        assert resp != null;

        Data.ChannelCloseUpdate r = new Data.ChannelCloseUpdate();
        r.closingTxid = resp.getClosingTxid().toByteArray();
        r.success = resp.getSuccess();
        return r;
    }
    public static Data.CloseStatusUpdate decode (lnrpc.Rpc.CloseStatusUpdate resp) {
        assert resp != null;

        Data.CloseStatusUpdate r = new Data.CloseStatusUpdate();
        r.closePending = decode(resp.getClosePending());
        r.chanClose = decode(resp.getChanClose());
        return r;
    }


    public static lnrpc.Rpc.EstimateFeeRequest encode (Data.EstimateFeeRequest r) {
        lnrpc.Rpc.EstimateFeeRequest.Builder b = lnrpc.Rpc.EstimateFeeRequest.newBuilder();

        if (r.addrToAmount != null) {
            for(Map.Entry<String,Long> e: r.addrToAmount.entrySet())
                b.getAddrToAmountMap().put(e.getKey(), e.getValue());
        }
        b.setTargetConf(r.targetConf);

        return b.build();
    }
    public static Data.EstimateFeeResponse decode (lnrpc.Rpc.EstimateFeeResponse resp) {
        assert resp != null;

        Data.EstimateFeeResponse r = new Data.EstimateFeeResponse();
        r.feeSat = resp.getFeeSat();
        r.feerateSatPerByte = resp.getFeerateSatPerByte();
        return r;
    }

    public static lnrpc.Rpc.SendCoinsRequest encode (Data.SendCoinsRequest r) {
        lnrpc.Rpc.SendCoinsRequest.Builder b = lnrpc.Rpc.SendCoinsRequest.newBuilder();

        if (r.addr != null)
            b.setAddr(r.addr);
        b.setAmount(r.amount);
        b.setTargetConf(r.targetConf);
        b.setSatPerByte(r.satPerByte);
        b.setSendAll(r.sendAll);

        return b.build();
    }
    public static Data.SendCoinsResponse decode (lnrpc.Rpc.SendCoinsResponse resp) {
        assert resp != null;

        Data.SendCoinsResponse r = new Data.SendCoinsResponse();
        r.txid = resp.getTxid();
        return r;
    }

    public static lnrpc.Rpc.FeeLimit encode(Data.FeeLimit r) {
        lnrpc.Rpc.FeeLimit.Builder b = lnrpc.Rpc.FeeLimit.newBuilder();
        b.setFixed(r.fixed);
        b.setPercent(r.percent);
        return b.build();
    }
    public static lnrpc.Rpc.SendRequest encode(Data.SendRequest r) {
        lnrpc.Rpc.SendRequest.Builder b = lnrpc.Rpc.SendRequest.newBuilder();
        if (r.dest != null)
            b.setDest(ByteString.copyFrom(r.dest));
        if (r.destString != null)
            b.setDestString(r.destString);
        b.setAmt(r.amt);
        if (r.paymentHash != null)
            b.setPaymentHash(ByteString.copyFrom(r.paymentHash));
        if (r.paymentHashString != null)
            b.setPaymentHashString(r.paymentHashString);
        if (r.paymentRequest != null)
            b.setPaymentRequest(r.paymentRequest);
        b.setFinalCltvDelta(r.finalCltvDelta);
        if (r.feeLimit != null)
            b.setFeeLimit(encode(r.feeLimit));
        b.setOutgoingChanId(r.outgoingChanId);
        b.setCltvLimit(r.cltvLimit);
        if (r.destTlv != null) {
            for(Map.Entry<Long, byte[]> e: r.destTlv.entrySet()) {
                if (e.getValue() != null)
                    b.getDestTlvMap().put(e.getKey(), ByteString.copyFrom(e.getValue()));
                else
                    b.getDestTlvMap().put(e.getKey(), null); // FIXME god knows if it works
            }
        }

        return b.build();
    }
    public static Data.MPPRecord decode (lnrpc.Rpc.MPPRecord resp) {
        assert resp != null;

        Data.MPPRecord r = new Data.MPPRecord();
        r.paymentAddr = resp.getPaymentAddr().toByteArray();
        r.totalAmtMsat = resp.getTotalAmtMsat();
        return r;
    }
    public static Data.Hop decode (lnrpc.Rpc.Hop resp) {
        assert resp != null;

        Data.Hop r = new Data.Hop();
        r.chanId = resp.getChanId();
        r.chanCapacity = resp.getChanCapacity();
        r.expiry = resp.getExpiry();
        r.amtToForwardMsat = resp.getAmtToForwardMsat();
        r.feeMsat = resp.getFeeMsat();
        r.pubKey = resp.getPubKey();
        r.tlvPayload = resp.getTlvPayload();
        r.mppRecord = decode(resp.getMppRecord());
        return r;
    }
    public static Data.Route decode (lnrpc.Rpc.Route resp) {
        assert resp != null;

        Data.Route r = new Data.Route();
        r.totalTimeLock = resp.getTotalTimeLock();
        r.hops = new ArrayList<>();
        for(lnrpc.Rpc.Hop h: resp.getHopsList())
            r.hops.add(decode(h));
        r.totalFeesMsat = resp.getTotalFeesMsat();
        r.totalAmtMsat = resp.getTotalAmtMsat();
        return r;
    }
    public static Data.SendResponse decode (lnrpc.Rpc.SendResponse resp) {
        assert resp != null;

        Data.SendResponse r = new Data.SendResponse();
        r.paymentError = resp.getPaymentError();
        r.paymentPreimage = resp.getPaymentPreimage().toByteArray();
        r.paymentRoute = decode(resp.getPaymentRoute());
        r.paymentHash = resp.getPaymentHash().toByteArray();
        return r;
    }

    public static lnrpc.Rpc.PaymentHash encode(Data.PaymentHash r) {
        lnrpc.Rpc.PaymentHash.Builder b = lnrpc.Rpc.PaymentHash.newBuilder();
        if (r.rHashStr != null)
            b.setRHashStr(r.rHashStr);
        if (r.rHash != null)
            b.setRHash(ByteString.copyFrom(r.rHash));
        return b.build();
    }

    public static Data.HopHint decode (lnrpc.Rpc.HopHint resp) {
        assert resp != null;

        Data.HopHint r = new Data.HopHint();
        r.nodeId = resp.getNodeId();
        r.chanId = resp.getChanId();
        r.feeBaseMsat = resp.getFeeBaseMsat();
        r.feeProportionalMillionths = resp.getFeeProportionalMillionths();
        r.cltvExpiryDelta = resp.getCltvExpiryDelta();
        return r;
    }

    public static Data.RouteHint decode (lnrpc.Rpc.RouteHint resp) {
        assert resp != null;

        Data.RouteHint r = new Data.RouteHint();
        r.hopHints = new ArrayList<>();
        for (lnrpc.Rpc.HopHint hh: resp.getHopHintsList())
            r.hopHints.add(decode(hh));
        return r;
    }

    public static Data.InvoiceHTLC decode (lnrpc.Rpc.InvoiceHTLC resp) {
        assert resp != null;

        Data.InvoiceHTLC r = new Data.InvoiceHTLC();
        r.chanId = resp.getChanId();
        r.htlcIndex = resp.getHtlcIndex();
        r.amtMsat = resp.getAmtMsat();
        r.acceptHeight = resp.getAcceptHeight();
        r.acceptTime = resp.getAcceptTime();
        r.resolveTime = resp.getResolveTime();
        r.expiryHeight = resp.getExpiryHeight();
        r.state = resp.getState().getNumber();
        return r;
    }
    public static Data.Invoice decode (lnrpc.Rpc.Invoice resp) {
        assert resp != null;

        Data.Invoice r = new Data.Invoice();
        r.memo = resp.getMemo();
        r.rPreimage = resp.getRPreimage().toByteArray();
        r.rHash = resp.getRHash().toByteArray();
        r.value = resp.getValue();
        r.creationDate = resp.getCreationDate();
        r.settleDate = resp.getSettleDate();
        r.paymentRequest = resp.getPaymentRequest();
        r.descriptionHash = resp.getDescriptionHash().toByteArray();
        r.expiry = resp.getExpiry();
        r.fallbackAddr = resp.getFallbackAddr();
        r.cltvExpiry = resp.getCltvExpiry();
        r.routeHints = new ArrayList<>();
        for (lnrpc.Rpc.RouteHint rh : resp.getRouteHintsList())
            r.routeHints.add(decode(rh));
        r.isPrivate = resp.getPrivate();
        r.addIndex = resp.getAddIndex();
        r.settleIndex = resp.getSettleIndex();
        r.amtPaidSat = resp.getAmtPaidSat();
        r.amtPaidMsat = resp.getAmtPaidMsat();
        r.state = resp.getState().getNumber();
        r.htlcs = new ArrayList<>();
        for (lnrpc.Rpc.InvoiceHTLC htlc: resp.getHtlcsList())
            r.htlcs.add(decode(htlc));

        return r;
    }

    public static lnrpc.Rpc.ListPaymentsRequest encode(Data.ListPaymentsRequest r) {
        lnrpc.Rpc.ListPaymentsRequest.Builder b = lnrpc.Rpc.ListPaymentsRequest.newBuilder();
        b.setIncludeIncomplete(r.includeIncomplete);
        return b.build();
    }
    public static Data.Payment decode (lnrpc.Rpc.Payment resp) {
        assert resp != null;

        Data.Payment r = new Data.Payment();
        r.paymentHash = resp.getPaymentHash();
        r.creationDate = resp.getCreationDate();
        r.path = new ArrayList<>();
        for (String p: resp.getPathList())
            r.path.add(p);
        r.paymentPreimage = resp.getPaymentPreimage();
        r.valueSat = resp.getValueSat();
        r.valueMsat = resp.getValueMsat();
        r.paymentRequest = resp.getPaymentRequest();
        r.status = resp.getStatus().getNumber();
        r.feeSat = resp.getFeeSat();
        r.feeMsat = resp.getFeeMsat();

        return r;
    }
    public static Data.ListPaymentsResponse decode (lnrpc.Rpc.ListPaymentsResponse resp) {
        assert resp != null;
        Data.ListPaymentsResponse r = new Data.ListPaymentsResponse();
        r.payments = new ArrayList<>();
        for(lnrpc.Rpc.Payment p: resp.getPaymentsList())
            r.payments.add(decode(p));

        return r;
    }

    public static lnrpc.Rpc.PayReqString encode(Data.PayReqString r) {
        lnrpc.Rpc.PayReqString.Builder b = lnrpc.Rpc.PayReqString.newBuilder();
        if (r.payReq != null)
            b.setPayReq(r.payReq);
        return b.build();
    }
    public static Data.PayReq decode (lnrpc.Rpc.PayReq resp) {
        assert resp != null;
        Data.PayReq r = new Data.PayReq();
        r.destination = resp.getDestination();
        r.paymentHash = resp.getPaymentHash();
        r.numSatoshis = resp.getNumSatoshis();
        r.timestamp = resp.getTimestamp();
        r.expiry = resp.getExpiry();
        r.description = resp.getDescription();
        r.descriptionHash = resp.getDescriptionHash();
        r.fallbackAddr = resp.getFallbackAddr();
        r.cltvExpiry = resp.getCltvExpiry();
        r.routeHints = new ArrayList<>();
        for(lnrpc.Rpc.RouteHint rh: resp.getRouteHintsList())
            r.routeHints.add(decode(rh));

        return r;
    }
}
