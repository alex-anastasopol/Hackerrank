package ro.cst.tsearch.monitor;

public class Counter {

    private long counter = 0;

    public long read() {
        return counter;
    }

    public void increment() {
        counter++;
    }
}

