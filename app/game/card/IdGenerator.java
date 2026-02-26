package game.card;

public class IdGenerator {
    private int counter = 1;

    public synchronized String next(String prefix) {
        return prefix + (counter++);
    }
}
