import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MessageListInterface extends Remote {
    void addElement(String key, String msg) throws RemoteException;
    void removeElement(String k) throws RemoteException;
}
