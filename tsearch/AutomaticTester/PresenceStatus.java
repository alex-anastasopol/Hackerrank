package ro.cst.tsearch.AutomaticTester;

public class PresenceStatus
{
    private long testDate;
    private String testStatus;
    private long dbID;
    
    public static final String STATUS_UP = "OK";
    public static final String STATUS_DOWN = "NOT OK";
    public static final String STATUS_DISABLED = "DISABLED";
    
    public PresenceStatus(String testDate, String testStatus, long dbID)
   // public PresenceStatus(long testDate, String testStatus, long dbID)
    {
       // this.testDate = testDate;
        this.testStatus = testStatus;
        this.dbID = dbID;
    }
    
    public long getTestDate()
    {
        return testDate;
    }
    
    public String getStatus()
    {
        return testStatus;
    }
}