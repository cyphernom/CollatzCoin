package Wallet;

import Util.StringUtil;
import Blockchain.Block;
import Blockchain.Blockchain;
import Blockchain.UTXOPool;
import Transaction.TransactionInput;
import Transaction.TransactionOutput;
import Transaction.Transaction;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.util.List;
import Blockchain.TransactionPool;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */
public final class Wallet implements Serializable  {
    private PrivateKey privateKey;
    private PublicKey publicKey;

    // Assuming a reference to the UTXOPool to know which outputs belong to this wallet
    private UTXOPool utxoPool;

    public Wallet(UTXOPool utxoPool) {
        this.utxoPool = utxoPool;
        generateKeyPair();
    }

    public void setUTXOPool(UTXOPool pool) {
        this.utxoPool = pool;
    }

    // Retrieve UTXOs that belong to this wallet (i.e., where the recipient matches this wallet's public key)
    public ArrayList<TransactionOutput> getMyUTXOs() {

        ArrayList<TransactionOutput> myUTXOs = new ArrayList<>();
        for (TransactionOutput utxo : utxoPool.getAllUTXOs().values()) {

            if (utxo.isMine(StringUtil.getAddressFromKey(this.publicKey))) {
              
                myUTXOs.add(utxo);
            }
        }
        return myUTXOs;
    }

    // This method creates transaction inputs from UTXOs until it covers the amount we want to send
    public ArrayList<TransactionInput> getUnspentTransactions(long value) {
        ArrayList<TransactionOutput> myUTXOs = getMyUTXOs();
        ArrayList<TransactionInput> inputs = new ArrayList<>();
        long total = 0;

        for (TransactionOutput utxo : myUTXOs) {
            total += utxo.getValue();
            TransactionInput input = new TransactionInput(utxo.getId());
            input.setUTXO(utxo);
            inputs.add(input);
            if (total > value) break;
        }

        // This is a simple check. A real implementation should also consider transaction fees, if applicable.
        if (total < value) {
            System.out.println("Not Enough funds to send transaction. Transaction Discarded.");
            return null;
        }

        return inputs;
    }
    
    public Wallet() {
        generateKeyPair();
    }

    public void generateKeyPair() {
        try {

            Security.addProvider(new BouncyCastleProvider());
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            keyGen.initialize(ecSpec, random); //256 bytes provides an acceptable security level
            KeyPair keyPair = keyGen.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PrivateKey getPrivateKey() {
       return privateKey;
    }
    

    public PublicKey getPublicKey() {
        return publicKey;
    }
    
    
   public List<Transaction> getMyTransactions(Blockchain blockchain) {
        List<Transaction> myTransactions = new ArrayList<>();
        String myAddress = generateAddress(); // Method to generate address from public key

        for (Block block : blockchain.getChain()) { // Assuming there's a method to get the chain
            for (Transaction transaction : block.getTransactions()) {
                // Check if you are the sender or the recipient in the transaction
                if (transaction.getSenderAddress().equals(myAddress) || 
                    transaction.getRecipientAddress().equals(myAddress)) {
                    myTransactions.add(transaction);
                }
            }
        }
        return myTransactions;
    }
    public long getBalance() {
        long balance = 0;
        for (TransactionOutput utxo : this.utxoPool.getAllUTXOs().values()) {
            if (utxo.isMine(StringUtil.getAddressFromKey(this.publicKey))) {
                balance += utxo.getValue();
            }
        }
        return balance;
    }
    
    
public Transaction sendFunds(String recipient, long amount) {
    if (getBalance() < amount) {
        System.out.println("Not Enough funds to send transaction. Transaction Discarded.");
        return null;
    }

    // Create array list of inputs
    ArrayList<TransactionInput> inputs = getUnspentTransactions(amount);

    Transaction newTransaction = new Transaction(publicKey, recipient, amount, inputs, utxoPool);
    newTransaction.generateSignature(privateKey);

    // Process the transaction and broadcast it to the network.
    if(TransactionPool.getInstance().addTransaction(newTransaction)) {
        System.out.println("Transaction added to the transaction pool");
    } else {
        System.out.println("Transaction failed to process. Discarded.");
        return null;
    }

    return newTransaction;
}

    public String generateAddress() {
        return StringUtil.getAddressFromKey(publicKey);
    }

}
