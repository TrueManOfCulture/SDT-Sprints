import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class MessageList  extends UnicastRemoteObject implements MessageListInterface {
    static boolean lock = false;
    HashMap<Integer,String> messageList = new HashMap<Integer,String>();

    public MessageList(HashMap<Integer,String> messageList) throws RemoteException {
        super();
        this.messageList = messageList;
    }

    public MessageList() throws RemoteException {
        super();

    }

    public synchronized void addElement(int k, String o){
        messageList.put(k,o);
    }

    public synchronized void removeElement(int k){
        messageList.remove(k);
    }

    public synchronized HashMap<Integer,String> getClone(){
        return new HashMap<Integer,String>(messageList);
    }

    public synchronized void clear(){
        messageList.clear();
    }
}
