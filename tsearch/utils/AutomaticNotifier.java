package ro.cst.tsearch.utils;

public class AutomaticNotifier
{
    public static final int SERVER_CHANGED_MASK = 1;
    public static final int NEW_CHAPTERS_MASK = 2;
    
    private int statusChanged;
    
    public AutomaticNotifier()
    {
        statusChanged = 0;
    }
    
    public boolean hasStatusChanged( int testMask )
    {
        return ((testMask & statusChanged) != 0);
    }
    
    public void setServerChanged()
    {
        statusChanged = statusChanged | SERVER_CHANGED_MASK;
    }
    
    public void setNewChaptersAdded()
    {
        statusChanged = statusChanged | NEW_CHAPTERS_MASK;
    }
    
    public void reset()
    {
        statusChanged = 0;
    }
}