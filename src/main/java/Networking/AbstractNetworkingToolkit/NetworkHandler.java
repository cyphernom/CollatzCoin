/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Networking.AbstractNetworkingToolkit;

/**
 *
 * @author nick
 */
import Networking.Datagrasms.NodeInfo;
import Networking.Datagrasms.BlockchainFetchRequest;
import Networking.Datagrasms.ChainLengthResponse;
import Networking.IDataProcessor;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkHandler implements INetworkSender, INetworkReceiver {
    private final ExecutorService executorService;
    private static final Logger LOGGER = Logger.getLogger(NetworkHandler.class.getName());
    private final IDataProcessor dataProcessor; // An interface to process data
    private final Map<NodeInfo, Socket> connections = new ConcurrentHashMap<>();

    private final long MAX_BLOCKS_PER_REQUEST=100000000;

    public NetworkHandler(IDataProcessor dataProcessor) {
        this.executorService = Executors.newCachedThreadPool();
        this.dataProcessor = dataProcessor;
    }

    @Override
    public void send(String ip, int port, Object data) throws IOException {
        NodeInfo nodeInfo = new NodeInfo(ip, port);
        Socket socket = connections.get(nodeInfo);

        if (socket == null || socket.isClosed()) {
            socket = new Socket(ip, port);
            connections.put(nodeInfo, socket);
        }

        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(data);
            out.flush();
        } catch (IOException e) {
            closeQuietly(socket);
            connections.remove(nodeInfo);
            throw e;
        }
    }


    public void listen(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.log(Level.INFO, "Listening on port {0}", port);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    NodeInfo nodeInfo = new NodeInfo(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
                    connections.put(nodeInfo, clientSocket);

                    LOGGER.log(Level.INFO, "Connection established with {0}", clientSocket.getInetAddress().getHostAddress());
                    executorService.execute(() -> handleClientSocket(clientSocket));
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating server socket on port " + port, e);
        }
    }


    private void handleClientSocket(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            Object object;
            while ((object = in.readObject()) != null) {
                dataProcessor.processData(object);
            }
        } catch (EOFException eof) {
            // End of stream, connection closed by client
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error handling client socket", e);
        } finally {
            NodeInfo nodeInfo = new NodeInfo(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
            closeQuietly(clientSocket);
            connections.remove(nodeInfo);
            LOGGER.log(Level.INFO, "Connection closed with {0}", clientSocket.getInetAddress().getHostAddress());
        }
    }


    @Override
    public void onDataReceived(Object data) {
        dataProcessor.processData(data);
    }

    public List<String> getKnownNodes(String excludeAddress, int excludePort) {
        List<String> nodes = new ArrayList<>();
        for (NodeInfo nodeInfo : connections.keySet()) {
            if (!(nodeInfo.getIp().equals(excludeAddress) && nodeInfo.getPort() == excludePort)) {
                nodes.add(nodeInfo.getIp() + ":" + nodeInfo.getPort());
            }
        }
        return nodes;
    }
    


    
    @Override
    public void onConnectionEstablished(Socket clientSocket) {
        String ip = clientSocket.getInetAddress().getHostAddress();
        int port = clientSocket.getPort();
        LOGGER.log(Level.INFO, "Connection established with {0}:{1}", new Object[]{ip, port});
        NodeInfo nodeInfo = new NodeInfo(ip, port);
        connections.put(nodeInfo, clientSocket);
    }




    @Override
    public void onConnectionClosed(String ip, int port) {
        NodeInfo nodeInfo = new NodeInfo(ip, port);
        Socket socket = connections.get(nodeInfo);
        if (socket != null) {
            closeQuietly(socket);
            connections.remove(nodeInfo);
        }
        LOGGER.log(Level.INFO, "Connection closed with {0}:{1}", new Object[]{ip, port});
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
    
    public void requestBlockchainData(long startBlockHeight) {
        BlockchainFetchRequest request = new BlockchainFetchRequest(startBlockHeight, MAX_BLOCKS_PER_REQUEST, generateRequestId());
        try {
            broadcast(request);
        } catch (IOException ex) {
            Logger.getLogger(NetworkHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    public void sendChainLengthRespone(ChainLengthResponse response) {
        try {
            broadcast(response);
        } catch (IOException ex) {
            Logger.getLogger(NetworkHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void broadcast(Object data) throws IOException {
        for (NodeInfo nodeInfo : connections.keySet()) {
            Socket socket = connections.get(nodeInfo);
            if (socket != null && !socket.isClosed()) {
                try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                    out.writeObject(data);
                    out.flush();
                } catch (IOException e) {
                    closeQuietly(socket);
                    connections.remove(nodeInfo);
                    // Optionally, handle the exception, like logging or retrying
                }
            }
        }
    }

    public void addNode(NodeInfo nodeInfo) {
        // Check if the node is already known
        if (!connections.containsKey(nodeInfo)) {
            // Here, you might want to establish a connection immediately,
            // or you could just add the NodeInfo to your connections map
            // and connect later when needed.
            // For immediate connection:
            try {
                Socket socket = new Socket(nodeInfo.getIp(), nodeInfo.getPort());
                connections.put(nodeInfo, socket);
                LOGGER.log(Level.INFO, "New node added: {0}", nodeInfo);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e.toString()+"""
                                                       Failed to connect to new node: {0}
                                                       Error:""", nodeInfo);
                // Handle exception, such as retrying later or logging the error
            }
        }
    }


}

