package View;


import Blockchain.Blockchain;
import Mining.Mining;
import Networking.Node;
import Wallet.Wallet;
import Transaction.Transaction;
import Util.StringUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import java.security.spec.KeySpec;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */

public class MainClass {

    private Wallet wallet;

    private Node node;
    private Mining mining;

    private boolean exitMainMenu;

  
    
    public MainClass(int port, String file)  {
   try {
            // Initialize the node for networking, replace 8080 with your chosen port
            node = new Node("127.0.0.1", port, file);
            new Thread(node).start(); // Start the networking thread

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    
    private Wallet createNewWallet() {
        Wallet newWallet = new Wallet();
        String walletName = "wallet_" + System.currentTimeMillis() + ".dat";
        System.out.println("New wallet created: " + walletName);
        return newWallet; // Return the new wallet
    }


    private void saveWalletToFile(Wallet wallet, String walletName) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(walletName))) {
            oos.writeObject(wallet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Wallet loadWalletFromFile(String walletName) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(walletName))) {
            return (Wallet) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void listAvailableWallets() {
        try (Stream<Path> paths = Files.walk(Paths.get("./"))) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().startsWith("./wallet_"))
                 .forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void displayMenu() {
        wallet.setUTXOPool(node.getUTXOPool());

        System.out.println("\nChoose an option:");
        System.out.println("1: Mine");
        System.out.println("2: Check Balance");
        System.out.println("3: Receive");
        System.out.println("4: Send");
        System.out.println("5: Audit Total Supply");
        System.out.println("6: List transactions");
        System.out.println("7: Exit to menu");
        System.out.println("8: Exit Application");
        System.out.print("Enter your choice: ");
    }

    private void handleMenu(int choice) {
        switch (choice) {
            case 1:
                startMine();
                break;
            case 2:
                checkBalance();
                break;
            case 3:
                receive();
                break;
            case 4:
                send();
                break;
            case 5:
                auditTotalSupply();
                break;
            case 6:
                getTXList();
                break;
            case 7:
                System.out.println("Returning to wallet menu...");
                exitMainMenu = true;
                break;
            case 8:
                shutdownApplication();
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }
    private void shutdownApplication() {
        if (node != null) {
            node.shutdown();
        }
        System.out.println("Application shutting down...");
        System.exit(0);
    }
    private void checkBalance() {
        // Implement the logic to check balance
        System.out.println("Balance: " + wallet.getBalance());
    }

    private void receive() {
        try {
            String address = wallet.generateAddress();
            System.out.println("address:\n\n"+address+"\n\n");
        } catch (Exception e) {
          e.printStackTrace();
        }
    }

    private void send() {
        Scanner scanner = new Scanner(System.in);

        // Prompt for recipient's address
        System.out.print("Enter the recipient's address: ");
        String recipientAddress = scanner.nextLine();

        // Prompt for the amount to send
        System.out.print("Enter the amount to send: ");
        long amount = scanner.nextLong();
        scanner.nextLine(); // Consume the remaining newline
        
        wallet.sendFunds(recipientAddress, amount);
        

    }

    private void auditTotalSupply() {
        System.out.println("Total Coins:" + node.getTotalCoins());
    }
    public void startMine() {
    System.out.println("Starting mining...");
    mining = new Mining(node, wallet);
    Thread miningThread = new Thread(mining);
    miningThread.start(); // Start the mining thread1
        
        
        Thread controlThread = new Thread(() -> {
            try {
                while (true) {
                    if (System.in.available() > 0 && System.in.read() == ' ') {
                        mining.stopMining();
                        miningThread.interrupt();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        controlThread.start(); // Start the control thread
        try {
            miningThread.join(); // Wait for the mining thread to finish
            controlThread.interrupt(); // Stop the control thread
            controlThread.join(); // Wait for the control thread to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Prompt for port number
        System.out.print("Enter port number for this node: ");
        int port = scanner.nextInt();
        scanner.nextLine(); // Consume the newline

        // Prompt for blockchain data file name
        System.out.print("Enter the blockchain data file name (e.g., blockchain.dat): ");
        String blockchainFileName = scanner.nextLine();

        MainClass mc = new MainClass(port, blockchainFileName);

        while(true) {
            mc.wallet = null; // Reset the wallet to null
            while (mc.wallet == null) {
                System.out.println("1: Select Wallet");
                System.out.println("2: Create New Wallet");
                System.out.println("3: List Available Wallets");
                int initialChoice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (initialChoice) {
                    case 1:
                        System.out.print("Enter wallet filename: ");
                        String walletName = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String password = scanner.nextLine();
                        mc.wallet = StringUtil.loadWalletFromFile(walletName, password);
                        break;
                    case 2:
                        System.out.print("Enter a password for the new wallet: ");
                        String newPassword = scanner.nextLine();
                        mc.wallet = mc.createNewWallet(); // Update this line
                        String newWalletName = "wallet_" + System.currentTimeMillis() + ".dat";
                        StringUtil.saveWalletToFile(mc.wallet, newWalletName, newPassword);
                        break;
                    case 3:
                        mc.listAvailableWallets();
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            }

            mc.exitMainMenu = false; // Reset the flag
            while (!mc.exitMainMenu) {
                mc.displayMenu();
                int choice = scanner.nextInt();
                mc.handleMenu(choice);
            }
        }
    }

    private void getTXList() {
        List<Transaction> transactions = wallet.getMyTransactions(node.getBlockchain());
        if (transactions.isEmpty()) {
            System.out.println("No transactions found for this wallet.");
        } else {
            System.out.println("Transactions for Wallet Address: " + wallet.generateAddress());
            System.out.println("---------------------------------------------------");
            for (Transaction transaction : transactions) {
                String senderAddress = transaction.getSenderAddress(); // Assuming these methods exist in Transaction class
                String recipientAddress = transaction.getRecipientAddress();
                long value = transaction.getValue();
                String transactionId = transaction.getTransactionId();

                System.out.println("Transaction ID: " + transactionId);
                System.out.println("Sender Address: " + senderAddress);
                System.out.println("Recipient Address: " + recipientAddress);
                System.out.println("Amount: " + value);
                System.out.println("---------------------------------------------------");
            }
        }
    }


}
