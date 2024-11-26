import java.util.ArrayList;
import java.util.HashMap;

public class Heartbeat {
    static int id = 0;
    private String type;
    private HashMap<Integer,String> content;

    public Heartbeat(String type, HashMap<Integer,String> content) {
        id++;
        this.type = type;
        this.content = content;
    }
}
