package Transaction;


import Blockchain.UTXOPool;
import Util.StringUtil;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */
public class Transaction  implements Serializable {
    private final PublicKey sender; // Sender's public key
    private final String recipient; // Recipient's address (hash of their public key)
    private final long value;
    private byte[] signature; // This is to prevent anybody else from spending funds in our wallet.
    private ArrayList<TransactionInput> inputs = new ArrayList<>();
    private final ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0; // A rough count of how many transactions have been generated. 
    private String transactionId;

    private final UTXOPool utxoPool;
    private final boolean isCoinbase;
    // Constructor:
    public Transaction(PublicKey from, String toAddress, long value, ArrayList<TransactionInput> inputs, UTXOPool utxoPool, boolean isCoinbase) {
        this.sender = from;
        this.recipient = toAddress;
        this.value = value;
        this.inputs = inputs;
        this.transactionId = calculateHash(); // Initialize the transaction ID
        this.utxoPool = utxoPool;
        this.isCoinbase = isCoinbase;
    }
    //overload contructor default coinbase false
    public Transaction(PublicKey from, String to, long value, ArrayList<TransactionInput> inputs, UTXOPool utxoPool) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
        this.transactionId = calculateHash(); // Initialize the transaction ID
        this.utxoPool = utxoPool;
        this.isCoinbase = false;
    }
    public void signTransaction(PrivateKey privateKey) {
        if (!isCoinbase) { // Coinbase transactions do not have a signature
            String data = StringUtil.getStringFromKey(sender) + recipient + Float.toString(value);
            signature = StringUtil.applyECDSASig(privateKey, data);
        }
    }
    // In the Transaction class
    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + recipient + Float.toString(value);
        signature = StringUtil.applyECDSASig(privateKey, data);
    }
    // In the Transaction class
    public boolean verifySignature() {
        if (isCoinbase) {
            return true; // Coinbase transactions do not have a signature
        }
        String data = StringUtil.getStringFromKey(sender) + recipient + Float.toString(value);
        return StringUtil.verifyECDSASig(sender, data, signature);
    }


    // Helper method to get the total value of unspent transactions for a wallet.
    private long getInputsValue(ArrayList<TransactionInput> inputs) {
        long total = 0;
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue; // If the transaction can't be found, skip it.
            total += i.UTXO.getValue();
        }
        return total;
    }

    // This Calculates the transaction hash (which will be used as its Id)
    private String calculateHash() {
        sequence++;
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                recipient +
                Float.toString(value) + sequence
        );
    }
    // Other methods would include processing the transaction, adding inputs and outputs, etc.

    public boolean isValid() {
        // Verify the signature
        if (isCoinbase) {
            return true; // Skip the rest of the validation for coinbase transactions
        }

        if (!verifySignature()) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }
        // Calculate the sum of transaction inputs
        long inputsSum = 0;
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) {
                System.out.println("#Referenced input on Transaction(" + transactionId + ") is Missing or invalid");
                return false;
            }
            inputsSum += i.UTXO.getValue();
        }

        // Calculate the sum of transaction outputs
        long outputsSum = 0;
        for (TransactionOutput o : outputs) {
            outputsSum += o.getValue();
        }

        // Check if inputs sum is greater than or equal to outputs sum
        if (inputsSum < outputsSum) {
            System.out.println("#Input values are not equal to the output values on Transaction(" + transactionId + ")");
            return false;
        }

        // Check if inputs are unspent, that is left to the UTXO management system

        return true;
    }

    public boolean createAndProcessTransaction() {
        if (isCoinbase) {
            System.out.println("attempting to process coinbase!");
            // Special handling for coinbase transactions
            outputs.add(new TransactionOutput(this.recipient, value, transactionId)); //send value to recipient
            utxoPool.addUTXO(outputs.get(0).getId(), outputs.get(0));
            return true;
        }
        if (getInputsValue(inputs, utxoPool) < value) {
            System.out.println("#Not Enough funds to send transaction. Transaction Discarded.");
            return false;
        }

        // Generate transaction outputs:
        long leftOver = getInputsValue(inputs, utxoPool) - value; //get value of inputs then the left over change:
        transactionId = calculateHash();
        outputs.add(new TransactionOutput(this.recipient, value, transactionId)); //send value to recipient
      
                // Change the part where you create TransactionOutputs
        outputs.add(new TransactionOutput(this.recipient, value, transactionId)); // Send value to address
        if (leftOver > 0) {
            outputs.add(new TransactionOutput(StringUtil.getStringFromKey(sender), leftOver, transactionId)); // Return change
        }
        
        // Add outputs to Unspent list
        for (TransactionOutput o : outputs) {
            utxoPool.addUTXO(o.getId(), o);
        }

        // Remove transaction inputs from UTXO lists as spent:
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue; //if Transaction can't be found skip it 
            utxoPool.removeUTXO(i.UTXO.getId());
        }

        return true;
    }

    // Gets the sum of inputs (UTXOs) values
    private long getInputsValue(ArrayList<TransactionInput> inputs, UTXOPool utxoPool) {
        long total = 0l;
        for (TransactionInput i : inputs) {
            if (utxoPool.getUTXO(i.getTransactionOutputId()) != null) {
                total += utxoPool.getUTXO(i.getTransactionOutputId()).getValue();
            }
        }
        return total;
    }


    // This method should simply return the transactionId.
    public String getTransactionId() {
        return transactionId;
    }
    // And in Transaction class
    @Override
    public String toString() {
        // Include all the necessary fields to represent the transaction
        return StringUtil.getStringFromKey(sender) +
               recipient +
               Float.toString(value) +
               Arrays.toString(signature);
    }

    // Returns the list of inputs
    public ArrayList<TransactionInput> getInputs() {
        return inputs;
    }

    // Returns the list of outputs
    public ArrayList<TransactionOutput> getOutputs() {
        return outputs;
    }
    public String getSenderAddress() {
        // Method to return the address of the sender
        return StringUtil.getAddressFromKey(sender);
    }

    public String getRecipientAddress() {
        // Return the recipient address directly
        return recipient;
    }

    public long getValue() {
       return value;
    }

  public  boolean isCoinbase() {
        return this.isCoinbase;
    }
}
