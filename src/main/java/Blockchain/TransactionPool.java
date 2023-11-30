package Blockchain;


import Transaction.Transaction;
import java.io.Serializable;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionPool  implements Serializable {
    private List<Transaction> pool;
    private static TransactionPool instance;
   // Make the constructor private so it's not accessible from outside
    private TransactionPool() {
        System.out.println("Mempool instantiated");
        pool = Collections.synchronizedList(new ArrayList<>());
    }

    // Provide a static method that returns the instance
    public static TransactionPool getInstance() {
        if (instance == null) {
            // If there is no instance available, create a new one (thread-safe)
            synchronized (TransactionPool.class) {
                if (instance == null) {
                    instance = new TransactionPool();
                }
            }
        }
        return instance;
    }

    public boolean addTransaction(Transaction transaction) {
        System.out.println("attempting to add a tx to mempool");
        // Add verification logic to ensure the transaction is valid before adding
        return pool.add(transaction);
    }

    public boolean removeTransaction(String transactionId) {
       return pool.removeIf(transaction -> (transaction.getTransactionId() == null ? transactionId == null : transaction.getTransactionId().equals(transactionId)));
    }

    public List<Transaction> getUnconfirmedTransactions() {
        // Return transactions that have not yet been included in a block
       System.out.println("attempting to return the pool as an arraylist:" +pool.toString());
        return new ArrayList<>(pool); // Return a copy to avoid modification while iterating
    }
}

