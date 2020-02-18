package org.lndroid.lnd.data;

import java.util.List;
import java.util.Map;

public final class Data
{
    // unlockWallet
    public static final class UnlockWalletRequest {
        public byte[] walletPassword;
    }
    public static final class UnlockWalletResponse {
    }

    // initWallet
    public static final class InitWalletRequest {
        public byte[] walletPassword;
        public List<String> cipherSeedMnemonic;
        public byte[] aezeedPassphrase;
        public ChanBackupSnapshot chanBackups;
    }
    public static final class InitWalletResponse {
    }

    // genSeed
    public static final class GenSeedRequest {
        public byte[] aezeedPassphrase;
        public byte[] seedEntropy;
    }
    public static final class GenSeedResponse {
        public List<String> cipherSeedMnemonic;
        public byte[] encipheredSeed;
    }

    // newAddress
    public static final int ADDRESS_TYPE_WITNESS_PUBKEY_HASH = 0;
    public static final int ADDRESS_TYPE_NESTED_PUBKEY_HASH = 1;
    public static final int ADDRESS_TYPE_UNUSED_WITNESS_PUBKEY_HASH = 2;
    public static final int ADDRESS_TYPE_UNUSED_NESTED_PUBKEY_HASH = 3;
    public static final class NewAddressRequest {
        public int type = ADDRESS_TYPE_WITNESS_PUBKEY_HASH;
    }
    public static final class NewAddressResponse {
        public String address;
    }

    // getInfo
    public static final class GetInfoRequest {
    }
    public static final class Chain {
        /// The blockchain the node is on (eg bitcoin, litecoin)
        public String chain;

        /// The network the node is on (eg regtest, testnet, mainnet)
        public String network;
    }
    public static final class GetInfoResponse {
        /// The identity pubkey of the current node.
        public String identityPubkey;

        /// If applicable, the alias of the current node, e.g. "bob"
        public String alias;

        /// Number of pending channels
        public int numPendingChannels;

        /// Number of active channels
        public int numActiveChannels;

        /// Number of peers
        public int numPeers;

        /// The node's current view of the height of the best block
        public int blockHeight;

        /// The node's current view of the hash of the best block
        public String blockHash;

        /// Whether the wallet's view is synced to the main chain
        public boolean syncedToChain;

        /// The URIs of the current node.
        public List<String> uris;

        /// Timestamp of the block best known to the wallet
        public long bestHeaderTimestamp;

        /// The version of the LND software that the node is running.
        public String version;

        /// Number of inactive channels
        public int numInactiveChannels;

        /// A list of active chains the node is connected to
        public List<Chain> chains;

        /// The color of the current node in hex code format
        public String color;

        // Whether we consider ourselves synced with the public channel graph.
        public boolean syncedToGraph;
    }

    // ================
    // WalletBalance
    public static final class WalletBalanceRequest {
    }
    public static final class WalletBalanceResponse {
        public long totalBalance;

        /// The confirmed balance of a wallet(with >= 1 confirmations)
        public long confirmedBalance;

        /// The unconfirmed balance of a wallet(with 0 confirmations)
        public long unconfirmedBalance;
    }

    // ====================
    // ConnectPeer
    public static final class LightningAddress{
        /// The identity pubkey of the Lightning node
        public String pubkey;

        /// The network location of the lightning node, e.g. `69.69.69.69:1337` or `localhost:10011`
        public String host;
    }
    public static final class ConnectPeerRequest {
        /// Lightning address of the peer, in the format `<pubkey>@host`
        public LightningAddress addr;

        /** If set, the daemon will attempt to persistently connect to the target
         * peer.  Otherwise, the call will be synchronous. */
        public boolean perm;
    }
    public static final class ConnectPeerResponse {
    }

    // =================
    // ListChannels

    public static final class HTLC {
        public boolean incoming;
        public long amount;
        public byte[] hashLock;
        public int expirationHeight;
    }
    public static final class Channel {
        /// Whether this channel is active or not
        public boolean active;

        /// The identity pubkey of the remote node
        public String remotePubkey;

        /**
         The outpoint (txid:index) of the funding transaction. With this value, Bob
         will be able to generate a signature for Alice's version of the commitment
         transaction.
         */
        public String channelPoint;

        /**
         The unique channel ID for the channel. The first 3 bytes are the block
         height, the next 3 the index within the block, and the last 2 bytes are the
         output index for the channel.
         */
        public long chanId;

        /// The total amount of funds held in this channel
        public long capacity;

        /// This node's current balance in this channel
        public long localBalance;

        /// The counterparty's current balance in this channel
        public long remoteBalance;

        /**
         The amount calculated to be paid in fees for the current set of commitment
         transactions. The fee amount is persisted with the channel in order to
         allow the fee amount to be removed and recalculated with each channel state
         update, including updates that happen after a system restart.
         */
        public long commitFee;

        /// The weight of the commitment transaction
        public long commitWeight;

        /**
         The required number of satoshis per kilo-weight that the requester will pay
         at all times, for both the funding transaction and commitment transaction.
         This value can later be updated once the channel is open.
         */
        public long feePerKw;

        /// The unsettled balance in this channel
        public long unsettledBalance;

        /**
         The total number of satoshis we've sent within this channel.
         */
        public long totalSatoshisSent;

        /**
         The total number of satoshis we've received within this channel.
         */
        public long totalSatoshisReceived;

        /**
         The total number of updates conducted within this channel.
         */
        public long numUpdates;

        /**
         The list of active, uncleared HTLCs currently pending within the channel.
         */
        public List<HTLC> pendingHtlcs;

        /**
         The CSV delay expressed in relative blocks. If the channel is force closed,
         we will need to wait for this many blocks before we can regain our funds.
         */
        public int csvDelay;

        /// Whether this channel is advertised to the network or not.
        public boolean isPrivate;

        /// True if we were the ones that created the channel.
        public boolean initiator;

        /// A set of flags showing the current state of the channel.
        public String chanStatusFlags;

        /// The minimum satoshis this node is required to reserve in its balance.
        public long localChanReserveSat;

        /**
         The minimum satoshis the other node is required to reserve in its balance.
         */
        public long remoteChanReserveSat;

        /**
         If true, then this channel uses the modern commitment format where the key
         in the output of the remote party does not change each state. This makes
         back up and recovery easier as when the channel is closed, the funds go
         directly to that key.
         */
        public boolean staticRemoteKey;

        /**
         The number of seconds that the channel has been monitored by the channel
         scoring system. Scores are currently not persisted, so this value may be
         less than the lifetime of the channel [EXPERIMENTAL].
         */
        public long lifetime;

        /**
         The number of seconds that the remote peer has been observed as being online
         by the channel scoring system over the lifetime of the channel [EXPERIMENTAL].
         */
        public long uptime;
    }
    public static final class ListChannelsRequest {
        public boolean activeOnly;
        public boolean inactiveOnly;
        public boolean publicOnly;
        public boolean privateOnly;
    }
    public static final class ListChannelsResponse {
        /// The list of active channels
        public List<Channel> channels;
    }

    // ==========================
    // AddInvoice
    public static final int INVOICE_STATE_OPEN = 0;
    public static final int INVOICE_STATE_SETTLED = 1;
    public static final int INVOICE_STATE_CANCELED = 2;
    public static final int INVOICE_STATE_ACCEPTED = 3;
    public static final class HopHint {
        /// The public key of the node at the start of the channel.
        public String nodeId;

        /// The unique identifier of the channel.
        public long chanId;

        /// The base fee of the channel denominated in millisatoshis.
        public int feeBaseMsat;

        /**
         The fee rate of the channel for sending one satoshi across it denominated in
         millionths of a satoshi.
         */
        public int feeProportionalMillionths;

        /// The time-lock delta of the channel.
        public int cltvExpiryDelta;
    }
    public static final class RouteHint {
        /**
         A list of hop hints that when chained together can assist in reaching a
         specific destination.
         */
        public List<HopHint> hopHints;
    }
    public static final int INVOICE_HTLC_STATE_ACCEPTED = 0;
    public static final int INVOICE_HTLC_STATE_SETTLED = 1;
    public static final int INVOICE_HTLC_STATE_CANCELED = 2;
    /// Details of an HTLC that paid to an invoice
    public static final class InvoiceHTLC {

        /// Short channel id over which the htlc was received.
        public long chanId;

        /// Index identifying the htlc on the channel.
        public long htlcIndex;

        /// The amount of the htlc in msat.
        public long amtMsat;

        /// Block height at which this htlc was accepted.
        public int acceptHeight;

        /// Time at which this htlc was accepted.
        public long acceptTime;

        /// Time at which this htlc was settled or canceled.
        public long resolveTime;

        /// Block height at which this htlc expires.
        public int expiryHeight;

        /// Current state the htlc is in.
        public int state;

        public Map<Long,byte[]> tlv;
    }
    public static final class Invoice {
        /**
         An optional memo to attach along with the invoice. Used for record keeping
         purposes for the invoice's creator, and will also be set in the description
         field of the encoded payment request if the description_hash field is not
         being used.
         */
        public String memo;

        /**
         The hex-encoded preimage (32 byte) which will allow settling an incoming
         HTLC payable to this preimage
         */
        public byte[] rPreimage;

        /// The hash of the preimage
        public byte[] rHash;

        /// The value of this invoice in satoshis
        public long value;

        /// When this invoice was created
        public long creationDate;

        /// When this invoice was settled
        public long settleDate;

        /**
         A bare-bones invoice for a payment within the Lightning Network.  With the
         details of the invoice, the sender has all the data necessary to send a
         payment to the recipient.
         */
        public String paymentRequest;

        /**
         Hash (SHA-256) of a description of the payment. Used if the description of
         payment (memo) is too long to naturally fit within the description field
         of an encoded payment request.
         */
        public byte[] descriptionHash;

        /// Payment request expiry time in seconds. Default is 3600 (1 hour).
        public long expiry;

        /// Fallback on-chain address.
        public String fallbackAddr;

        /// Delta to use for the time-lock of the CLTV extended to the final hop.
        public long cltvExpiry;

        /**
         Route hints that can each be individually used to assist in reaching the
         invoice's destination.
         */
        public List<RouteHint> routeHints;

        /// Whether this invoice should include routing hints for private channels.
        public boolean isPrivate;

        /**
         The "add" index of this invoice. Each newly created invoice will increment
         this index making it monotonically increasing. Callers to the
         SubscribeInvoices call can use this to instantly get notified of all added
         invoices with an add_index greater than this one.
         */
        public long addIndex;

        /**
         The "settle" index of this invoice. Each newly settled invoice will
         increment this index making it monotonically increasing. Callers to the
         SubscribeInvoices call can use this to instantly get notified of all
         settled invoices with an settle_index greater than this one.
         */
        public long settleIndex;

        /**
         The amount that was accepted for this invoice, in satoshis. This will ONLY
         be set if this invoice has been settled. We provide this field as if the
         invoice was created with a zero value, then we need to record what amount
         was ultimately accepted. Additionally, it's possible that the sender paid
         MORE that was specified in the original invoice. So we'll record that here
         as well.
         */
        public long amtPaidSat;

        /**
         The amount that was accepted for this invoice, in millisatoshis. This will
         ONLY be set if this invoice has been settled. We provide this field as if
         the invoice was created with a zero value, then we need to record what
         amount was ultimately accepted. Additionally, it's possible that the sender
         paid MORE that was specified in the original invoice. So we'll record that
         here as well.
         */
        public long amtPaidMsat;

        /**
         The state the invoice is in.
         */
        public int state;

        public boolean isKeysend;

        /// List of HTLCs paying to this invoice [EXPERIMENTAL].
        public List<InvoiceHTLC> htlcs;

        public List<Integer> features;
    }
    public static final class AddInvoiceResponse {
        public byte[] rHash;

        /**
         A bare-bones invoice for a payment within the Lightning Network.  With the
         details of the invoice, the sender has all the data necessary to send a
         payment to the recipient.
         */
        public String paymentRequest;

        /**
         The "add" index of this invoice. Each newly created invoice will increment
         this index making it monotonically increasing. Callers to the
         SubscribeInvoices call can use this to instantly get notified of all added
         invoices with an add_index greater than this one.
         */
        public long addIndex;
    }

    // =============================
    // SubscribeTransactions
    public static final class GetTransactionsRequest {

    }
    public static final class Transaction {
        /// The transaction hash
        public String txHash;

        /// The transaction amount, denominated in satoshis
        public long amount;

        /// The number of confirmations
        public int numConfirmations;

        /// The hash of the block this transaction was included in
        public String blockHash;

        /// The height of the block this transaction was included in
        public int blockHeight;

        /// Timestamp of this transaction
        public long timeStamp;

        /// Fees paid for this transaction
        public long totalFees;

        /// Addresses that received funds for this transaction
        public List<String> destAddresses;

        /// The raw transaction hex.
        public String rawTxHex;
    }
    public static final class TransactionDetails {
        public List<Transaction> transactions;
    }

    //===========================
    // OpenChannel
    public static final class OpenChannelRequest {
        // FIXME remove when lnd bug fixed
        public String nodePubkeyString;

        /// The pubkey of the node to open a channel with
        public byte[] nodePubkey;

        /// The number of satoshis the wallet should commit to the channel
        public long localFundingAmount;

        /// The number of satoshis to push to the remote side as part of the initial commitment state
        public long pushSat;

        /// The target number of blocks that the funding transaction should be confirmed by.
        public int targetConf;

        /// A manual fee rate set in sat/byte that should be used when crafting the funding transaction.
        public long satPerByte;

        /// Whether this channel should be private, not announced to the greater network.
        public boolean isPrivate;

        /// The minimum value in millisatoshi we will require for incoming HTLCs on the channel.
        public long minHtlcMsat;

        /// The delay we require on the remote's commitment transaction. If this is not set, it will be scaled automatically with the channel size.
        public int remoteCsvDelay;

        /// The minimum number of confirmations each one of your outputs used for the funding transaction must satisfy.
        public int minConfs;

        /// Whether unconfirmed outputs should be used as inputs for the funding transaction.
        public boolean spendUnconfirmed;
    }
    public static final class ChannelPoint {
        /// Txid of the funding transaction
        public byte[] fundingTxidBytes;

        /// Hex-encoded string representing the funding transaction
        public String fundingTxidStr;

        /// The index of the output of the funding transaction
        public int outputIndex;
    }
    public static final class OpenStatusUpdate {
        public PendingUpdate chanPending;
        public ChannelOpenUpdate chanOpen;
    }
    public static final class PendingUpdate {
        public byte[] txid;
        public int outputIndex;
    }
    public static final class ChannelOpenUpdate {
        public ChannelPoint channelPoint;
    }

    // ======================
    // CloseChannel
    public static final class CloseChannelRequest {
        /**
         The outpoint (txid:index) of the funding transaction. With this value, Bob
         will be able to generate a signature for Alice's version of the commitment
         transaction.
         */
        public ChannelPoint channelPoint;

        /// If true, then the channel will be closed forcibly. This means the current commitment transaction will be signed and broadcast.
        public boolean force;

        /// The target number of blocks that the closure transaction should be confirmed by.
        public int targetConf;

        /// A manual fee rate set in sat/byte that should be used when crafting the closure transaction.
        public long satPerByte;
    }
    public static final class ChannelCloseUpdate {
        public byte[] closingTxid;

        public boolean success;
    }
    public static final class CloseStatusUpdate {
        public PendingUpdate closePending;
        public ChannelCloseUpdate chanClose;
    }

    // =====================
    // EstimateFee
    public static final class EstimateFeeRequest {
        /// The map from addresses to amounts for the transaction.
        public Map<String, Long> addrToAmount;

        /// The target number of blocks that this transaction should be confirmed by.
        public int targetConf;
    }
    public static final class EstimateFeeResponse {
        /// The total fee in satoshis.
        public long feeSat;

        /// The fee rate in satoshi/byte.
        public long feerateSatPerByte;
    }

    // =====================
    // SendCoins
    public static final class SendCoinsRequest {
        /// The address to send coins to
        public String addr;

        /// The amount in satoshis to send
        public long amount;

        /// The target number of blocks that this transaction should be confirmed by.
        public int targetConf;

        /// A manual fee rate set in sat/byte that should be used when crafting the transaction.
        public long satPerByte;

        /**
         If set, then the amount field will be ignored, and lnd will attempt to
         send all the coins under control of the internal wallet to the specified
         address.
         */
        public boolean sendAll;
    }
    public static final class SendCoinsResponse {
        /// The transaction ID of the transaction
        public String txid;
    }

    public static final class SendManyRequest {
        /// The map from addresses to amounts
        public Map<String, Long> addrToAmount;

        /// The target number of blocks that this transaction should be confirmed by.
        public int targetConf;

        /// A manual fee rate set in sat/byte that should be used when crafting the transaction.
        public long satPerByte;
    }
    public static final class SendManyResponse {
        /// The id of the transaction
        public String txid;
    }

    // =====================
    // SendPayment
    public static final class FeeLimit {
        /// The fee limit expressed as a fixed amount of satoshis.
        public long fixedMsat;

        /// The fee limit expressed as a percentage of the payment amount.
        public long percent;
    }
    public static final class SendRequest {
        /// The identity pubkey of the payment recipient
        public byte[] dest;

        /// Number of satoshis to send.
        public long amtMsat;

        /// The hash to use within the payment's HTLC
        public byte[] paymentHash;

        /**
         A bare-bones invoice for a payment within the Lightning Network.  With the
         details of the invoice, the sender has all the data necessary to send a
         payment to the recipient.
         */
        public String paymentRequest;

        /**
         The CLTV delta from the current height that should be used to set the
         timelock for the final hop.
         */
        public int finalCltvDelta;

        /**
         An upper limit on the amount of time we should spend when attempting to
         fulfill the payment. This is expressed in seconds. If we cannot make a
         successful payment within this time frame, an error will be returned.
         This field must be non-zero.
         */
        public int timeoutSeconds;

        /**
         The maximum number of satoshis that will be paid as a fee of the payment.
         This value can be represented either as a percentage of the amount being
         sent, or as a fixed amount of the maximum fee the user is willing the pay to
         send the payment.
         */
        public FeeLimit feeLimit;

        /**
         The pubkey of the last hop of the route. If empty, any hop may be used.
         */
        public byte[] lastHopPubkey;

        /**
         The channel id of the channel that must be taken to the first hop. If zero,
         any channel may be used.
         */
        public long outgoingChanId;

        /**
         An optional maximum total time lock for the route. This should not exceed
         lnd's `--max-cltv-expiry` setting. If zero, then the value of
         `--max-cltv-expiry` is enforced.
         */
        public int cltvLimit;

        /**
         Route hints that can each be individually used to assist in reaching the
         invoice's destination.
         */
        public List<RouteHint> routeHints;

        /**
         An optional field that can be used to pass an arbitrary set of TLV  records
         to a peer which understands the new records. This can be used to pass
         application specific data during the payment attempt.
         */
        public Map<Long, byte[]> destTlv;

        /**
         Features assumed to be supported by the final node. All transitive feature
         depdencies must also be set properly. For a given feature bit pair, either
         optional or remote may be set, but not both. If this field is nil or empty,
         the router will try to load destination features from the graph as a
         fallback.
         */
        public List<Integer> features;
    }
    public static final class Hop {
        /**
         * The unique channel ID for the channel. The first 3 bytes are the block
         * height, the next 3 the index within the block, and the last 2 bytes are the
         * output index for the channel.
         */
        public long chanId;
        public long chanCapacity;
        public int expiry;
        public long amtToForwardMsat;
        public long feeMsat;

        /**
         * An optional public key of the hop. If the public key is given, the payment
         * can be executed without relying on a copy of the channel graph.
         */
        public String pubKey;

        /**
         * If set to true, then this hop will be encoded using the new variable length
         * TLV format.
         */
        public boolean tlvPayload;

        /**
         * An optional TLV record tha singals the use of an MPP payment. If present,
         * the receiver will enforce that that the same mpp_record is included in the
         * final hop payload of all non-zero payments in the HTLC set. If empty, a
         * regular single-shot payment is or was attempted.
         */
        public MPPRecord mppRecord;

        // custom records
        public Map<Long, byte[]> tlv;
    }
    public static final class MPPRecord {
        /**
         * A unique, random identifier used to authenticate the sender as the intended
         * payer of a multi-path payment. The payment_addr must be the same for all
         * subpayments, and match the payment_addr provided in the receiver's invoice.
         * The same payment_addr must be used on all subpayments.
         */
        public byte[] paymentAddr;

        /**
         * The total amount in milli-satoshis being sent as part of a larger multi-path
         * payment. The caller is responsible for ensuring subpayments to the same node
         * and payment_hash sum exactly to total_amt_msat. The same
         * total_amt_msat must be used on all subpayments.
         */
        public long totalAmtMsat;
    }
    public static final class Route {

        /**
         * The cumulative (final) time lock across the entire route.  This is the CLTV
         * value that should be extended to the first hop in the route. All other hops
         * will decrement the time-lock as advertised, leaving enough time for all
         * hops to wait for or present the payment preimage to complete the payment.
         */
        public int totalTimeLock;

        /**
         * Contains details concerning the specific forwarding details at each hop.
         */
        public List<Hop> hops;

        /**
         * The total fees in millisatoshis.
         */
        public long totalFeesMsat;

        /**
         * The total amount in millisatoshis.
         */
        public long totalAmtMsat;
    }
    public static final class SendResponse {
        public String paymentError;
        public byte[] paymentPreimage;
        public Route paymentRoute;
        public byte[] paymentHash;
    }

    // ====================
    // LookupInvoice
    public static final class PaymentHash {
        /// The payment hash of the invoice to be looked up.
        public byte[] rHash;
    }

    // =======================
    // ListPayments
    public static final class ListPaymentsRequest {
        /**
         * If true, then return payments that have not yet fully completed. This means
         * that pending payments, as well as failed payments will show up if this
         * field is set to True.
         */
        public boolean includeIncomplete;
    }
    public static final int PAYMENT_STATUS_UNKNOWN = 0;
    public static final int PAYMENT_STATUS_IN_FLIGHT = 1;
    public static final int PAYMENT_STATUS_SUCCEEDED = 2;
    public static final int PAYMENT_STATUS_FAILED = 3;
    public static final class Payment {
        /// The payment hash
        public String paymentHash;

        /// The time of this payment in ms
        public long creationTime;

        /// The payment preimage
        public String paymentPreimage;

        /// The value of the payment in satoshis
        public long valueSat;

        /// The value of the payment in milli-satoshis
        public long valueMsat;

        /// The optional payment request being fulfilled.
        public String paymentRequest;

        // The status of the payment.
        public int status;

        ///  The fee paid for this payment in satoshis
        public long feeSat;

        ///  The fee paid for this payment in milli-satoshis
        public long feeMsat;
    }
    public static final class ListPaymentsResponse {
        /// The list of payments
        public List<Payment> payments;
    }

    public static final class PayReqString {
        public String payReq;
    }
    public static final class PayReq {
        public String destination;
        public String paymentHash;
        public long numSatoshis;
        public long timestamp;
        public long expiry;
        public String description;
        public String descriptionHash;
        public String fallbackAddr;
        public long cltvExpiry;
        public byte[] paymentAddr;
        public List<RouteHint> routeHints;
        public List<Integer> features;
    }

    // ====================
    // ChannelBalance
    public static final class ChannelBalanceRequest {
    }
    public static final class ChannelBalanceResponse {
        public long balance;
        public long pendingOpenBalance;
    }

    // =====================
    public static final class BlockEpoch {
        // The hash of the block.
        public byte[] hash;

        // The height of the block.
        public int height;
    }

    public static final class DeleteAllPaymentsRequest{
    }
    public static final class DeleteAllPaymentsResponse{
    }

    public static final class InvoiceSubscription{
        public long addIndex;
        public long settleIndex;
    }

    public static final int CHANNEL_CLOSE_COOPERATIVE_CLOSE = 0;
    public static final int CHANNEL_CLOSE_LOCAL_FORCE_CLOSE = 1;
    public static final int CHANNEL_CLOSE_REMOTE_FORCE_CLOSE = 2;
    public static final int CHANNEL_CLOSE_BREACH_CLOSE = 3;
    public static final int CHANNEL_CLOSE_FUNDING_CANCELED = 4;
    public static final int CHANNEL_CLOSE_ABANDONED = 5;
    public static final class ChannelCloseSummary {
        /// The outpoint (txid:index) of the funding transaction.
        public String channelPoint;

        ///  The unique channel ID for the channel.
        public long chanId;

        /// The hash of the genesis block that this channel resides within.
        public String chainHash;

        /// The txid of the transaction which ultimately closed this channel.
        public String closingTxHash;

        /// Public key of the remote peer that we formerly had a channel with.
        public String remotePubkey;

        /// Total capacity of the channel.
        public long capacity;

        /// Height at which the funding transaction was spent.
        public int closeHeight;

        /// Settled balance at the time of channel closure
        public long settledBalance;

        /// The sum of all the time-locked outputs at the time of channel closure
        public long timeLockedBalance;

        /// Details on how the channel was closed.
        public int closeType;
    }

    public static final class ChannelEventSubscription{
    }
    public static final int CHANNEL_EVENT_OPEN_CHANNEL = 0;
    public static final int CHANNEL_EVENT_CLOSED_CHANNEL = 1;
    public static final int CHANNEL_EVENT_ACTIVE_CHANNEL = 2;
    public static final int CHANNEL_EVENT_INACTIVE_CHANNEL = 3;
    public static final class ChannelEventUpdate {
        public Channel openChannel;
        public ChannelCloseSummary closedChannel;
        public ChannelPoint activeChannel;
        public ChannelPoint inactiveChannel;
        public int type;
    }

    public static final class LightningNode {
        public int lastUpdate;
        public String pubKey;
        public String alias;
        // FIXME add
        // repeated NodeAddress addresses =4[json_name ="addresses"];
        public String color;
        public List<Integer> features;
    }

    public static final class NodeInfoRequest {
        /// The 33-byte hex-encoded compressed public of the target node
        public String pubKey;

        /// If true, will include all known channels associated with the node.
        public boolean includeChannels;
    }

    public static final class RoutingPolicy {
        public int timeLockDelta;
        public long minHtlc;
        public long feeBaseMsat;
        public long feeRateMilliMsat;
        public boolean disabled;
        public long maxHtlcMsat;
        public int lastUpdate;
    }

    /**
     A fully authenticated channel along with all its unique attributes.
     Once an authenticated channel announcement has been processed on the network,
     then an instance of ChannelEdgeInfo encapsulating the channels attributes is
     stored. The other portions relevant to routing policy of a channel are stored
     within a ChannelEdgePolicy for each direction of the channel.
     */
    public static final class ChannelEdge {

        /**
         * The unique channel ID for the channel. The first 3 bytes are the block
         * height, the next 3 the index within the block, and the last 2 bytes are the
         * output index for the channel.
         */
        public long channelId;
        public String chanPoint;

        public String node1Pubkey;
        public String node2Pubkey;

        public long capacity;

        public RoutingPolicy node1Policy;
        public RoutingPolicy node2Policy;
    }

    public static final class NodeInfo {

        /**
         * An individual vertex/node within the channel graph. A node is
         * connected to other nodes by one or more channel edges emanating from it. As
         * the graph is directed, a node will also have an incoming edge attached to
         * it for each outgoing edge.
         */
        public LightningNode node;

        /// The total number of channels for the node.
        public int numChannels;

        public long totalCapacity;

        /// A list of all public channels for the node.
        public List<ChannelEdge> channels;
    }

    public static final class QueryRoutesRequest {

        /// The 33-byte hex-encoded public key for the payment destination
        public String pubKey;

        /**
         * The amount to send expressed in millisatoshis.
         * <p>
         * The fields amt and amt_msat are mutually exclusive.
         */
        public long amtMsat;

        /**
         * An optional CLTV delta from the current height that should be used for the
         * timelock of the final hop. Note that unlike SendPayment, QueryRoutes does
         * not add any additional block padding on top of final_ctlv_delta. This
         * padding of a few blocks needs to be added manually or otherwise failures may
         * happen when a block comes in while the payment is in flight.
         */
        public int finalCltvDelta;

        /**
         * The maximum number of satoshis that will be paid as a fee of the payment.
         * This value can be represented either as a percentage of the amount being
         * sent, or as a fixed amount of the maximum fee the user is willing the pay to
         * send the payment.
         */
        public FeeLimit feeLimit;

        /**
         * A list of nodes to ignore during path finding. When using REST, these fields
         * must be encoded as base64.
         */
        public List<byte[]> ignoredNodes;

        /**
         * The source node where the request route should originated from. If empty,
         * self is assumed.
         */
        public String sourcePubKey;

        /**
         * If set to true, edge probabilities from mission control will be used to get
         * the optimal route.
         */
        public boolean useMissionControl;

        /**
         * A list of directed node pairs that will be ignored during path finding.
         */
        // FIXME add later repeated NodePair ignored_pairs =10;

        /**
         * An optional maximum total time lock for the route. If the source is empty or
         * ourselves, this should not exceed lnd's `--max-cltv-expiry` setting. If
         * zero, then the value of `--max-cltv-expiry` is used as the limit.
         */
        public int cltvLimit;

        /**
         * An optional field that can be used to pass an arbitrary set of TLV records
         * to a peer which understands the new records. This can be used to pass
         * application specific data during the payment attempt. If the destination
         * does not support the specified recrods, and error will be returned.
         * Record types are required to be in the custom range >= 65536. When using
         * REST, the values must be encoded as base64.
         */
        public Map<Long, byte[]> destCustomRecords;

        /**
         * The channel id of the channel that must be taken to the first hop. If zero,
         * any channel may be used.
         */
        public long outgoingChanId;

        /**
         * The pubkey of the last hop of the route. If empty, any hop may be used.
         */
        public byte[] lastHopPubkey;

        /**
         * Optional route hints to reach the destination through private channels.
         */
        public List<RouteHint> routeHints;

        /**
         * Features assumed to be supported by the final node. All transitive feature
         * depdencies must also be set properly. For a given feature bit pair, either
         * optional or remote may be set, but not both. If this field is nil or empty,
         * the router will try to load destination features from the graph as a
         * fallback.
         */
        public List<Integer> destFeatures;
    }

    public static final class QueryRoutesResponse {
        /**
         * The route that results from the path finding operation. This is still a
         * repeated field to retain backwards compatibility.
         */
        public List<Route> routes;

        /**
         * The success probability of the returned route based on the current mission
         * control state. [EXPERIMENTAL]
         */
        public double successProb;
    }

    public static final class SendToRouteRequest {
        /**
         * The payment hash to use for the HTLC. When using REST, this field must be
         * encoded as base64.
         */
        public byte[] paymentHash;

        /// Route that should be used to attempt to complete the payment.
        public Route route;
    }

    public static final class ChanBackupExportRequest {}
    public static final class ChannelBackupSubscription {}
    public static final class ChannelBackup {
        /**
         * Identifies the channel that this backup belongs to.
         */
        public ChannelPoint chanPoint;

        /**
         * Is an encrypted single-chan backup. this can be passed to
         * RestoreChannelBackups, or the WalletUnlocker Init and Unlock methods in
         * order to trigger the recovery protocol. When using REST, this field must be
         * encoded as base64.
         */
        public byte[] chanBackup;
    }
    public static final class ChannelBackups {
        /**
         * A set of single-chan static channel backups.
         */
        public List<ChannelBackup> chanBackups;
    }

    public static final class MultiChanBackup {
        /**
         * Is the set of all channels that are included in this multi-channel backup.
         */
        public List<ChannelPoint> chanPoints;

        /**
         * A single encrypted blob containing all the static channel backups of the
         * channel listed above. This can be stored as a single file or blob, and
         * safely be replaced with any prior/future versions. When using REST, this
         * field must be encoded as base64.
         */
        public byte[] multiChanBackup;
    }
    public static final class ChanBackupSnapshot {
        /**
         * The set of new channels that have been added since the last channel backup
         * snapshot was requested.
         */
        public ChannelBackups singleChanBackups;

        /**
         * A multi-channel backup that covers all open channels currently known to
         * lnd.
         */
        public MultiChanBackup multiChanBackup;
    }


}

