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
public class ChainLengthRequest implements Serializable {

    private final long mysizeis;
    public ChainLengthRequest(long mysize) {
        this.mysizeis = mysize;
    }
    public long getRequesterSize() {
        return this.mysizeis;
    }
}
