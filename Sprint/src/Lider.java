import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Lider {
    private int id;
    private MessageList msgs = new MessageList(new ArrayList<String>(Arrays.asList("m1", "m2", "m3")));

    public Lider(int id) throws IOException, InterruptedException {
        this.id = id;
        new messageSender(msgs);
    }
}