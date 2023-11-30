package Mining;


import Blockchain.Blockchain;
import Blockchain.Block;
import Transaction.Transaction;
import Blockchain.TransactionPool;
import Networking.Node;
import Wallet.Wallet;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import Util.StringUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */
 public class Mining implements Runnable {
    private volatile boolean running = true; // Flag to control the mining loop

    private final int difficulty; // This is the number of leading zeroes required in the hash
    private final Blockchain blockchain;
    private final Node node;
    private final Wallet minerWallet; // Wallet belonging to the miner
    private final long INITIAL_MINING_REWARD = 50*100000000; // The reward amount for mining a new block
    // The block reward decay rate might be a function of the target S2F ratio:
    private final long TARGET_S2F_RATIO = 60;
    private final long TARGET_YEARS_TO_REACH_S2F = 2; // Midpoint between 2 and 3 years
    private long currentMiningReward = INITIAL_MINING_REWARD;
    
    public Mining(Node node, Wallet minerWallet) {
        this.node = node;        
        this.blockchain = node.getBlockchain();
        this.difficulty = node.getDifficulty();

        this.minerWallet = minerWallet; // Initialize the miner's wallet
    }
    
    
    public Block mineBlock(List<Transaction> transactions) throws InterruptedException {
        String target = new String(new char[difficulty]).replace('\0', '0');
        Block latestBlock = blockchain.getLatestBlock();
        long nonce = 0;

        while(!Thread.currentThread().isInterrupted()) {
            Block newBlock = new Block(
                latestBlock.getHash(),
                System.currentTimeMillis(),
                transactions,
                nonce,
                this.difficulty
            );

            String hash = newBlock.calculateHash();

            if (hash.substring(0, difficulty).equals(target)) {
                if (validCollatzSequence(nonce)) {
                    return newBlock; // Found a valid block
                }
            }

            nonce++; // Increment nonce for next iteration
        }

        throw new InterruptedException("Mining was interrupted");
    }



    
    
    private boolean validCollatzSequence(long nonce) {
    /*    while (nonce != 1) {
            if ((nonce % 2) == 0) {
                nonce = nonce / 2;
            } else {
                // Here you can define the property that you consider to be "valid" for your blockchain.
                // For example, you might require that the sequence reaches a number below a certain threshold
                // within a certain number of steps.
                nonce = 3 * nonce + 1;
            }
        }*/
        // If the sequence reaches 1, then it's a valid Collatz sequence.
        // You can add additional conditions to define a valid sequence for your blockchain.
        return true;
    }


 
    // This method can be called to start the mining process
    public void startMining() {

            System.out.println("miner wallet balance:"+minerWallet.getBalance());
            // For continuous mining, you might want a different condition to stop the mining process
            List<Transaction> transactions = getTransactionsForNewBlock(); 
            Block newBlock = null;
        try {
            newBlock = mineBlock(transactions);
        } catch (InterruptedException ex) {
            Logger.getLogger(Mining.class.getName()).log(Level.SEVERE, null, ex);
        }
            try {
                System.out.println("Block found, broadcasting to nodes");
                 node.broadcastNewBlock(newBlock);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private void adjustMiningReward() {    
        
        System.out.println("currentMiningReward: "+currentMiningReward);
        // Get the total number of coins mined and in circulation
        long totalCoins = blockchain.getTotalCoins();
        System.out.println("total:"+totalCoins);
        // Calculate the current S2F ratio
        long currentS2F = totalCoins / (currentMiningReward * blockchain.getBlocksPerYear());
        if(currentS2F ==0.0) currentS2F = 1l;
            System.out.println("s2f:"+currentS2F);
        // Check if the current S2F ratio is already at or above the target
        if (currentS2F >= TARGET_S2F_RATIO) {
            // Adjust the reward to maintain the target S2F ratio       
            currentMiningReward = totalCoins / (TARGET_S2F_RATIO * blockchain.getBlocksPerYear());
                    System.out.println("currentMiningReward: "+currentMiningReward);
        } else {
            // Calculate the decay factor per block to reach the target S2F ratio within the target time frame
            long blocksToTargetS2F = TARGET_YEARS_TO_REACH_S2F * blockchain.getBlocksPerYear();
            long decayRate = (long) Math.pow(TARGET_S2F_RATIO / currentS2F, 1 / blocksToTargetS2F);
            currentMiningReward *= decayRate;
                    System.out.println("currentMiningReward: "+currentMiningReward);
        }
    }
    
    private List<Transaction> getTransactionsForNewBlock() {
        List<Transaction> transactions = new ArrayList<>();

        // Create a coinbase transaction for the block reward
        Transaction coinbaseTx = createCoinbaseTransaction(minerWallet.getPublicKey(), currentMiningReward);
        
        transactions.add(coinbaseTx);

        // Add other transactions from the pool
        transactions.addAll(getUnconfirmedTransactions());

        try {
            // Verify transactions and remove any invalid ones.
            transactions.removeIf(transaction -> !transaction.isValid());
        } catch (Exception e) {
            e.printStackTrace(); // This will print the stack trace of the exception
        }
        for(Transaction t: transactions) {
            t.createAndProcessTransaction();
        }
        // If the code reaches this point, the println will execute
        System.out.println("Transactions after removal of invalid ones: " + transactions);

        return transactions;
    }


    private Transaction createCoinbaseTransaction(PublicKey minerPublicKey, long rewardAmount) {
        adjustMiningReward(); // Adjust the block reward before creating the coinbase transaction
        Transaction coinbaseTx = new Transaction(minerPublicKey, StringUtil.getAddressFromKey(minerPublicKey), rewardAmount, null, blockchain.getUTXOPool(),true);
        return coinbaseTx;
    }

    private List<Transaction> getUnconfirmedTransactions() {
        // Retrieve transactions from the pool of unconfirmed transactions.
        // This would interact with a data structure that holds all the transactions
        // that have been broadcast to the network but not yet confirmed in a block.

        System.out.println("getting mempool tx:");
        return TransactionPool.getInstance().getUnconfirmedTransactions();
    }


    // Modify the run method
    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            startMining(); // Exit the loop if an exception occurs or running is false
        }
    }

    // Method to stop mining
    public void stopMining() {
        running = false;
    }

    
    

}


