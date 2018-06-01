package ro.cst.tsearch.monitor;

public class Time {
    
    private long counter = 0;
    
    private long minTime = 0;
    private double averageTime = 0;
    private long maxTime = 0;
    
    
    public double read() {
        return averageTime;
    }

    public void update(long time) {        
        
        if (minTime < time)
            minTime = time;
        
        if (time > maxTime)
            maxTime = time;
        
        if (time > 0) {
            averageTime = (counter * averageTime + time) / (counter + 1);
            counter = counter + 1;
        }
        
    }
    
    public void reset() {
        
        counter = 0;
        minTime = 0;
        averageTime = 0;
        maxTime = 0;
        
    }

}
