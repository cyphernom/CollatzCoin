package Blockchain;


import Transaction.TransactionOutput;
import Transaction.Transaction;
import Transaction.TransactionInput;
import Networking.Node;
import Util.StringUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */
public class Blockchain implements Serializable {

    private ArrayList<Block> chain;
    private int difficulty; // Current difficulty level of the blockchain
    private final long targetBlockTime; // Target time between blocks in milliseconds
    private final int adjustmentInterval; // How many blocks to consider for adjusting difficulty
    private final UTXOPool utxoPool; // The pool of unspent transactions
    private final TransactionPool transactionPool; // The pool of unconfirmed transactions
    private String blockchainFile;
    private static Blockchain instance;
    private transient Node node;
    private long totalWork = 0; // To track the total work of the blockchain
    public Blockchain(int initialDifficulty, long targetBlockTime, int adjustmentInterval, String fileName, Node node ) {
        this.blockchainFile = fileName;
       File file = new File(this.blockchainFile);
        if (file.exists()) {
            // File exists, load the blockchain from the file
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                Blockchain loadedBlockchain = (Blockchain) in.readObject();
                // Now, copy the loaded data into this instance
                this.chain = loadedBlockchain.chain;
                this.difficulty = loadedBlockchain.difficulty;
                this.targetBlockTime = loadedBlockchain.targetBlockTime;
                this.adjustmentInterval = loadedBlockchain.adjustmentInterval;
                this.utxoPool = loadedBlockchain.utxoPool;
                this.transactionPool = loadedBlockchain.transactionPool;
                this.node = loadedBlockchain.node;
            } catch (IOException | ClassNotFoundException e) {
                // Handle exceptions: for example, log them and exit or throw a RuntimeException
                e.printStackTrace();
                throw new RuntimeException("Failed to load the blockchain from file.", e);
            }
        } else {
            System.out.println("starting blockchain record");
            // File does not exist, initialize a new blockchain
            this.chain = new ArrayList<>();
            this.difficulty = initialDifficulty;
            this.targetBlockTime = targetBlockTime;
            this.adjustmentInterval = adjustmentInterval;
            this.utxoPool = new UTXOPool();
            chain.add(createGenesisBlock());
            this.transactionPool = TransactionPool.getInstance();
            this.node = node;
             
        }

    }
    // Method to get the entire chain
    public List<Block> getChain() {
        return chain;
    }
    public long getTotalWork() {
        return totalWork;
    }
    
// Method to add a transaction to the pool
    public boolean addTransactionToPool(Transaction transaction) {
        transactionPool.addTransaction(transaction);
        node.broadcastTransaction(transaction); // Broadcast the transaction
        return true;
    }


    // Method to remove a transaction from the pool
    public boolean removeTransactionFromPool(String transaction) {
       return transactionPool.removeTransaction(transaction);
    }

    // Method to get the list of unconfirmed transactions
    public List<Transaction> getUnconfirmedTransactions() {
        return transactionPool.getUnconfirmedTransactions();
    }

    // Method to get the transaction pool itself
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    // Add a new block to the chain and update the UTXOPool

    public boolean addBlock(Block newBlock) {
    // Validate the new block
        if (!isValidBlock(newBlock)) {
            // Handle invalid block, e.g., by logging or throwing an exception
            System.out.println("invalid block not added");
            return false;
        }

        // Add the new block to the blockchain
        this.chain.add(newBlock);
        this.totalWork += calculateBlockWork(newBlock);
        updateUTXOPoolWithBlock(newBlock);



        // Adjust the difficulty
        adjustDifficulty();
            // After adding a block and updating the chain, save the blockchain to a file
        saveToFile();

        node.broadcastNewBlock(newBlock);
    
    System.out.println("Block added with difficulty: " + difficulty);
    return true;
}

    // Updates the UTXOPool with the transactions in the new block,clears the transaction pool of the transactions in the newblock
    private void updateUTXOPoolWithBlock(Block newBlock) {
        for (Transaction transaction : newBlock.getTransactions()) {
            System.out.println("adding transaction"+transaction.toString());
            // For each input in the transaction, remove the corresponding UTXO from the pool
            try {
            for (TransactionInput input : transaction.getInputs()) {
                System.out.println("removing tx inputs from utxopool:");
                utxoPool.removeUTXO(input.getTransactionOutputId());
            }
            } catch (Exception e) {
              //  e.printStackTrace();
            }
            // For each output in the transaction, add a new UTXO to the pool
            for (TransactionOutput output : transaction.getOutputs()) {
                utxoPool.addUTXO(output.getId(), output);
            }
                //remove tx from transaction pool

            removeTransactionFromPool(transaction.getTransactionId());

        }
    }

    // Other methods unchanged...

    // Method to adjust the difficulty of the mining process
    public void adjustDifficulty() {
        // We only adjust the difficulty after the blockchain has a certain number of blocks
        if ((chain.size() % adjustmentInterval) != 0) {
            System.out.println("no difficulty adjustment yet");
                    return;
        }

        Block lastBlock = chain.get(chain.size() - 1);
        Block comparisonBlock = chain.get(chain.size() - adjustmentInterval);

        long actualTimeTaken = lastBlock.getTimeStamp() - comparisonBlock.getTimeStamp();
        long expectedTimeTaken = targetBlockTime * adjustmentInterval;
            System.out.println("difficulty:"+difficulty+" timetaken:"+actualTimeTaken+" expected:"+expectedTimeTaken);

        // If the actual time taken is less than the expected time, we increase the difficulty
        if (actualTimeTaken < expectedTimeTaken) {
            difficulty++;
        } else if (actualTimeTaken > expectedTimeTaken) {
            difficulty = Math.max(difficulty - 1, 1); // Ensure difficulty doesn't fall below 1
        }
    }

    // Method to get the current difficulty
    public int getDifficulty() {
        return difficulty;
    }
    
    public boolean isValidBlock(Block block) {
        // Check if the hash is as claimed
        if (!block.getHash().equals(block.calculateHash())) {
            return false;
        }

        // Check if the hash has the required number of leading zeros (proof of work)
        String requiredPrefix = String.join("", Collections.nCopies(difficulty, "0"));
        if (!block.getHash().startsWith(requiredPrefix)) {
            return false;
        }
        // Verify Merkle root
        String calculatedMerkleRoot = StringUtil.getMerkleRoot(new ArrayList<>(block.getTransactions()));
        if (!block.getMerkleRoot().equals(calculatedMerkleRoot)) {
            return false;
        }

            // Verify the signature of each transaction
        for (Transaction transaction : block.getTransactions()) {
            if (!transaction.verifySignature()) {
                return false;
            }
        }
        //verify each UTXO in each tranaction is unspent and that each tranaction is balanced
        for (Transaction transaction : block.getTransactions()) {
            if ((!areInputsUnspent(transaction, utxoPool) || !isTransactionBalanced(transaction, utxoPool))&&!transaction.isCoinbase()) {
                return false;
            }
        }
        
        return true;
    }
    private boolean areInputsUnspent(Transaction transaction, UTXOPool utxoPool) {
        if(!transaction.isCoinbase()){
            for (TransactionInput input : transaction.getInputs()) {
                TransactionOutput referencedUTXO = utxoPool.getUTXO(input.getTransactionOutputId());
                if (referencedUTXO == null || !referencedUTXO.equals(input.getUTXO())) {
                    // The input is not in UTXO set or does not match the referenced UTXO.
                    return false;
                }
            }
        }
        return true;
    }


    private boolean isTransactionBalanced(Transaction transaction, UTXOPool utxoPool) {
        if (!transaction.isCoinbase()){
            long inputSum = 0;
            for (TransactionInput input : transaction.getInputs()) {
                TransactionOutput utxo = utxoPool.getUTXO(input.getTransactionOutputId());
                if (utxo != null) {
                    inputSum += utxo.getValue();
                }
            }

            long outputSum = 0;
            for (TransactionOutput output : transaction.getOutputs()) {
                outputSum += output.getValue();
            }

            // For non-coinbase transactions, the input sum should be at least the output sum.
            // Any excess in inputs is considered a transaction fee.
            return  inputSum >= outputSum;
        }
        return transaction.isCoinbase();
    }

    // In Blockchain class

    public boolean addTransactionToBlockchain(Transaction transaction) {
        if (transaction.verifySignature()) {
            if (transaction.createAndProcessTransaction()) {
              node.broadcastTransaction(transaction);
            }
        }
        return false;
    }
    // Method to handle chain reorganization
    public void reorganizeChain(List<Block> newChain) {
        // Find the common ancestor block between the current chain and newChain
        int commonAncestorIndex = findCommonAncestorIndex(newChain);

        // Remove blocks from the current chain that are not in the new chain
        for (int i = chain.size() - 1; i > commonAncestorIndex; i--) {
            removeBlock(chain.get(i));
        }

        // Add blocks from the new chain
        for (int i = commonAncestorIndex + 1; i < newChain.size(); i++) {
            addBlock(newChain.get(i));
        }

        // Save the updated blockchain
        saveToFile();
    }
    
        // Find the common ancestor index in the current chain for the given new chain
    private int findCommonAncestorIndex(List<Block> newChain) {
        for (int i = 0; i < Math.min(chain.size(), newChain.size()); i++) {
            if (!chain.get(i).getHash().equals(newChain.get(i).getHash())) {
                return i - 1; // Return the index of the last common block
            }
        }
        return Math.min(chain.size(), newChain.size()) - 1; // In case one chain is a subset of the other
    }

    // Method to remove a block (and its associated transactions) from the blockchain
    private void removeBlock(Block block) {
        // Logic to reverse the effects of this block on the UTXOPool, transactionPool, etc.
        chain.remove(block);
        totalWork -= calculateBlockWork(block);
        // More cleanup logic as needed...
    }
    
    // Manually create the first block in the blockchain with a predefined hash (as there is no previous block).
    private Block createGenesisBlock() {
        // Assuming the Block constructor takes parameters for previousHash, timestamp, list of transactions, and nonce
        // We'll use "0" as the previousHash and an empty list for transactions for the genesis block
        return new Block("0", System.currentTimeMillis(), new ArrayList<Transaction>(), 0, this.difficulty);
    }

    // Get the latest block in the chain
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }



    // Checks if the blockchain is valid
    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block currentBlock = chain.get(i);
            Block previousBlock = chain.get(i - 1);

            // Compare registered hash and calculated hash:
            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                return false;
            }

            // Compare previous hash and registered previous hash:
            if (!previousBlock.getHash().equals(currentBlock.getPreviousHash())) {
                return false;
            }
        }
        return true;
    }

    // Getters and setters as needed.

    public Map<String, TransactionOutput> getUTXOs() {
        return utxoPool.getAllUTXOs();
    }
    
    public UTXOPool getUTXOPool(){
        return utxoPool;
    }

    public void saveToFile() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(this.blockchainFile))) {
            out.writeObject(this);
        } catch (IOException e) {
            // Handle exceptions, possibly rethrow as a RuntimeException
            e.printStackTrace();
        }
    }
    public long getTotalCoins() {
        long totalCoins = 0;
        for (TransactionOutput utxo : utxoPool.getAllUTXOs().values()) {
            totalCoins += utxo.getValue();
        }
        return totalCoins;
    }

    public int getBlocksPerYear() {
        // There are 31,536,000 seconds in a year (365 days)
        System.out.println("expected bpy:" +(int)(31536000000l / this.targetBlockTime));
        return (int)(31536000000l / this.targetBlockTime);
    }

    public boolean isEmpty() {
        return chain.isEmpty();
    }

    public void setNode(Node node) {
        this.node = node;
    }
    // Method to calculate the work of a single block (you can adjust the logic as needed)
    public long calculateBlockWork(Block block) {
        // Assuming difficulty is inversely proportional to work
        return (long) Math.pow(2, 32) / (block.getDifficulty() + 1);
    }
    
    


}
