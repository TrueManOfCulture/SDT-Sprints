import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.List;

public class Entidade {
    private int id;
    private int tipo;
    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[256];
    private DatagramSocket enviarSocket;
    private InetAddress group;
    private List<String> msgs = Arrays.asList("m1", "m2", "m3");

    public Entidade(int id, int tipo) throws IOException, InterruptedException {

        this.id = id;
        this.tipo = tipo;
        if (tipo == 1) {
            while (true) {
                new messageSender(msgs);
                Thread.sleep(5000);
            }
        }else{
            System.out.println("create this");
            new messageReceiver(id);
        }
    }
}
