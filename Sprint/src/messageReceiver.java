import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;

public class messageReceiver extends Thread {
    protected byte[] buf = new byte[4096];
    protected MulticastSocket socket = null;
    private InetAddress group;
    private MessageList messageList;
    private Entity entity;

    public messageReceiver(MessageList messageList, Entity entity) throws IOException {
        this.messageList = messageList;
        socket = new MulticastSocket(4446);
        group = InetAddress.getByName("230.0.0.0");
        socket.joinGroup(group);
        this.entity = entity;
        this.start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                byte[] buffer = new byte[4096];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Heartbeat message = deserialize(packet.getData());
                entity.processMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Heartbeat deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (Heartbeat) ois.readObject();
        }
    }

}

