/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Networking.Datagrasms;

import java.io.Serializable;

/**
 *
 * @author nick
 */
public class ChainLengthResponse implements Serializable {
    private final long chainLength;

    public ChainLengthResponse(long chainLength) {
        this.chainLength = chainLength;
    }

    public long getChainLength() {
        return chainLength;
    }
}

