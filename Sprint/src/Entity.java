import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Entity {
    private int id;
    private boolean lider;
    private HashMap data;
    private HashMap dataPending;
    private ArrayList entidades;

    public Entity(int id) throws IOException, InterruptedException {
        new messageReceiver(id);
    }
}
