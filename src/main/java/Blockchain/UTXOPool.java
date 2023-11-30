package Blockchain;


import Transaction.TransactionOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */
public class UTXOPool implements Serializable {
    private final Map<String, TransactionOutput> UTXOs; // A container to store the UTXOs.

    public UTXOPool() {
        UTXOs = new HashMap<>();
    }

    // Adds a new UTXO to the pool.
    public void addUTXO(String transactionOutputId, TransactionOutput transactionOutput) {
        System.out.println("adding UTXO:" + transactionOutput.toString());
        UTXOs.put(transactionOutputId, transactionOutput);
    }

    // Removes the UTXO from the pool.
    public void removeUTXO(String transactionOutputId) {
        System.out.println("removing txid from utxopool:" + transactionOutputId);
        try {
        UTXOs.remove(transactionOutputId);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Check if the UTXO exists in the pool.
    public boolean containsUTXO(String transactionOutputId) {
        return UTXOs.containsKey(transactionOutputId);
    }

    // Get the UTXO from the pool.
    public TransactionOutput getUTXO(String transactionOutputId) {
        return UTXOs.get(transactionOutputId);
    }

    // Get all UTXOs in the pool. This might be used when constructing the list of transactions to include in a new block.
    public Map<String, TransactionOutput> getAllUTXOs() {
        return UTXOs;
    }


}
