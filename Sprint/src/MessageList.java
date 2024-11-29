import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class MessageList  extends UnicastRemoteObject implements MessageListInterface {
    HashMap messageList = new HashMap();

    public MessageList(HashMap<Integer,String> messageList) throws RemoteException {
        super();
        this.messageList = messageList;
    }

    public MessageList() throws RemoteException {
        super();
    }

    public synchronized void addElement(String k, String o){
        messageList.put(k,o);
    }

    public synchronized void removeElement(String k){
        messageList.remove(k);
    }

    public synchronized HashMap<String,String> getClone(){
        return new HashMap(messageList);
    }

    public synchronized void clear(){
        messageList.clear();
    }
}
