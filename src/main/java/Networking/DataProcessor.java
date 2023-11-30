package Networking;

import Networking.Datagrasms.ChainLengthResponse;
import Networking.Datagrasms.NodeInfo;
import Networking.Datagrasms.ChainLengthRequest;
import Networking.AbstractNetworkingToolkit.NetworkHandler;
import Blockchain.Block;
import Blockchain.Blockchain;
import Blockchain.TransactionPool;
import Transaction.Transaction;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
// Import other necessary classes

public class DataProcessor implements IDataProcessor {
    private final Blockchain blockchain;
    private static final Logger LOGGER = Logger.getLogger(DataProcessor.class.getName());
    private  NetworkHandler networkHandler;

    public DataProcessor(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public void processData(Object data) {
        if (data instanceof Transaction transaction) {
            processTransaction(transaction);
        } else if (data instanceof Block block) {
            processBlock(block);
        } else if (data instanceof ChainLengthRequest chainLengthRequest) {
            processChainLengthRequest(chainLengthRequest);
        } else if (data instanceof ChainLengthResponse chainLengthResponse) {
            processChainLengthResponse(chainLengthResponse);
        } else if (data instanceof NodeInfo nodeInfo) {
            processNodeInfo(nodeInfo);
        } else if(data instanceof AIRequest aiRequest) {
            processAIRequest(aiRequest);
        }
    }

    private void processBlock(Block block) {
        blockchain.addBlock(block);
        try {
            this.networkHandler.broadcast (block);
        } catch (IOException ex) {
            Logger.getLogger(DataProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void processTransaction(Transaction transaction) {
        TransactionPool.getInstance().addTransaction(transaction);
        try {
            networkHandler.broadcast(transaction);
        } catch (IOException ex) {
            Logger.getLogger(DataProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    
    public void broadcastNode(String ip, int port) {
        NodeInfo n = new NodeInfo(ip, port);
        try {
            networkHandler.broadcast(n);
        } catch (IOException ex) {
            Logger.getLogger(DataProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    

    private void processChainLengthRequest(ChainLengthRequest clr) {
        ChainLengthResponse response = new ChainLengthResponse(blockchain.getChain().size());
        networkHandler.sendChainLengthRespone(response);
     }

    private void processNodeInfo(NodeInfo nodeInfo) {
        networkHandler.addNode(nodeInfo);
    }

    // Additional methods for other data types

    private void processChainLengthResponse(ChainLengthResponse chainLengthResponse) {
        long longestChainLength = blockchain.getChain().size(); // Start with the current local chain length

        long peerChainLength = chainLengthResponse.getChainLength();
        if (peerChainLength > longestChainLength) {
          synchronizeBlockchain();
        }
    }
    
    public void synchronizeBlockchain() {
        long localChainLength = blockchain.getChain().size();
        networkHandler.requestBlockchainData( localChainLength);
    }

    public void processReceivedBlockchainData(List<Block> newBlocks) {
        if (!newBlocks.isEmpty()) {
            for (Block block : newBlocks) {
                if (blockchain.isValidBlock(block) && !blockchain.getChain().contains(block)) {
                    blockchain.addBlock(block);
                }
            }
            blockchain.saveToFile(); // Assuming this is a method to save the blockchain state
        }
    }

    void setNetworkHandler(NetworkHandler networkHandler) {
       this.networkHandler = networkHandler;
    }

    private void processAIRequest(AIRequest aiRequest) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }


    
    
}


