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
public     class NodeInfo implements Serializable {
        private final int port;
        private final String ip;
                
        public NodeInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
        
        public String getIp(){
            return this.ip;
        }
        public int getPort() {
            return this.port;
        }
        
    }
