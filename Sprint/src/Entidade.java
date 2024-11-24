import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Entidade {
    private int id;
    private boolean lider;
    private MessageList msgs = new MessageList(new ArrayList<String>(Arrays.asList("m1", "m2", "m3")));

    public Entidade(int id, boolean lider) throws IOException, InterruptedException {

        this.id = id;
        this.lider = lider;
        if (lider) {
            new messageSender(msgs);
        } else {
            new messageReceiver(id);
        }
    }
}
