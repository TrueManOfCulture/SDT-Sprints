import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.HashMap;

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
                switch (entity.getType()) {
                    case NodeType.LEADER -> sendLeaderHeartbeat();
                }
                Thread.sleep(HeartbeatTime.TIME.getValue());
            }

        } catch (IOException |
                 InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            socket.close();
        }
    }

    private synchronized void sendLeaderHeartbeat() throws IOException {
        HashMap<String, String> content = entity.generateHeartbeatContent();
        Heartbeat heartbeat;
        if (messageList.isEmpty()) {
            heartbeat = new Heartbeat(MessageType.LEADERHEARTBEAT, content);
        }
        else{
            heartbeat = new Heartbeat(MessageType.SYNC, content);
        }

        sendSerializedMessage(heartbeat);
        System.out.println("Leader heartbeat sent: " + content);
    }



    void sendSerializedMessage(Heartbeat message) throws IOException {
        byte[] serializedMsg = serialize(message);
        DatagramPacket packet = new DatagramPacket(serializedMsg, serializedMsg.length, group, 4446);
        socket.send(packet);
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
