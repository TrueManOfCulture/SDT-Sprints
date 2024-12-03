import java.util.Random;

public enum HeartbeatTime {
    TIME(3000);
    private int value;

    HeartbeatTime(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static int generateRandomTimeout() {
        Random random = new Random();
        return 5000 + random.nextInt(10000);
    }
}