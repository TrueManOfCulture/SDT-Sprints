import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MessageListInterface extends Remote {
    void addElement(int key, String msg) throws RemoteException;
    void removeElement(int k) throws RemoteException;
}
