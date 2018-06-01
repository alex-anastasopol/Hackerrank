package ro.cst.tsearch.servers.response;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Category;

public class RegexDocSplitter extends SimpleDocSplitter implements DocSplitter
{
	protected static final Category logger= Category.getInstance(RegexDocSplitter.class.getName());
	
   public RegexDocSplitter	()
   {
   	  super();
   }
   public RegexDocSplitter(String doc,String only_tag)
   {
   	   super(doc,only_tag);
   }
   public RegexDocSplitter(String doc,String s_tag,String e_tag)
   {
   	  super(doc,s_tag,e_tag);
   }
    
    public int getSplitNo() 
	{
		if(parse_mode==ONE_TAG_MODE)
			return docs.size()/2;
		else
			return docs.size()/4;
	}
	
	//get docs - index starting from 0
	public String getSplitDoc(int idx)
	{	
	   int l1=0,l2=0;
	   if(parse_mode==ONE_TAG_MODE)	
	   {
	   	   l1=((Integer)(docs.get(2*idx))).intValue();
	   	   
	       if(idx<docs.size()/2-1)
	       {	         
	          l2=((Integer)(docs.get(2*(idx+1)))).intValue();	          	          
	       }
	       
	       if(isIgnoreStart())
	         l1=((Integer)(docs.get(2*idx+1))).intValue();
	         
	       if(l2!=0)
	         return original.substring(l1,l2);
	       else  
	         return original.substring(l1);
	         
       }
       else   
		//if(parse_mode==TWO_TAG_MODE)
		{
		   	l1=((Integer)(docs.get(4*idx))).intValue();
		   	l2=((Integer)(docs.get(4*idx+3))).intValue();
		   	
		   	if(isIgnoreStart())
	         l1=((Integer)(docs.get(4*idx+1))).intValue();
	         
	        if(isIgnoreEnd())
	         l2=((Integer)(docs.get(4*idx+2))).intValue();
	        
	        return original.substring(l1,l2); 
		   	
		}
		
	}
	
    protected void parseOneTag() throws Exception
    {
        int idx;
        docs=new ArrayList();
        Pattern p_start=Pattern.compile(start_tag); 
        Matcher m_start=p_start.matcher(work);
        
        idx=0;
        while(m_start.find(idx))
        {
           docs.add(new Integer(m_start.start()));
           docs.add(new Integer(m_start.end()));
           idx=m_start.end();
        }        	        
    }
    
    protected void parseTwoTag() throws Exception
    {   
        int i_start;
        docs=new ArrayList();
        i_start=0;        
        
        Pattern p_start=Pattern.compile(start_tag); 
        Matcher m_start=p_start.matcher(work);
        
        Pattern p_end=Pattern.compile(end_tag);
        Matcher m_end=p_end.matcher(work);
        
        if(m_start.find()) i_start=m_start.start();
        
        while(m_start.find(i_start) && m_end.find(i_start))
        {
           docs.add(new Integer(m_start.start()));
           docs.add(new Integer(m_start.end()));
           docs.add(new Integer(m_end.start()));
           docs.add(new Integer(m_end.end()));
           
           i_start=m_end.end();           
        }        	    	
    }
    
     public static void main(String args[])
    {
    	try{
    		BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream("1.htm")));
    		StringBuffer sb=new StringBuffer();
    		String line,html;
    		line=br.readLine();
    		while(line!=null)
    		{
    		sb.append(line+"\n");
    		line=br.readLine();
    	    }
    	    br.close();
    	    html=sb.toString();
    	    
    	    RegexDocSplitter hsp=new RegexDocSplitter(html,"<TR.[^>]*>","</TR>");
    	//	RegexDocSplitter hsp=new RegexDocSplitter(html,"<TR.[^>]*>");
    	    
    	    
    	    //logger.info(html);
    	    /*
    		int i,j;
    		i=html.indexOf("<TABLE border=0>");
    		i=html.indexOf("<TABLE border=0>",i+1);
    		//i=html.indexOf("<TABLE border=0>",i);
    		j=html.indexOf("</TABLE>",i)+(new String("</TABLE>")).length();
    		html=html.substring(i,j);
    		logger.info(html);
    		
    		
    		//hsp.setIgnoreEnd(true); 
    		//hsp.setIgnoreStart(true);
    		hsp.setIgnoreBoth(true);
    		
    		logger.info(">>>>>>>>>doc count "+hsp.getSplitNo());
    		logger.info(">>>>>>>>>doc (0) fara coada \n  "+hsp.getSplitDoc(0,true,true));
    		logger.info(">>>>>>>>>doc (3) ambele tag \n  "+hsp.getSplitDoc(20,true,true));
    		*/
    		logger.info(">>>>next link >>>"+hsp.getNextLink());
    		logger.info(">>>>Previous link >>>"+hsp.getPrevLink());
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }
}