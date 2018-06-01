package ro.cst.tsearch.servers.response;
import java.util.*;
import java.io.*;
import java.util.regex.*;
import org.apache.log4j.Category;

public class SimpleDocSplitter implements DocSplitter
{
	protected static final Category logger= Category.getInstance(SimpleDocSplitter.class.getName());
	
	//splitting using a single separator
	public static final int ONE_TAG_MODE=0;
	//splitting using two separators
	//parses all docs BETWEEN 2 distinct tags 
	public static final int TWO_TAG_MODE=1;
	
	//original and working copy of document
	protected String original,work;
	
	//start and end tags
	protected String start_tag,end_tag;
	
	//parsed docs
	protected ArrayList docs;
	
	//parsing mode see above
	protected int parse_mode;
	
	//flags to ignore the separator tags --will not appear in the parsed doc
	protected boolean ign_start;
	protected boolean ign_end;
	protected boolean ign_both;
	
	public SimpleDocSplitter()
	{
		docs=new ArrayList();		
		ign_start=false;
		ign_end=false;
		ign_both=false;
	} 
	
	public SimpleDocSplitter(String doc,String only_tag)
	{
		docs=new ArrayList();		
		original=new String(doc);
		work=doc;
		start_tag=only_tag;
		parse_mode=ONE_TAG_MODE;
		ign_start=false;
		ign_end=false;
		ign_both=false;
		try{
		parse();
   	    }
   	    catch(Exception e)
   	    {
   	       logger.error("Splitter Exception!"+e);
   	       e.printStackTrace();
   	    }	
		
	}
	public SimpleDocSplitter(String doc,String s_tag,String e_tag)
	{
		docs=new ArrayList();		
		original=new String(doc);
		work=doc;
		start_tag=s_tag;
		end_tag=e_tag;
		parse_mode=TWO_TAG_MODE;
		ign_start=false;
		ign_end=false;
		ign_both=false;
		try{
		parse();
   	    }
   	    catch(Exception e)
   	    {
   	       logger.error("Splitter Exception!"+e);
   	       e.printStackTrace();
   	    }	
		
	}
	
	public void setDoc(String doc) 
	{
		original=new String(doc);
		work=doc;
		try{
		parse();
   	    }
   	    catch(Exception e)
   	    {
   	       logger.error("Splitter Exception!"+e);
   	       e.printStackTrace();
   	    }	
	}
	public String getDoc() 
	{
		return original;
	}
	public int getSplitNo() 
	{
		if(parse_mode==ONE_TAG_MODE)
			return docs.size();
		else
			return docs.size()/2;
	}
	
	public boolean isIgnoreStart()
	{
		return ign_start || ign_both;
	}
	
	public boolean isIgnoreEnd()
	{
		return ign_end || ign_both;
	}
	public boolean isIgnoreBoth()
	{
		return ign_both;
	}
	
	public void setIgnoreStart(boolean b)
	{
		ign_start=b;
	}
    public void setIgnoreBoth(boolean b)
	{
		ign_both=b;
	}	
	public void setIgnoreEnd(boolean b)
	{
		ign_end=b;
	}
	//get docs - index starting from 0
	public String getSplitDoc(int idx)
	{	
	   int l1=0,l2=0;
	   if(parse_mode==ONE_TAG_MODE)	
	   {
	   	   l1=((Integer)(docs.get(idx))).intValue();
	       if(idx<docs.size()-1)
	       {	         
	          l2=((Integer)(docs.get(idx+1))).intValue();	          	          
	       }
	       
	       if(isIgnoreStart())
	         l1+=start_tag.length();
	         
	       if(l2!=0)
	         return original.substring(l1,l2);
	       else  
	         return original.substring(l1);
	         
       }
       else   
		//if(parse_mode==TWO_TAG_MODE)
		{
		   	l1=((Integer)(docs.get(2*idx))).intValue();
		   	l2=((Integer)(docs.get(2*idx+1))).intValue();
		   	
		   	if(isIgnoreStart())
	         l1+=start_tag.length();
	         
	        if(isIgnoreEnd())
	         l2-=end_tag.length(); 
	        
	        return original.substring(l1,l2); 
		   	
		}
		
	}
	
	public String getSplitDoc(int idx,boolean start,boolean end)
	{
		boolean old_start=isIgnoreStart();
		boolean old_end=isIgnoreEnd();
		boolean old_both=isIgnoreBoth();
		String tmp;
		
		setIgnoreBoth(false);
		setIgnoreStart(start);
		setIgnoreEnd(end);
		
		tmp=getSplitDoc(idx); 
		
		setIgnoreBoth(old_both);
		setIgnoreStart(old_start);
		setIgnoreEnd(old_end);
		
		return tmp;
    }
	
	
	protected void parse() throws Exception
	{
		if(parse_mode==ONE_TAG_MODE)
		   parseOneTag();
		else   
		if(parse_mode==TWO_TAG_MODE)
		  parseTwoTag();
		
    }
    
    public void setStartTag(String tag)
    {
    	start_tag=tag;
    }
    public void setEndTag(String tag)
    {
    	end_tag=tag;
    }
    public void setOneTag(String tag)
    {
    	start_tag=tag;
    }
    
    public String getStartTag()
    {
    	return start_tag;
    }
    public String getEndTag(String tag)
    {
    	return end_tag;
    }
    public String getOneTag(String tag)
    {
    	return start_tag;
    }
    
    
    
    protected void parseOneTag() throws Exception
    {
        int idx;
        docs=new ArrayList();
        idx=work.indexOf(start_tag);
        while(idx!=-1)
        {
           docs.add(new Integer(idx));
           idx=work.indexOf(start_tag,idx+1);
        }        	
    }
    
    protected void parseTwoTag() throws Exception
    {   
        int i_start,i_end;
        docs=new ArrayList();
        i_start=work.indexOf(start_tag);
        i_end=work.indexOf(end_tag,i_start);
        while(i_start!=-1 && i_end!=-1)
        {
           docs.add(new Integer(i_start));
           docs.add(new Integer(i_end+end_tag.length()));
           
           i_start=work.indexOf(start_tag,i_end+1);
           i_end=work.indexOf(end_tag,i_start);
        }        	    	
    }
    
    public String getNextLink()
    {
    	/*
    	Pattern p=Pattern.compile("</table>",Pattern.CASE_INSENSITIVE);
    	Matcher m=p.matcher(original);
    	int start=-1,end=-1,i=0;
    	String result=null;
    	
    	//find last </table>
    	
    	while(m.find()) i=m.end();    	
    	
    	//find "next" link
    	p=Pattern.compile("next",Pattern.CASE_INSENSITIVE);
    	m=p.matcher(original);
    	
    	if(m.find(i))
    	{
    		i=m.start();
    		start=original.lastIndexOf("<a",i);
    		if(start==-1 || original.lastIndexOf("<A",i)>start)
    		   start=original.lastIndexOf("<A",i);
    		   
    		end=original.indexOf("</a",i);
    		if(end==-1 || original.indexOf("</A",i)<end)
    		   end=original.indexOf("</A",i);
    	}
    	if(start!=-1 && end!=-1 && start<end)    	
    	   result=original.substring(start,end)+"</a>";
    	   
    	return result;
    	*/
		return searchLink("next");
    }
    public String getPrevLink()
    {
    	/*
    	Pattern p=Pattern.compile("</table>",Pattern.CASE_INSENSITIVE);
    	Matcher m=p.matcher(original);
    	int start=-1,end=-1,i=0;
    	String result=null;
    	
    	//find last </table>
    	
    	while(m.find()) i=m.end();    	
    	
    	//find "next" link
    	p=Pattern.compile("prev",Pattern.CASE_INSENSITIVE);
    	m=p.matcher(original);
    	
    	if(m.find(i))
    	{
    		i=m.start();
    		start=original.lastIndexOf("<a",i);
    		if(start==-1 || original.lastIndexOf("<A",i)>start)
    		   start=original.lastIndexOf("<A",i);
    		   
    		end=original.indexOf("</a",i);
    		if(end==-1 || (original.indexOf("</A",i)<end && original.indexOf("</A",i)!=-1))
    		   end=original.indexOf("</A",i);
    	}
    	if(start!=-1 && end!=-1 && start<end)    	
    	   result=original.substring(start,end)+"</a>";
    	   
    	return result;
    	*/
    	return searchLink("prev");
    }
    protected String searchLink(String l)
    {
		Pattern p=Pattern.compile("</table>",Pattern.CASE_INSENSITIVE);
				Matcher m=p.matcher(original);
				int start=-1,end=-1,i=0;
				String result=null;
    	
				//find last </table>
    	
				while(m.find()) i=m.end();    	
    	
				//find l="next"/"prev" link
				p=Pattern.compile(l,Pattern.CASE_INSENSITIVE);
				m=p.matcher(original);
    	
				if(m.find(i))
				{
					i=m.start();
					start=original.lastIndexOf("<a",i);
					if(start==-1 || original.lastIndexOf("<A",i)>start)
					   start=original.lastIndexOf("<A",i);
    		   
					end=original.indexOf("</a",i);
					if(end==-1 || (original.indexOf("</A",i)<end && original.indexOf("</A",i)!=-1))
					   end=original.indexOf("</A",i);
				}
				if(start!=-1 && end!=-1 && start<end)    	
				   result=original.substring(start,end)+"</a>";    	   
				return result;        
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
    	    //logger.info(html);
    	    
    		int i,j;
    		i=html.indexOf("<TABLE border=0>");
    		i=html.indexOf("<TABLE border=0>",i+1);
    		//i=html.indexOf("<TABLE border=0>",i);
    		j=html.indexOf("</TABLE>",i)+(new String("</TABLE>")).length();
    		html=html.substring(i,j);
    		logger.info(html);
    		
    		SimpleDocSplitter hsp=new SimpleDocSplitter(html,"<TR","</TR>");
    		
    		//hsp.setIgnoreEnd(true);
    		//hsp.setIgnoreStart(true);
    		hsp.setIgnoreBoth(true);
    		
    		logger.info(">>>>>>>>>doc count "+hsp.getSplitNo());
    		logger.info(">>>>>>>>>doc (0) fara coada \n  "+hsp.getSplitDoc(0,false,true));
    		logger.info(">>>>>>>>>doc (3) ambele tag \n  "+hsp.getSplitDoc(2,false,false));
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }
}
