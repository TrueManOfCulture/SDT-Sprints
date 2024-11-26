import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Criação e registro de entidades
        if(Boolean.parseBoolean(args[1])){
            Leader l = new Leader(Integer.parseInt(args[0]));
        }else{
            Entity e = new Entity(Integer.parseInt(args[0]));
        }
    }
}
