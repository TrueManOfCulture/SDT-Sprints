import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.List;

public class messageSender extends Thread{
    protected byte[] buf = new byte[256];
    DatagramPacket packet;
    protected MulticastSocket socket = null;
    private InetAddress group;
    DataOutputStream out;
    List<String> msgs;

    public messageSender(List<String> msgs) throws IOException {
        this.msgs = msgs;
        group = InetAddress.getByName("230.0.0.0");
        socket = new MulticastSocket(4446);
        socket.joinGroup(group);
        this.start();
    }
    @Override
    public void run() {
        try {
            // Serialize the list of messages
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(byteStream);
            oos.writeObject(msgs);
            oos.flush();
            byte[] serializedMsgs = byteStream.toByteArray();

            // Send the serialized list as a single DatagramPacket
            DatagramPacket packet = new DatagramPacket(serializedMsgs, serializedMsgs.length, group, 4446);
            socket.send(packet);
            System.out.println("Sent all messages in one packet.");

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            socket.close();
        }
    }
}
