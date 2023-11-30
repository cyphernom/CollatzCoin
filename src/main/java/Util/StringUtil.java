package Util;


import Transaction.Transaction;
import Wallet.Wallet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import static org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters.KEY_SIZE;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nick
 */
public class StringUtil {
        private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private static final int KEY_SIZE = 128;  // AES key size in bits
    //Applies Sha256 to a string and returns the result. 
    public static String applySha256(String input){		
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");	        
            //Applies sha256 to our input, 
            byte[] hash = digest.digest(input.getBytes("UTF-8"));	        
            StringBuilder hexString = new StringBuilder(); // This will contain hash as hexadecimal
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    //Applies ECDSA Signature and returns the result ( as bytes ).
    public static byte[] applyECDSASig(PrivateKey privateKey, String input) {
        Signature dsa;
        byte[] output = new byte[0];
        try {
            dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privateKey);
            byte[] strByte = input.getBytes();
            dsa.update(strByte);
            byte[] realSig = dsa.sign();
            output = realSig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    //Verifies a String signature 
    public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
        try {
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        }catch(InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    //Tacks in array of transactions and returns a merkle root.
    public static String getMerkleRoot(ArrayList<Transaction> transactions) {
        int count = transactions.size();
        ArrayList<String> previousTreeLayer = new ArrayList<>();
        for(Transaction transaction : transactions) {
            previousTreeLayer.add(transaction.getTransactionId());
        }
        ArrayList<String> treeLayer = previousTreeLayer;
        while(count > 1) {
            treeLayer = new ArrayList<>();
            for(int i=1; i < previousTreeLayer.size(); i++) {
                treeLayer.add(applySha256(previousTreeLayer.get(i-1) + previousTreeLayer.get(i)));
            }
            count = treeLayer.size();
            previousTreeLayer = treeLayer;
        }
        String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0) : "";
        return merkleRoot;
    }

    //Gets a string from key
    public static String getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String getAddressFromKey(PublicKey publicKey) {
        try {
                        Security.addProvider(new BouncyCastleProvider());
            // Step 1: Perform a SHA-256 hash on the encoded public key
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] sha256hash = sha.digest(publicKey.getEncoded());

            // Step 2: Perform a RIPEMD-160 hash on the SHA-256 hash
            MessageDigest rmd = MessageDigest.getInstance("RipeMD160", "BC");
            byte[] rmd160hash = rmd.digest(sha256hash);

            // Step 3: Add version byte in front (0x00 for Bitcoin Main Network)
            byte[] versionedPayload = new byte[1 + rmd160hash.length];
            versionedPayload[0] = 0;
            System.arraycopy(rmd160hash, 0, versionedPayload, 1, rmd160hash.length);

            // Step 4: Perform SHA-256 hash twice on the versioned payload
            byte[] doubleSHA = sha.digest(sha.digest(versionedPayload));

            // Step 5: Take the first 4 bytes of the double SHA-256 hash as checksum
            byte[] checksum = new byte[4];
            System.arraycopy(doubleSHA, 0, checksum, 0, 4);

            // Step 6: Append checksum to the versioned payload
            byte[] binaryAddress = new byte[versionedPayload.length + checksum.length];
            System.arraycopy(versionedPayload, 0, binaryAddress, 0, versionedPayload.length);
            System.arraycopy(checksum, 0, binaryAddress, versionedPayload.length, checksum.length);

            // Step 7: Convert the result to a Base58 string
            return toBase58(binaryAddress);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Error creating wallet address from public key", e);
        }
    }
    // A simple method to convert a BigInteger to a base58 string
    public static String toBase58(byte[] input) {
        final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
        // Convert the byte array to a positive BigInteger
        BigInteger num = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();

        // Divide the number by 58 and keep track of the remainder
        while (num.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = num.divideAndRemainder(BigInteger.valueOf(58));
            sb.insert(0, ALPHABET[divmod[1].intValue()]); // Append the character corresponding to the remainder
            num = divmod[0];
        }

        // Add '1' characters for each leading zero byte
        for (int i = 0; i < input.length && input[i] == 0; i++) {
            sb.insert(0, '1');
        }

        return sb.toString();
    }
    
  public static void saveWalletToFile(Wallet wallet, String walletName, String password) {
        try {
            SecretKey key = generateSecretKey(password);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(new CipherOutputStream(byteArrayOutputStream, cipher))) {
                oos.writeObject(wallet);
            }
            byte[] encryptedData = byteArrayOutputStream.toByteArray();
            Files.write(Paths.get(walletName), encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Wallet loadWalletFromFile(String walletName, String password) {
        try {
            SecretKey key = generateSecretKey(password);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encryptedData = Files.readAllBytes(Paths.get(walletName));
            try (ObjectInputStream ois = new ObjectInputStream(new CipherInputStream(new ByteArrayInputStream(encryptedData), cipher))) {
                return (Wallet) ois.readObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SecretKey generateSecretKey(String password) throws Exception {
        // Use a password-based key derivation function
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), new byte[16], 65536, KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }
}
