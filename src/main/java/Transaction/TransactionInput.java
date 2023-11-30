package Transaction;


import java.io.Serializable;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */
public class TransactionInput implements Serializable  {
    private String transactionOutputId; // Reference to TransactionOutputs -> transactionId
    TransactionOutput UTXO; // Contains the Unspent transaction output
    
    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }

    // Getters and setters
    public String getTransactionOutputId() {
        return transactionOutputId;
    }

    public void setTransactionOutputId(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }

    public TransactionOutput getUTXO() {
        return UTXO;
    }

    public void setUTXO(TransactionOutput UTXO) {
        this.UTXO = UTXO;
    }

}
