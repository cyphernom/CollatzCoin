/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package Networking.AbstractNetworkingToolkit;

import java.net.Socket;

/**
 *
 * @author nick
 */
public interface INetworkReceiver {
    void onDataReceived(Object data);
    void onConnectionEstablished(Socket clientSocket);
    void onConnectionClosed(String ip, int port);
}