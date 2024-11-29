import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class messageSender extends Thread {
    protected MulticastSocket socket = null;
    private InetAddress group;
    private MessageList messageList;
    private Entity entity;

    public messageSender(MessageList messageList, Entity entity) throws IOException {
        this.messageList = messageList;
        this.entity = entity;
        group = InetAddress.getByName("230.0.0.0");
        socket = new MulticastSocket(4446);
        socket.joinGroup(group);
        this.start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                sendHeartbeat();

                Thread.sleep(HeartbeatTime.TIME.getValue());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            socket.close();
        }
    }

    private void sendHeartbeat() throws IOException {
        HashMap<String, String> content = entity.generateHeartbeatContent(); // Fetch content based on state
        if (content == null || content.isEmpty()) {
            System.out.println("No heartbeat content to send.");
            return;
        }

        // Construct the heartbeat
        MessageType type = entity.getHeartbeatType(); // Determine the type of heartbeat
        Heartbeat heartbeat = new Heartbeat(type, content);

        // Serialize the heartbeat
        byte[] serializedMsgs = serialize(heartbeat);

        // Send the heartbeat
        DatagramPacket packet = new DatagramPacket(serializedMsgs, serializedMsgs.length, group, 4446);
        socket.send(packet);
        System.out.println("Sent heartbeat: " + type + " with content: " + content);
    }

    private byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(byteStream)) {
            oos.writeObject(obj);
            oos.flush();
            return byteStream.toByteArray();
        }
    }

}
