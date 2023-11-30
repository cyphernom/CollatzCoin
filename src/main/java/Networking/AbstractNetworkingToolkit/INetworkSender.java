/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package Networking.AbstractNetworkingToolkit;

import java.io.IOException;

/**
 *
 * @author nick
 */

public interface INetworkSender {
    void send(String ip, int port, Object data) throws IOException;
}
