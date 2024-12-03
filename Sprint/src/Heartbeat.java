import java.io.Serializable;
import java.util.HashMap;

public class Heartbeat implements Serializable {
    static int id = 0;
    private MessageType type;
    private HashMap<String,String> content;
    private static final long serialVersionUID = 1L;

    public Heartbeat(MessageType type, HashMap<String,String> content) {
        id++;
        this.type = type;
        this.content = content;
    }

    public static int getId() {
        return id;
    }

    public static void setId(int id) {
        Heartbeat.id = id;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public HashMap<String, String> getContent() {
        return content;
    }

    public void setContent(HashMap<String, String> content) {
        this.content = content;
    }
}
