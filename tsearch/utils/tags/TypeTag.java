package ro.cst.tsearch.utils.tags;

public class TypeTag extends WhateverTag
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -6203454877671808L;
	
	private final String[] mIds = new String[] {"type"};
    
	public TypeTag(){
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