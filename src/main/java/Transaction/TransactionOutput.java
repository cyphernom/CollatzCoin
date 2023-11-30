package Transaction;


import Util.StringUtil;
import java.io.Serializable;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */
public class TransactionOutput implements Serializable {
    String id;
    private final String recipientAddress; // The address, not the public key
    private final long value;
    private final String parentTransactionId;

    // Constructor
    public TransactionOutput(String recipientAddress, long value, String parentTransactionId) {
        this.recipientAddress = recipientAddress;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = calculateHash();
    }

    // Calculate the hash (ID) of this transaction output
    private String calculateHash() {
        return StringUtil.applySha256(
                recipientAddress +
                Long.toString(value) +
                parentTransactionId
        );
    }

    // Check if this UTXO belongs to an address
    public boolean isMine(String address) {
        return address.equals(this.recipientAddress);
    }

    // Getters
    public String getRecipientAddress() { return recipientAddress; }
    public long getValue() { return value; }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

    public String getId() {
        return id;
    }
}
