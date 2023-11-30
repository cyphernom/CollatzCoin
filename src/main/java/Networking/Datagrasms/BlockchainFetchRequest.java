/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Networking.Datagrasms;

/**
 *
 * @author nick
 */
import java.io.Serializable;

/**
 * Represents a request for a specific range of blocks in the blockchain.
 */
public class BlockchainFetchRequest implements Serializable {
    private final long startBlockHeight; // The starting height of the blocks being requested
    private final long maxBlocks;         // Maximum number of blocks to fetch
    private final String requestId;      // A unique identifier for the request

    /**
     * Constructs a BlockchainFetchRequest.
     *
     * @param startBlockHeight The height from where to start fetching blocks.
     * @param maxBlocks The maximum number of blocks to fetch.
     * @param requestId A unique identifier for the request.
     */
    public BlockchainFetchRequest(long startBlockHeight, long maxBlocks, String requestId) {
        this.startBlockHeight = startBlockHeight;
        this.maxBlocks = maxBlocks;
        this.requestId = requestId;
    }

    /**
     * Gets the starting block height for the fetch request.
     *
     * @return The starting block height.
     */
    public long getStartBlockHeight() {
        return startBlockHeight;
    }

    /**
     * Gets the maximum number of blocks to fetch.
     *
     * @return The maximum number of blocks.
     */
    public long getMaxBlocks() {
        return maxBlocks;
    }

    /**
     * Gets the unique identifier for this request.
     *
     * @return The request ID.
     */
    public String getRequestId() {
        return requestId;
    }
}

