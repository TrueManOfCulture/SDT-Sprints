import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.util.List;

public class messageReceiver extends Thread {
    protected byte[] buf = new byte[4096];
    protected MulticastSocket socket = null;
    private InetAddress group;
    int id;

    public messageReceiver(int id) throws IOException {
        this.id = id;
        socket = new MulticastSocket(4446);
        group = InetAddress.getByName("230.0.0.0");
        socket.joinGroup(group);
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
                List<String> receivedMsgs = (List<String>) objStream.readObject();

                System.out.println("Heartbeat " + id + " received messages:");
                /*for (String msg : receivedMsgs) {
                    System.out.println(" - " + msg);
                }*/
                System.out.println(receivedMsgs);
                if (receivedMsgs.contains("end")) {
                    System.out.println("End signal received, closing receiver...");
                    break;
                }
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
}

