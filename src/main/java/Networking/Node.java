/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Networking;

import Networking.Datagrasms.ChainLengthResponse;
import Networking.Datagrasms.BlockchainFetchRequest;
import Networking.Datagrasms.NodeInfo;
import Networking.Datagrasms.ChainLengthRequest;
import Blockchain.Blockchain;
import Blockchain.Block;
import Transaction.Transaction;
import Blockchain.TransactionPool;
import Blockchain.UTXOPool;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author nick
 */
public class Node implements Runnable {
    private final ServerSocket serverSocket;
    private final Set<Socket> connections = ConcurrentHashMap.newKeySet();
    private final Blockchain blockchain;
    private final int port;
    private static final long MAX_BLOCKS_PER_REQUEST = 1000000000;
    private final Map<Socket, ObjectOutputStream> outputStreams = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());
    private final String address;
    private List<BlockAddedListener> listeners = new CopyOnWriteArrayList<>();
   
   public void addBlockAddedListener(BlockAddedListener listener) {
        listeners.add(listener);
    }
    private void notifyBlockAdded(Block block) {
        for (BlockAddedListener listener : listeners) {
            listener.onBlockAdded(block);
        }
    }   
    public Node(String address, int port, String file) throws IOException {
        this.blockchain =  new Blockchain(1,600000,2016, file, this); 
        this.port = port;
       this.serverSocket = new ServerSocket(port);
        this.address = address;
        Future<?> submit = executorService.submit(this); // never used???

        // Schedule initial synchronization
        scheduler.schedule(this::attemptSynchronization, 5, TimeUnit.SECONDS);
    }
    private void attemptSynchronization() {
        // Trigger initial synchronization if blockchain is empty or needs synchronization
        LOGGER.info("Starting attemptSynchronization");
        if (this.blockchain.getChain().size()<=1|| needsSynchronization()) {
            try {
                LOGGER.info("Attempting initial synchronization");

                // Check if there are any known nodes (existing connections)
                List<String> knownNodes = getKnownNodes(this.address, this.port);
                if (knownNodes.isEmpty()) {
                    // If no known nodes, use seed nodes to establish initial connections
                    List<String> seedNodes = getSeedNodes(this.address, this.port);
                    for (String nodeAddress : seedNodes) {
                        connectToNode(nodeAddress);
                    }
                }

                // Synchronize blockchain with peers
                synchronizeBlockchain();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error during initial synchronization", ex);
            }
        }

        // Schedule periodic synchronization
        scheduler.scheduleAtFixedRate(this::synchronizeIfNeeded, 0, 5, TimeUnit.SECONDS);
        LOGGER.info("Finished attemptSynchronization");
    }

    private void connectToNode(String nodeAddress) {
        // Implementation for connecting to a node
        // Parse the nodeAddress into IP and port
        LOGGER.log(Level.INFO, "Connecting to node: {0}", nodeAddress);
        String[] parts = nodeAddress.split(":");
        if (parts.length != 2) {
            LOGGER.log(Level.WARNING, "Invalid node address: {0}", nodeAddress);
            return;
        }
        String ip = parts[0];
        int portf = Integer.parseInt(parts[1]);

        Socket socket = null;
        try {
            socket = new Socket(ip, portf);
            connections.add(socket);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            outputStreams.put(socket, out);

            LOGGER.log(Level.INFO, "Connected to seed node: {0}", nodeAddress);
            broadcastNode(this.address, this.port);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to connect to node: " + nodeAddress, e);
            closeQuietly(socket);  // Close socket if an exception occurs
        }
    }


    private void synchronizeIfNeeded() {
        if (needsSynchronization()) {
            LOGGER.info("Scheduled sync");
            try {
                synchronizeBlockchain();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean needsSynchronization() {
        long lcfp = getLongestChainLengthFromPeers();
        LOGGER.log(Level.INFO, "Checking sync: longest chain from peers: {0}, this node''s length: {1}", new Object[]{lcfp, blockchain.getChain().size()});
        return blockchain.getTotalWork() < lcfp;
    }

    private List<String> getSeedNodes(String excludeAddress, int excludePort) {
        // Return a list of known nodes (IP addresses). This could be hardcoded or dynamically discovered.
        List<String> nodes = new ArrayList<>(Arrays.asList("127.0.0.1:5000"));
        nodes.removeIf(node -> node.equals(excludeAddress + ":" + excludePort));
        return nodes;
    }

    private List<String> getKnownNodes(String excludeAddress, int excludePort) {
        System.out.println("getting nodes");
        List<String> nodes = new ArrayList<>();
        for (Socket socket : connections) {
            if (!socket.isClosed()) {
                String peerAddress = socket.getInetAddress().getHostAddress();
                int peerPort = socket.getPort();
                if (!(peerAddress.equals(excludeAddress) && peerPort == excludePort)) {
                    nodes.add(peerAddress + ":" + peerPort);
                    System.out.println("added node:"+peerAddress+":"+peerPort);
                }
            }
        }
        return nodes;
    }


    
    
    private long getLongestChainLengthFromPeers() {
        LOGGER.info("Starting getLongestChainLengthFromPeers");
        List<String> peers = getKnownNodes(this.address, this.port); // Your method to get peer addresses
        long longestChainLength = blockchain.getTotalWork(); // Start with the current local chain length

        for (String peerAddress : peers) {
            try {
                
                String[] parts = peerAddress.split(":");
                String ip = parts[0];
                int portf = Integer.parseInt(parts[1]);
                System.out.println("attempting to get tip info from:" +portf);
                try (Socket socket = new Socket(ip, portf);
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                    // Send chain length request
                    out.writeObject(new ChainLengthRequest(longestChainLength));
                    out.flush();

                    // Wait for response
                    Object response = in.readObject();
                    if (response instanceof ChainLengthResponse chainLengthResponse) {
                        long peerChainLength = chainLengthResponse.getChainLength();
                        if (peerChainLength > longestChainLength) {
                            longestChainLength = peerChainLength;
                        }
                    }

                } catch (IOException | ClassNotFoundException e) {
                    LOGGER.log(Level.INFO, "class not found exception{0}", e.toString());
                }
            } catch (Exception e) {
                    LOGGER.log(Level.INFO, "exception getting ip etc: {0}", e.toString());
            }
        }
            LOGGER.info("Longest chain length from peers: " + longestChainLength);
        return longestChainLength;
    }
    private void synchronizeBlockchain() throws ClassNotFoundException {
        int maxRetries = 5;
        long retryDelayMillis = 5000; // 5 seconds
        List<String> knownNodes = getKnownNodes(this.address, this.port); 

        System.out.println("In synchronize method");
        for (String nodeAddress : knownNodes) {
            String[] parts = nodeAddress.split(":");
            String ip = parts[0];
            int serverPort = Integer.parseInt(parts[1]);

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                Socket socket = null;
                try {
                    socket = new Socket(ip, serverPort);
                    long startBlockHeight = blockchain.getChain().size();
                    System.out.println("Sending fetch request to node " + serverPort);

                    sendBlockchainFetchRequest(socket, startBlockHeight);
                    List<Block> newBlocks = receiveBlockchainData(socket);

                    if (!newBlocks.isEmpty()) {
                        System.out.println("Processing received blocks");
                        processReceivedBlocks(newBlocks);
                        System.out.println("Saving blockchain locally");
                        blockchain.saveToFile();
                        break; // Exit loop after successful synchronization
                    }
                } catch (IOException e) {
                    System.out.println("Failed to connect or synchronize with node " + nodeAddress + ": " + e.getMessage());
                    // Optionally log the exception or handle it further
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ex) {
                            Logger.getLogger(Node.class.getName()).log(Level.SEVERE, "Failed to close socket", ex);
                        }
                    }
                }

                if (attempt < maxRetries) {
                    System.out.println("Retrying (" + attempt + "/" + maxRetries + ") for node " + nodeAddress);
                    try {
                        Thread.sleep(retryDelayMillis);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Node.class.getName()).log(Level.SEVERE, "Retry interrupted", ex);
                        Thread.currentThread().interrupt(); // Restore interrupted status
                    }
                }
            }
        }
    }



    public void broadcastNewBlock(Block block) {
        blockchain.addBlock(block);
        broadcastObjectToAllConnections(block);
    }

    public void broadcastTransaction(Transaction transaction) {
        TransactionPool.getInstance().addTransaction(transaction);
        broadcastObjectToAllConnections(transaction);
    }


    
    public void broadcastNode(String ip, int port) {
        NodeInfo n = new NodeInfo(ip, port);
        broadcastObjectToAllConnections(n);
    }
    
   
    private void broadcastObjectToAllConnections(Serializable object) {
        for (Socket socket : new HashSet<>(connections)) {
            try {
                ObjectOutputStream out = outputStreams.get(socket);
                if (out != null) {
                    out.writeObject(object);
                    out.flush();
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to broadcast to socket: " + socket, e);
                closeSocketAndRemoveFromCollections(socket);
            }
        }
    }

    
    
    
    
    public void shutdown() {
        try {
            LOGGER.info("Shutting down Node...");
            serverSocket.close(); // Stop accepting new connections

            // Shut down executor service
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                        LOGGER.severe("Executor service did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Shut down scheduler
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    LOGGER.severe("Scheduler did not terminate");
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error shutting down Node", e);
        }
    }

    private void closeSocketAndRemoveFromCollections(Socket socket) {
        ObjectOutputStream out = outputStreams.remove(socket);
        connections.remove(socket);
        closeQuietly(out);
        closeQuietly(socket);
    }

    private void closeQuietly(Closeable resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to close resource", e);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ClientHandler(clientSocket));
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                }
                break;
            }
        }

        // Clean up resources
        closeQuietly(serverSocket);
        executorService.shutdown();
        scheduler.shutdown();
    }
    public void saveBlockchainToFile(String filepath) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filepath))) {
            out.writeObject(blockchain);
        } catch (IOException e) {
        }
    }

    public Blockchain loadBlockchainFromFile(String filepath, Blockchain blockchain) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filepath))) {
            return (Blockchain) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
           e.printStackTrace();
        }
        throw new Exception("Blockchain not instantiated");
    }
    
private long findStartBlockHeight(Block newBlock) {
    // Iterate backwards through the current blockchain
    // Start from the latest block and move towards the genesis block
    List<Block> currentChain = blockchain.getChain();
    for (int i = currentChain.size() - 1; i >= 0; i--) {
        Block currentBlock = currentChain.get(i);

        // Compare the current block in the chain with the new block
        if (newBlock.getPreviousHash().equals(currentBlock.getHash())) {
            // Found the last common ancestor block
            return i + 1; // Return the height of the block after the common ancestor
        }
    }

    // If no common ancestor is found, it means the new block is on a completely different chain
    // In a real-world scenario, this might indicate a major issue or an attack
    // Depending on your blockchain's policy, you may handle this case differently
    // For now, we'll log a warning and return the current chain size
    LOGGER.warning("No common ancestor found for the new block. This might indicate a different chain or an attack.");
    return currentChain.size();
}

private synchronized List<Block> fetchForkedChain(Block newBlock) {
    List<Block> forkedChain = new ArrayList<>();
    long startBlockHeight = findStartBlockHeight(newBlock);
    LOGGER.info("fetching forked chain");
    // Request blocks from peers starting from startBlockHeight
    List<String> knownNodes = getKnownNodes(this.address, this.port);
    for (String nodeAddress : knownNodes) {
        try {
            String[] parts = nodeAddress.split(":");
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);

            try (Socket socket = new Socket(ip, port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Send fetch request
                BlockchainFetchRequest request = new BlockchainFetchRequest(startBlockHeight, MAX_BLOCKS_PER_REQUEST, generateRequestId());
                out.writeObject(request);
                out.flush();

                // Receive response
                Object response = in.readObject();
                if (response instanceof List<?> responseData && (!responseData.isEmpty() && responseData.get(0) instanceof Block)) {
                    List<Block> receivedBlocks = (List<Block>) responseData;
                    forkedChain.addAll(receivedBlocks);
                    break; // Exit after successful fetch
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to fetch forked chain from " + nodeAddress, e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in processing node address: " + nodeAddress, e);
        }
    }

    return forkedChain;
}

    private void sendBlockchainFetchRequest(Socket socket, long startBlockHeight) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        BlockchainFetchRequest request = new BlockchainFetchRequest(startBlockHeight, MAX_BLOCKS_PER_REQUEST, generateRequestId());
        out.writeObject(request);
        out.flush();
    }
    private List<Block> receiveBlockchainData(Socket socket) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        Object response = in.readObject();
        if (response instanceof List<?> responseData && (!responseData.isEmpty() && responseData.get(0) instanceof Block)) {
            return (List<Block>) responseData;
        }
        return new ArrayList<>();
    }
    private void processReceivedBlocks(List<Block> newBlocks) {
        for (Block block : newBlocks) {
            if (isValidBlock(block) && !blockchain.getChain().contains(block)) {
                blockchain.addBlock(block);
            }
        }
       
    }
    
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
    
     private boolean isValidBlock(Block block) {
        return blockchain.isValidBlock(block);
    }

    public UTXOPool getUTXOPool() {
       return blockchain.getUTXOPool();
    }

    public long getTotalCoins() {
        return  blockchain.getTotalCoins();
    }

    public Blockchain getBlockchain() {
        return this.blockchain;
    }

    public int getDifficulty() {
        return this.blockchain.getDifficulty();
    }
        
    private class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                outputStreams.put(socket, out);
                //connections.add(socket); 
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error creating ObjectOutputStream", e);
                closeSocketAndRemoveFromCollections(socket);
            }
        }



        @Override
       public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
               ObjectOutputStream out = outputStreams.get(clientSocket);

               outputStreams.put(clientSocket, out);

               while (!Thread.currentThread().isInterrupted()) {
                   Object object = in.readObject();
                   System.out.println("Received an object: " + object.toString());

                   if (object instanceof Transaction transaction) {
                       handleIncomingTransaction(transaction);
                   } else if (object instanceof Block block) {
                       handleIncomingBlock(block);
                   } else if (object instanceof ChainLengthRequest clr) {
                       handleChainLengthRequest(clr, out);
                   } else if (object instanceof NodeInfo n) {
                       handleNodeInfo(n);
                   }
               }
           } catch (IOException | ClassNotFoundException e) {
               LOGGER.log(Level.SEVERE, "Error in client handler", e);
           } finally {
               closeSocketAndRemoveFromCollections(clientSocket);
           }
       }
        private void handleChainLengthRequest(ChainLengthRequest clr, ObjectOutputStream out) {
            try {
                System.out.println("Received ChainLengthRequest");
                // Prepare and send the response
                ChainLengthResponse response = new ChainLengthResponse(blockchain.getChain().size());
                out.writeObject(response);
                out.flush();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error responding to ChainLengthRequest", e);
            }
        }
        private boolean handleIncomingTransaction(Transaction transaction) {
            if (transaction.isValid() && !blockchain.getTransactionPool().getUnconfirmedTransactions().contains(transaction)) {
                blockchain.addTransactionToPool(transaction);
                return true;
            }
                return false;
        }
        
        private synchronized boolean handleIncomingBlock(Block block) {
            System.out.println("Received a new block.");
            if (block != null && isValidBlock(block)) {
                
                boolean added = blockchain.addBlock(block);

                
                
                if (!added) {
                    LOGGER.info("block is part of a fork, handling it.");
                    handlePotentialFork(block);
                } else {
                    // The block extends the current longest work chain
                    LOGGER.info("block is on the longest work chain");
                    notifyBlockAdded(block);
                }
                return added;
            } 
            return false;
        }
    private synchronized void handlePotentialFork(Block newBlock) {
        // Fetch the forked chain starting from the new block
        List<Block> forkedChain = fetchForkedChain(newBlock);

        // Calculate total work for the forked chain
        long forkedChainWork = calculateTotalWork(forkedChain);

        // Compare the total work of the forked chain with the current chain
        if (forkedChainWork > blockchain.getTotalWork()) {
            // The forked chain has more work, switch to it
            blockchain.reorganizeChain(forkedChain);
            for (Block block : forkedChain) {
                notifyBlockAdded(block); // Notify listeners about each new block
            }
        } else {
            // The current chain has more work, ignore the fork
            LOGGER.info("Ignoring forked block as it has less total work.");
        }
    }
    
    





    private long calculateTotalWork(List<Block> chain) {
        // Calculate the total work of a given chain
        long totalWork = 0;
        for (Block block : chain) {
            totalWork += blockchain.calculateBlockWork(block); // Ensure calculateBlockWork is accessible here
        }
        return totalWork;
    }
    
    
    
        private boolean isAlreadyConnected(String ip, int port) {
            for (Socket socket : connections) {
                if (socket.getInetAddress().getHostAddress().equals(ip) && socket.getPort() == port) {
                    return true;
                }
            }
            return false;
        }

    public void handleNodeInfo(NodeInfo nodeInfo) {
        if (!isAlreadyConnected(nodeInfo.getIp(), nodeInfo.getPort())) {
            Socket newSocket = null;
            try {
                newSocket = new Socket(nodeInfo.getIp(), nodeInfo.getPort());
                connections.add(newSocket);

                ObjectOutputStream out = new ObjectOutputStream(newSocket.getOutputStream());
                outputStreams.put(newSocket, out);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to connect to new node: " + nodeInfo.getIp() + ":" + nodeInfo.getPort(), e);
                closeQuietly(newSocket);
            }
        } else {
            LOGGER.info("Connection already exists: " + nodeInfo.getIp() + ":" + nodeInfo.getPort());
        }
    }


    }
    // Method to check if node has active connections
    public boolean hasActiveConnections() {
        return !connections.isEmpty();
    }

}