/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package Networking;

import Blockchain.Block;

/**
 *
 * @author nick
 */
public interface BlockAddedListener {
    void onBlockAdded(Block block);
}

