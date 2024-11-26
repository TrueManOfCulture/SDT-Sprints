import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class LeaderMessageReceiver extends Thread {
    private int leaderId;
    private MessageList messageList;
    private int port;

    public LeaderMessageReceiver(int leaderId, MessageList messageList) throws IOException {
        this.leaderId = leaderId;
        this.messageList = messageList;
        this.port = 5000 + leaderId; // Assign a port based on leader's ID
        this.start();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Leader " + leaderId + " listening on port " + port);

            while (true) {
                // Accept incoming connections
                try (Socket clientSocket = serverSocket.accept();
                     ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream())) {

                    String receivedMessage = (String) ois.readObject();

                    messageList.addElement(2,receivedMessage);

                    System.out.println("Leader " + leaderId + " received: " + receivedMessage);
                    try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())) {
                        oos.writeObject("Message received by Leader " + leaderId);
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Error processing received message: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error in LeaderMessageReceiver: " + e.getMessage());
        }
    }
}
