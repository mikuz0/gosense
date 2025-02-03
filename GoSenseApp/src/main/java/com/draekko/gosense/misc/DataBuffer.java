package com.draekko.gosense.misc;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class DataBuffer<T> implements Iterable<T> {

    private static final String TAG = "DataBuffer";

    private ArrayList<T> buffer = new ArrayList<>();
    private long timelimit;

    public DataBuffer(long timelimit) {
        this.timelimit = timelimit;
    }

    public void setTimeLimit(long timelimit) {
        this.timelimit = timelimit;
    }

    public long getTimeLimit() {
        return timelimit;
    }

    public synchronized ArrayList<T> getDataBuffer() {
        return buffer;
    }

    public DataBuffer(long timelimit, ArrayList<T> newBuffer) {
        buffer = (ArrayList<T>) newBuffer.clone();
        this.timelimit = timelimit;
    }

    @Override
    public void forEach(@NonNull Consumer<? super T> action) {
        Iterable.super.forEach(action);
    }

    @NonNull
    @Override
    public Spliterator<T> spliterator() {
        return Iterable.super.spliterator();
    }

    public boolean isEmpty() {
        return buffer.size() == 0;
    }

    public int size() {
        return buffer.size();
    }

    public void clear() {
        buffer.clear();
    }

    public synchronized void push(T item) {
        buffer.add(item);
        popAll();
    }

    public synchronized void popSingle() {
        pop();
    }

    public synchronized void popAll() {
        pop();
    }

    public synchronized T pop() {
        if (!isEmpty()) {
            T item = buffer.get(0);
            buffer.remove(0);
            return item;
        }
        return null;
    }

    public Iterator<T> iterator() {
        return new DataBufferIterator();
    }

    private class DataBufferIterator implements Iterator<T> {
        private int i = 0;

        public T moveToFirst() {
            if (!isEmpty()) {
                i = 0;
                return buffer.get(i);
            }
            return null;
        }

        public boolean hasNext() {
            return i < buffer.size() - 1;
        }

        public void remove() {
        }

        public T next() {
            if (!isEmpty() && hasNext()) {
                return buffer.get(i++);
            }
            return null;
        }
    }
}
