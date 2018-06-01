package ro.cst.tsearch.utils.tags;

import org.htmlparser.PrototypicalNodeFactory;
import org.htmlparser.tags.CompositeTag;

public class WhateverTag extends CompositeTag {

	/**
	 * must be used like this
	 * 
	 * 
	 * 
	 * org.htmlparser.Parser htmlParser = org.htmlparser.Parser.createParser(htmlString, null);
			
			WhateverTag addTags = new WhateverTag();
			htmlParser.setNodeFactory(addTags.addNodeFactory());
			
		NodeList mainList = htmlParser.parse(null);
	 * 
	 * 
	 * 
	 */
	private static final long serialVersionUID = 2023073507259541761L;

	public WhateverTag(){
		
	}
	
	public PrototypicalNodeFactory addNodeFactory(){
		PrototypicalNodeFactory factory = new PrototypicalNodeFactory ();
		factory.registerTag(new FontTag());
		factory.registerTag(new BTag());
		factory.registerTag(new DLTag());
		factory.registerTag(new XMLTag());
		
		//add here new tags that you need, also, below, the specific class for that tag. 
		
		
		return factory;
	}
	public class FontTag extends WhateverTag
	{
	    /**
		 * 
		 */
		private static final long serialVersionUID = -6201888743877671808L;
		
		private final String[] mIds = new String[] {"FONT"};
	    
		public FontTag(){
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
	public class BTag extends WhateverTag
	{
	    /**
		 * 
		 */
		private static final long serialVersionUID = -6203454877671808L;
		
		private final String[] mIds = new String[] {"B"};
	    
		public BTag(){
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
	
	public class DLTag extends WhateverTag
	{
	    /**
		 * 
		 */
		private static final long serialVersionUID = -6203454877671808L;
		
		private final String[] mIds = new String[] {"DL"};
	    
		public DLTag(){
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
	
	public class XMLTag extends WhateverTag
	{
	    /**
		 * 
		 */
		private static final long serialVersionUID = -6203454877671808L;
		
		private final String[] mIds = new String[] {"XML"};
	    
		public XMLTag(){
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
	
}
