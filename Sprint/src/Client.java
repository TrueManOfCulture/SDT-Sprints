import java.io.IOException;
import java.rmi.Naming;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        Thread t = (new Thread() {
            public void run() {
                try {
                    Main.main(new String[]{});
                    Main.main(new String[]{});
                    Main.main(new String[]{});
                    Main.main(new String[]{});
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();
        Thread.sleep(3000);

        try {
            MessageListInterface m;
            m = (MessageListInterface) Naming.lookup("rmi://localhost:2000/MessageUpdater");
            m.addElement("1", "m1");
            m.addElement("2", "m2");
            m.addElement("3", "m3");
        } catch (Exception e) {
            System.out.println("Problemas de Comunicação\n" + e.getMessage());
        }

    }
}
