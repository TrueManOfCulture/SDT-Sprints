import java.util.Random;

public enum HeartbeatTime {
    TIME(1000);
    private int value;

    HeartbeatTime(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static int generateRandomTimeout() {
        Random random = new Random();
        return 1100 + random.nextInt(201); // Generates a random value between 1100 and 1300 }
    }
}