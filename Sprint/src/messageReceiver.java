import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

public class messageReceiver extends Thread {
    protected byte[] buf = new byte[4096];
    protected MulticastSocket socket = null;
    private InetAddress group;
    private UUID id, idLeader;
    private Entity entity;

    public messageReceiver(UUID id, UUID idLeader, Entity entity) throws IOException {
        this.id = id;
        this.idLeader = idLeader;
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
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                ObjectInputStream objStream = new ObjectInputStream(byteStream);
                Heartbeat message = (Heartbeat) objStream.readObject();

                entity.processMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                socket.leaveGroup(group);
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket.close();
        }
    }

    public void sendAckToLeader(int leaderId, String message) {
        /*int leaderPort = 6000 + leaderId;

        try (Socket socket = new Socket("localhost", leaderPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            // Send the ACK
            oos.writeObject(message);
            System.out.println("Entity " + id + " sent ACK: " + message);

        } catch (IOException e) {
            System.err.println("Error sending ACK to Leader " + leaderId + ": " + e.getMessage());
        }*/
    }


}

