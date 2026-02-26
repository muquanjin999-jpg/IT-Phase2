package game.model;

import java.util.*;

public class Hand<T> {
    private final List<T> cards = new ArrayList<>();
    private final int maxSize;

    public Hand(int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");
        this.maxSize = maxSize;
    }

    public int size() { return cards.size(); }
    public int getMaxSize() { return maxSize; }
    public boolean isFull() { return cards.size() >= maxSize; }
    public boolean isEmpty() { return cards.isEmpty(); }

    public boolean add(T card) {
        if (isFull()) return false;
        cards.add(card);
        return true;
    }

    public T removeAt(int index) {
        return cards.remove(index);
    }

    public boolean remove(T card) {
        return cards.remove(card);
    }

    public T get(int index) {
        return cards.get(index);
    }

    public List<T> snapshot() {
        return new ArrayList<>(cards);
    }

    @Override
    public String toString() {
        return cards.toString();
    }
}
