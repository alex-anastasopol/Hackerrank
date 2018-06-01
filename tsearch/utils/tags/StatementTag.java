package ro.cst.tsearch.utils.tags;

public class StatementTag extends WhateverTag
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -6203454877671808L;
	
	private final String[] mIds = new String[] {"statement"};
    
	public StatementTag(){
    }
    
    public String[] getIds(){
        return (mIds);
    }
    public String[] getEnders(){
        return (mIds);
    }
    public String[] getEndTagEnders(){
        return (new String[0]);
    }
}