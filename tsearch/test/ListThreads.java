package ro.cst.tsearch.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ListThreads {
	
	
    // This method recursively visits all thread groups under `group'.
    public static void visit(ThreadGroup group, int level, StringBuffer sb, Collection<String> names) {
    	
        // Get threads in `group'
        int numThreads = group.activeCount();
        Thread[] threads = new Thread[numThreads*2];
        numThreads = group.enumerate(threads, false);
    
        String prefix = "";
        for(int i=0; i<level; i++){
        	prefix += "    ";
        }
        
        // Enumerate each thread in `group'
        for (int i=0; i<numThreads; i++) {
            // Get thread
            Thread thread = threads[i];      
            String idx = "" + (names.size() + 1);
            for(int j = 0; j < (4-idx.length()); j++){
            	sb.append(" ");
            }
            sb.append(idx + ": " + prefix + thread.getName() + "<br/>");
            names.add(thread.getName());
        }
    
        // Get thread subgroups of `group'
        int numGroups = group.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[numGroups*2];
        numGroups = group.enumerate(groups, false);
    
        // Recursively visit each subgroup
        for (int i=0; i<numGroups; i++) {
            visit(groups[i], level+1, sb, names);
        }
    }

    public static String displayThreads(){
    	
    	// Find the root thread group
        ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        
        // Visit each thread group
        List<String> names = new ArrayList<String>(); 
        StringBuffer sb = new StringBuffer();
        sb.append("Thread list<br/>");
        sb.append("-----------------<br/>");
        visit(root, 0, sb, names);  
        sb.append("-----------------<br/>");
        sb.append("Total threads: " + names.size());
        
        return sb.toString();
    }
}
