package ro.cst.tsearch.AutomaticTester;

import java.util.*;
import java.io.File;

public class FileComparator implements Comparator
{
    public int compare( Object file1, Object file2 )
    {
        if( !( file1 instanceof File ) || !( file2 instanceof File ) )
        {
            return 0;
        }
        
        File f1 = (File) file1;
        File f2 = (File) file2;
        
        if( f1.lastModified() < f2.lastModified() )
        {
            return -1;
        }
        else if( f1.lastModified() > f2.lastModified() )
        {
            return 1;
        }
        
        return 0;
    }
}