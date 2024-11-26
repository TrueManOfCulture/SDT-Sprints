import java.io.IOException;

public class Client {
    public static void main(String[] args) throws InterruptedException {
        Thread t = (new Thread() {
            public void run() {
                try {
                    Main.main(new String[]{"2", "false"});
                    Main.main(new String[]{"3", "false"});
                    Main.main(new String[]{"4", "false"});
                    Main.main(new String[]{"1", "true"});
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();
        Thread.sleep(3000);
    }
}
