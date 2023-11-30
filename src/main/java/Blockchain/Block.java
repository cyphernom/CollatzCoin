package Blockchain;


import Transaction.Transaction;
import Util.StringUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */
public class Block implements Serializable {
    private String hash;
    private String previousHash;
    private List<Transaction> transactions;
    private long timeStamp;
    private long nonce;
    private String merkleRoot; // Add the Merkle root field
    // Constructor
    private final long difficulty;
    
    public Block(String previousHash, long timeStamp, List<Transaction> transactions, long nonce, long difficulty) {
        this.previousHash = previousHash;
        this.timeStamp = timeStamp;
        this.transactions = transactions;
        this.nonce = nonce;
        this.hash = calculateHash(); // Calculate the hash when the block is created
        this.merkleRoot = StringUtil.getMerkleRoot(new ArrayList<>(transactions)); // Compute the Merkle root
        this.difficulty = difficulty;
    }
    
    public long getDifficulty() {
        return this.difficulty;
    }
    
        // Method to calculate the block's hash
    //NOW that we have the merkleroot of the arraylist of tx, we no longer need the hashes of all of the tx.
    public String calculateHash() {
        return StringUtil.applySha256(
                previousHash +
                Long.toString(timeStamp) +
                Long.toString(nonce) +
                merkleRoot // Include the Merkle root in the hash calculation
        );
    } 
    
    private String getTransactionsHash() {
        StringBuilder hashBuilder = new StringBuilder();
        for (Transaction transaction : transactions) {
            hashBuilder.append(transaction.getTransactionId());
        }
        return StringUtil.applySha256(hashBuilder.toString());
    }


    // Standard getters and setters for each field
    public String getHash() {
        return hash;
    }



    public String getPreviousHash() {
        return previousHash;
    }


    public List<Transaction> getTransactions() {
        return transactions;
    }


    public long getTimeStamp() {
        return timeStamp;
    }



    public long getNonce() {
        return nonce;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Transaction transaction : transactions) {
            builder.append(transaction.toString());
        }
        return builder.toString();
    }
    // Additional methods could include a constructor and perhaps a toString() for easy printing

    // Getter for the Merkle root
    public String getMerkleRoot() {
        return merkleRoot;
    }
}
