import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Entidade {
    private int id;
    private boolean lider;
    private MessageList msgs = new MessageList(new ArrayList<String>(Arrays.asList("m1", "m2", "m3")));

    public Entidade(int id) throws IOException, InterruptedException {
        new messageReceiver(id);
    }
}
