import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class messageSender extends Thread {
    protected MulticastSocket socket = null;
    private InetAddress group;
    private MessageList messageList;

    public messageSender(MessageList messageList) throws IOException {
        this.messageList = messageList;
        group = InetAddress.getByName("230.0.0.0");
        socket = new MulticastSocket(4446);
        socket.joinGroup(group);
        this.start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                HashMap<Integer,String> messages = messageList.getClone();
                messageList.clear();

                if (messages.isEmpty()) {
                    System.out.println("No messages to send.");
                } else {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(byteStream);
                    oos.writeObject(messages);
                    oos.flush();
                    byte[] serializedMsgs = byteStream.toByteArray();

                    DatagramPacket packet = new DatagramPacket(serializedMsgs, serializedMsgs.length, group, 4446);
                    socket.send(packet);
                    System.out.println("Sent messages: " + messages);
                }

                Thread.sleep(5000);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            socket.close();
        }
    }
}
