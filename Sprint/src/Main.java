import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Criação e registro de entidades
        Entidade e = new Entidade(Integer.parseInt(args[0]), Boolean.parseBoolean(args[1]));
    }
}
