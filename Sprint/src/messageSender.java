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
                switch (entity.getType()) {
                    case NodeType.LEADER -> sendLeaderHeartbeat();
                    case NodeType.CANDIDATE -> sendVoteRequest();
                    case NodeType.NEW -> sendNewFollowerBroadcast();
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

    private void sendLeaderHeartbeat() throws IOException {
        HashMap<String, String> content = entity.generateHeartbeatContent();
        if (content.isEmpty()) return;

        Heartbeat heartbeat = new Heartbeat(MessageType.SYNC, content);
        sendSerializedMessage(heartbeat);
        System.out.println("Leader heartbeat sent: " + content);
    }

    private void sendVoteRequest() throws IOException {
        HashMap<String, String> content = entity.generateHeartbeatContent();
        if (content.isEmpty()) return;

        Heartbeat voteRequest = new Heartbeat(MessageType.REQUEST_VOTE, content);
        sendSerializedMessage(voteRequest);
        System.out.println("Vote request sent: " + content);
    }

    private void sendNewFollowerBroadcast() throws IOException {
        HashMap<String, String> content = new HashMap<>();
        content.put("followerId", entity.getId().toString());
        content.put("status", "new");

        Heartbeat newFollowerMessage = new Heartbeat(MessageType.NEWELEMENT, content);
        sendSerializedMessage(newFollowerMessage);
        System.out.println("New follower broadcast sent.");
    }

    private void sendSerializedMessage(Heartbeat message) throws IOException {
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
