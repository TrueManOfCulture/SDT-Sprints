import java.util.ArrayList;

public class MessageList {
    static boolean lock = false;
    ArrayList<String> messageList = new ArrayList<String>();

    public MessageList(ArrayList<String> messageList) {
        this.messageList = messageList;
    }

    public synchronized void addElement(String e){
        messageList.add(e);
    }

    public synchronized void removeElement(String e){
        messageList.remove(e);
    }

    public synchronized ArrayList<String> getClone(){
        return new ArrayList<String>(messageList);
    }
}
