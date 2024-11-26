import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

public class Leader{
    private int id;
    private MessageList msgs;
    private HashMap data;
    private HashMap dataPending;

    public Leader(int id) throws IOException, InterruptedException {
        this.id = id;

        HashMap<Integer, String> initialData = new HashMap<>();
        initialData.put(1, "m1");
        initialData.put(2, "m2");
        initialData.put(3, "m3");


        this.msgs = new MessageList(initialData);

        new messageSender(msgs);
        new LeaderMessageReceiver(id, msgs);
        Registry r = LocateRegistry.createRegistry(2000);
        r.rebind("MessageUpdater", msgs);
    }

    public void add(int key, String msg) {
        msgs.addElement(key, msg);
    }


}
