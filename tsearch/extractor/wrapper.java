/*

 Deprecated 
 * 
 * 
 */
package ro.cst.tsearch.extractor;

import java.util.*;
import java.io.*;
import javax.swing.tree.*;

import org.apache.log4j.Category;

import ro.cst.tsearch.servers.response.CourtDocumentIdentificationSet;
import ro.cst.tsearch.servers.response.InfSet;
import ro.cst.tsearch.servers.response.ParsedResponse;
import ro.cst.tsearch.servers.response.PropertyIdentificationSet;
import ro.cst.tsearch.servers.response.SaleDataSet;
import ro.cst.tsearch.servers.response.TaxHistorySet;
import ro.cst.tsearch.wrapper2.HTML_region;
import ro.cst.tsearch.wrapper2.RuleTreeManipulator;
import ro.cst.tsearch.wrapper2.action;
import ro.cst.tsearch.wrapper2.builder_st2;
import ro.cst.tsearch.wrapper2.limits;
import ro.cst.tsearch.wrapper2.my_node;
import ro.cst.tsearch.wrapper2.my_parser;
import ro.cst.tsearch.wrapper2.my_rule;
import ro.cst.tsearch.wrapper2.my_token;
import ro.cst.tsearch.wrapper2.node_constants;
import ro.cst.tsearch.wrapper2.patternToken;

public class wrapper
{
	//debugger log
	private static final Category logger= Category.getInstance(wrapper.class.getName());
	
	//flag daca s-a trecut printr-un nod iterativ
	private int passed_through_iterative=0;
	
	//constants used in parser
	public final int OIS= 1, ODS= 2, PAS= 3, PDS= 4, PIS= 5, SDS= 6, THS= 7, CDIS=8;
	
	String HTML_string;
	
   	File HTMLfile;
	File Rulesfile;
	
	InfSet infoset;
	
	/** this field contains the tree of rules used
	 * to parse a HTML file
	 */
	DefaultTreeModel model;
	
	/** this is the result of the parsing with a HTML_tree **/
     public String out;	
     
	/** parser engine **/
	  my_parser mp;
	  Vector buf_tok;
	/** vector of rules **/
	  Vector rules;
	
	/** set of rules for a path **/
	  Vector set_rules;
	
	/**   vector node_rules **/
	  Vector node_rules;
	
	  HashMap RulesHash;
				
	  my_rule r;
	/** action variable;used to build the rules **/
	  action a;		
	/** pattern_token variable**/
	  patternToken pt;
	
	/** value extracted **/
	  String result;
	/** result for iteration rules (Vector of Strings) **/
	  Vector it_results;
	/** vector of token codes **/
	  String codes;
	/** true if extraction was successful **/
	  boolean rez;
	
	//search zone
	  int idx_start,idx_end;
	//result bounds
	  int rez_start,rez_end;
	
	//Iteration buffers
	  Vector previous_iteration,next_iteration;
	  
	public wrapper()
	{
		//empty for easy-creation 	
	}
	
	public wrapper(String HTfile,String Rfile)
	{
		HTMLfile=new File(HTfile);
		Rulesfile=new File(Rfile);
	}
	public wrapper(File HTfile,File Rfile)
	{
		HTMLfile=HTfile;
		Rulesfile=Rfile;
	}
	
    public limits ExtractField2(HTML_region e,Vector rules)		
    {
    	limits ret_lim=new limits();
    	ret_lim.setInf(-1);ret_lim.setSup(-1);
    	//forward rule
    	my_rule fwr=(my_rule)rules.get(0);
    	//backward  rule
    	my_rule bkr=(my_rule)rules.get(1);
    	
    	if(fwr.type==my_rule.RULE_EXTRACT)
    	{
    		logger.debug("Extrag cu regula :"+fwr.name);
    	builder_st2 bst2=new builder_st2();
    	
    	ret_lim.setInf(bst2.applyForward(e,fwr));
    	
    	e.setPageLimits(new limits(ret_lim.getInf(),e.getPageLimits().getSup()));
    	
    	ret_lim.setSup(bst2.applyBackward(e,bkr));
         logger.debug("extraction limits:"+ret_lim.getInf()+","+ret_lim.getSup());
    	//temporary fix
    	e.setPageLimits(new limits(ret_lim.getInf(),ret_lim.getSup()));
    	
    	return ret_lim;
        }
        else
        {
        	
        	logger.debug("Extrag cu regula iterativa:"+fwr.name);
        	ret_lim=e.getPageLimits();
        	logger.debug("linf="+ret_lim.getInf()+" lsup="+ret_lim.getSup());
        	
            Vector intervals=Iterative_extraction(e,fwr,bkr);
            logger.debug("Am extras "+intervals.size()+" intervale");
            limits lim;
     		Vector tmp=e.getTokens();//original tokens
     		
     		//extracted tokens
     		Vector result=new Vector();//of my_tokens
     		                           
     		 int i,j;                          
     	    for(i=0;i<intervals.size();i++)
     	    {
     	         	lim=(limits)intervals.get(i);
     	         	for(j=lim.getInf();j<=lim.getSup();j++)
     	         	{
     	         		result.add(tmp.get(j));
     	         	}
     	    }	     	  
     	    e.setTokens(result);  	
     	    lim=new limits(0,result.size()-1);
     	    //e.setPageLimits(lim);
     	    return lim;
        }  

    }
    
	/**
		 * Method getSet.
		 * @param pr
		 * @param hn
		 * @return InfSet
		 */
	private InfSet getSet(ParsedResponse pr, my_node hn)
		{
			switch (hn.getNode_category_type())
			{
				case PAS :
					return pr.getPropertyAppraisalSet();
				case PIS :
					if (pr.getPropertyIdentificationSetCount() ==0)
					{
						logger.debug("Adaug mpis din functia getset");
						pr.addPropertyIdentificationSet (new PropertyIdentificationSet());
					}
						
					return pr.getPropertyIdentificationSet(pr.getPropertyIdentificationSetCount()-1);
				case SDS :
					if (pr.getSaleDataSetsCount() ==0)
					{
						pr.addSaleDataSet(new SaleDataSet());
						logger.debug("Adaug SDS din functia getset");
					}
						
					return pr.getSaleDataSet(pr.getSaleDataSetsCount()-1);
				
				case CDIS :
					if (pr.getCourtDocumentIdentificationSetCount() ==0)
					{
						pr.addCourtDocumentIdentificationSet(new CourtDocumentIdentificationSet());
						logger.debug("Adaug CDIS din functia getset");
					}
						
					return pr.getCourtDocumentIdentificationSet(pr.getCourtDocumentIdentificationSetCount()-1);
				case THS :
					if (pr.getTaxHistorySetsCount() ==0)
					{
						pr.addTaxHistorySet(new TaxHistorySet());
						logger.debug("Adaug THS din functia getset");
					}
						
					return pr.getTaxHistorySet(pr.getTaxHistorySetsCount()-1);
			}
			return null;
		}	
	
	/** This function prepares the HTML file **/
	public void LoadHTMLfile()
	{
		  //logger.info("Start loading HTML...");
           try
           {
     	     mp=new my_parser(new FileInputStream(HTMLfile));
		//	logger.info("done InputStream"); 		  
           }
           catch (Exception e)
           {
               logger.debug("Error: Wrapper -> Parser Html:"+e);
           }
          // logger.info("Start parse enegine");
          mp.parseEngine();
		  //logger.info("done parsing HTML");                    
          buf_tok=mp.getResult();
          //vectorize buffered tokens
//          vectorize();
		//logger.info("done Load&parse");
          logger.debug("done Load&parse");		
          
	}
	public DefaultTreeModel getModel()
	{
		return model;
	}
	
	public String html_to_string()
	{
		return HTML_string;
	}
	public void LoadHTMLfile(File f) 
		{
			  logger.info("Start loading HTML...");
			   try
			   {
			  mp=new my_parser(new FileInputStream(f)); 		  
			   }
			   catch (Exception e)
			   {
				   logger.debug("Wrapper -> Parser Html:"+e);
			   }
			  mp.parseEngine();                    
			  buf_tok=mp.getResult();
			  
//			Loading...
					  try{
					  	String tmp;
						BufferedReader   br=new BufferedReader(new FileReader(f));
						 logger.debug("File opened.");
						while((tmp=br.readLine())!=null)
						{
						   HTML_string+=tmp;						   
						   HTML_string+="\n";
						}
						br.close();
					   }
					   catch(IOException ex)
					   {
						 logger.debug("Exception in reading html file "+ex);
						 ex.printStackTrace();
					   }
			  //vectorize buffered tokens
//				vectorize();
			  logger.debug("done");		
		}
	/** This function prepares the Rules **/
	public void LoadRulesfile()
	{
		logger.info("Start loading Rules....");
		RuleTreeManipulator rules=new RuleTreeManipulator();
		rules.setF(Rulesfile);
		model=rules.ReadFromFile();				
        logger.info("done");
		
	}
	public String[] getAvailableRules()
	{
	    String tablou[]=new String[RulesHash.size()];
	    Set s;
//	    int i;
	    /*
	    for(i=0;i<tablou.length;i++)
	    {
	    //	tablou[i]=new String((String)RulesHash.get(new Integer(i)));
	    	
	    }*/
	    s=RulesHash.keySet();
	    tablou=(String[])s.toArray(tablou);
	    return tablou;
	}
	
	public Vector Iterative_extraction(HTML_region e,my_rule fwr,my_rule bkr)
   {
   	    int idx_start,idx_end,crt_idx;
   	    limits l1=e.getPageLimits(),l2;
   	    int i;
   	    //this contains the limits of the tokens
   	    Vector result_vector=new Vector();
   	  
   	   //  Vector toks=e.getTokens();
   	       
   	    idx_start=e.getPageLimits().getInf();
   	    idx_end=e.getPageLimits().getSup();
   	    
   	    builder_st2 bst2=new builder_st2();  
   	       	       	    
   	    i=idx_start;
   	    crt_idx=i;
   	    
   	    //forward search
   	    while(i<idx_end)
   	    {
   	    	crt_idx=bst2.applyForward(e,fwr);
   	    	if(crt_idx!=-1)
   	    	{
   	    		//logger.info("Forward index:"+crt_idx);   	    
   	    		i=crt_idx;   	    		
                        //back end search
                        l2=e.getPageLimits();
                        l1=new limits(crt_idx,l2.getSup());
                        e.setPageLimits(l1);
                        crt_idx=bst2.applyBackward(e,bkr);
                  //      logger.info("Backward index:"+crt_idx);
                        if(crt_idx!=-1)
                        {    
                          result_vector.add(new limits(i,crt_idx));
                          //set new page limits
                            l2=e.getPageLimits();
                            l1=new limits(crt_idx,l2.getSup());
                            logger.debug("l1inf="+l1.getInf()+" l1sup="+l1.getSup());
                            e.setPageLimits(l1);
                            i=crt_idx;                           
                        }
                        
   	    	}
   	    	else
   	    	{
   	    	  i++;	
   	    	  crt_idx=i;
   	    	  l1=e.getPageLimits();
   	    	  l1.setInf(crt_idx);
   	    	  e.setPageLimits(l1);  
   	    	}     	    	   	    	 	    
   	    }
   	    return result_vector;
   	} 
   	
	public String ExtractFieldValue2(String RuleName)
	{
		int k,j;		
		String my_result=null;
		try
		{
			
			//get set of rules from hash
		int idx=((Integer)RulesHash.get(RuleName)).intValue();										
	    set_rules=(Vector)node_rules.get(idx);      	
                    
       limits lm=new limits();
       lm.setInf(0);
       lm.setSup(buf_tok.size()-1);
      
        
       HTML_region ex=new HTML_region(buf_tok);
       ex.setPageLimits(lm);              
      	      	
      	for(k=0;k<set_rules.size();k++)      	      	
      	{
      	  rules=(Vector)set_rules.get(k); 	       	  
      	  lm=ExtractField2(ex,rules);      	 
      	  ex.setPageLimits(lm);
      	}  
          	    
          Vector res_vect=ex.getTokens();	    	    	    	            
          
       	my_result=new String();
           	for(j=lm.getInf();j<=lm.getSup();j++)
              	     my_result+=((my_token)res_vect.get(j)).tk+" ";              	  		              			
		}
		catch(Exception e)
		{
			logger.debug("oops :"+e);
			e.printStackTrace();
		}		
		//return result
		return my_result;
	}
	
	public void printstatus()
	{
		logger.debug("HTML file: "+HTMLfile);
		logger.debug("Rules file: "+Rulesfile);
	}	
	
	/*
	//DefaultTreeModel model
	public void parse_html()
	{
	//	TreeNode path[];
		my_node root=(my_node)model.getRoot();        
        
        int i;
        //Get_leafs_from_tree
    	Vector leafs=new Vector();
    	
    	
    	my_node frunza=(my_node)root.getFirstLeaf();
    	
    	while(frunza!=null)
    	{
    		leafs.add((my_node)frunza);
    		//print info
    		logger.info("Nume: ["+frunza.label+"] Rule:"+frunza.rule_type);
    		frunza=(my_node)frunza.getNextLeaf();
    	}
    	
    	//setup root node
		limits lm=new limits();            
         HTML_region ex=new HTML_region(buf_tok);
                  
       
       out=new String();
       
    	//for each child of root call parser
    	for(i=0;i<root.getChildCount();i++)
    	{
			
		    lm.setInf(0);
		    lm.setSup(buf_tok.size()-1);
			ex.setPageLimits(lm);      	
			Recursive_parser((my_node)root.getChildAt(i),ex);
    	}
    	    	
    	
    	 //for each leaf get the path to root
    	//for each leaf
    	 
    	for (i=0;i<leafs.size();i++)
    	{
    		frunza=(my_node)leafs.get(i);    	    	
    		path=model.getPathToRoot(frunza);
            
            out+="Extracting for :"+((my_node)path[path.length-1]).toString()+"\n";
    	    //parse each path then add to results
    	     recursive_extract(ex,path,1);
     	} 
     	       	
	}	*/
	
	/*
	public void recursive_extract(HTML_region ex,TreeNode path[],int crt)
	{
		my_node nod=(my_node)path[crt];
		int i,j;                       
		
		if(crt==path.length-1) //last node ==> leaf
		{
		    if(nod.getType()==1) //iterative
		    {
		    	//extract intervals
		    	Vector intervals=Iterative_extraction(ex,nod.getForward(),nod.getBackward());		    	
		    	Vector tmp=ex.getTokens();//original tokens
		    	//add intervals to final result		    	
		    		limits lim;      		             		     		     		     		                                		    
     	            for(i=0;i<intervals.size();i++)
     	            { 
     	         	    lim=(limits)intervals.get(i);
     	         	    for(j=lim.getInf();j<=lim.getSup();j++)
     	         	     {
     	         		    
     	         		    out+=tmp.get(j)+" ";
     	                 }
     	                 out+="\n";
     	             }	     	  
		    }
		    else //extraction
		    {
		    	
    	           limits ret_lim=new limits();
    	           builder_st2 bst2=new builder_st2();
    	
    	           ret_lim.setInf(bst2.applyForward(ex,nod.getForward()));
    	
    	           ex.setPageLimits(new limits(ret_lim.getInf(),ex.getPageLimits().getSup()));
    	
    	           ret_lim.setSup(bst2.applyBackward(ex,nod.getBackward()));    	    	
    	            	Vector tmp2=ex.getTokens();
    	           for(j=ret_lim.getInf();j<=ret_lim.getSup();j++)
     	         	     {
     	         		    
     	         		    logger.debug(tmp2.get(j)+" ");
     	         		    out+=tmp2.get(j)+" ";
     	                 }
     	                 out+="\n";
    	           
		    }
		    			   
		}
		else //intermediate node ---> recursive parse		
		{
			if(nod.getType()==1) //iterative
			{
				//extract intervals
		    	Vector intervals=Iterative_extraction(ex,nod.getForward(),nod.getBackward());		    	
		    	//Vector tmp=ex.getTokens();//original tokens
		    	//add intervals to final result		    	
		    		limits lim;      		             		     		     		     		                                		    
     	            for(i=0;i<intervals.size();i++)
     	            { 
     	         	    lim=(limits)intervals.get(i);
     	         	    HTML_region e1=new HTML_region(ex.getTokens(),ex.getVectorized(),lim);
     	         	    //e1.setPageLimits(lim);
     	         	    logger.debug("Recurenta pe iterativ");
     	         	    recursive_extract(e1,path,crt+1);
     	            }	     	  
				
			}
			else //normal extraction
			{
				  limits ret_lim2=new limits();
    	           builder_st2 bst22=new builder_st2();
    	
    	           ret_lim2.setInf(bst22.applyForward(ex,nod.getForward()));
    	
    	           ex.setPageLimits(new limits(ret_lim2.getInf(),ex.getPageLimits().getSup()));
    	
    	           ret_lim2.setSup(bst22.applyBackward(ex,nod.getBackward())); 
     	         HTML_region e2=new HTML_region(ex.getTokens(),ex.getVectorized(),ret_lim2);
     	         	    logger.debug("Recurenta pe extractie");
     	         	    recursive_extract(e2,path,crt+1);
    	            
			}
		}
	}
	*/
	
	/** get/set functiona **/
	public void setModel(DefaultTreeModel m)
	{
		model=m;	
	}
	
	/** init Vector of tokens from Stream **/
	public void WrapInputStream(InputStream is)
	{
		mp=new my_parser(is);
		
		mp.parseEngine();				
			
		buf_tok=mp.getResult();	
		
		//call recursive parser
	}
	public void parse_html_vector(Vector v)
	{
		 ParsedResponse pr=new ParsedResponse();
		 int i;
		 out=new String();
		 my_node root=(my_node)model.getRoot();
		 limits lm=new limits();   
		 logger.info("Before copy...");         
		 buf_tok=v;
		 logger.info("After copy...");
		 HTML_region ex=new HTML_region(buf_tok);
        
				//for each child of root call parser
				for(i=0;i<root.getChildCount();i++)
				{
							lm.setInf(0);
							lm.setSup(buf_tok.size()-1);
							ex.setPageLimits(lm);      	
							try {
								TS_Recursive_parser(pr,(my_node)root.getChildAt(i),ex);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
				}  
		
	}
	public void Parse_HTML(ParsedResponse pr,String sHtml, DefaultTreeModel m) throws IOException
	{
		int i;
		
		logger.debug("Setting Model...");
		setModel(m);
		
		logger.debug("Tokenizing HTML...");
		WrapInputStream(new ByteArrayInputStream(sHtml.getBytes()));
		
		logger.debug("Begin Parsing...");
		
		out=new String();
		
		my_node root=(my_node)model.getRoot();
		limits lm=new limits();
		//buf_tok=tokens;            
				 HTML_region ex=new HTML_region(buf_tok);
        
		//for each child of root call parser
		for(i=0;i<root.getChildCount();i++)
		{
					lm.setInf(0);
					lm.setSup(buf_tok.size()-1);
					ex.setPageLimits(lm);      	
					try {
						TS_Recursive_parser(pr,(my_node)root.getChildAt(i),ex);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		}
	}
	/**
		 * Method AddIterationObject.
		 * @param pr
		 * @param hn
		 */
		private void AddIterationObject(ParsedResponse pr, my_node hn)
		{
			switch (hn.getNode_category_type())
			{
				case SDS :
				
				    logger.debug("Parser :Adaug un obiect SDS");
					pr.addSaleDataSet(new SaleDataSet());
					break;
				case PIS :
				  {
				  	logger.debug("Parser :Adaug un obiect PIS");
					pr.addPropertyIdentificationSet( new PropertyIdentificationSet());
					break;
				  }
				  
				case CDIS :
				  {
				  	logger.debug("Parser :Adaug un obiect CDIS");
					pr.addCourtDocumentIdentificationSet( new CourtDocumentIdentificationSet());
					break;
				  }
			}
		}
		
	private String Parse_Tree_Path(TreeNode[] path,HTML_region ex) throws Exception
	{
		String result=new String("");
		Vector intervals=null;
		limits new_limits;
		int i=1;
		   //parse path
		   if(path!=null)
		   {
			while(i<path.length)
					   {
						intervals=Parser_core((my_node)path[i],ex);
						new_limits=(limits) intervals.get(0);
						ex.setPageLimits(new_limits);		   	
						i++;
					   }
		   }
		   
		  try
		  {
			//assemble result
			new_limits=(limits) intervals.get(0);
			result=getParsedValue((my_node)path[path.length-1],new_limits,ex);            
		  }
		  catch(Exception e)
		  {
		  	 logger.debug("Parse_tree_path Exception!");
		  	 e.printStackTrace();		  	 
		  }
		
		   		
		return result; 
	}
	private Vector Parser_core(my_node nod,HTML_region ex) throws Exception
	{
	   Vector intervals=new Vector();
	   
	    int     type=nod.getNodeType();
			
			   logger.debug("Entering limits :"+ex.getPageLimits().getInf()+","+ex.getPageLimits().getSup());
			   logger.debug(nod+" Node type: "+type+" second type:"+nod.getNodeSecondType());
			   switch(type)
			   {
				   //extract basic fields & other keys
				 case node_constants.ntype_extract:
				   logger.debug("Extraction node: "+nod);
				 case node_constants.ntype_basic:
				 {
				   logger.debug("Basic node: "+nod);					  
									//do extraction
									limits ret_lim=new limits();
									builder_st2 bst2=new builder_st2();
									ret_lim.setInf(bst2.applyForward(ex,nod.getForward()));
									ex.setPageLimits(new limits(ret_lim.getInf(),ex.getPageLimits().getSup()));
									ret_lim.setSup(bst2.applyBackward(ex,nod.getBackward()));    	 	  
									//add extracted zone to intervals
									intervals.add(ret_lim);   			      
									break;
				 }
				 //for iterative keys ... table rows
				 case node_constants.ntype_iterat:
				 {
				   logger.debug("Iterative node: "+nod);
				   passed_through_iterative=1;
				   logger.debug("Limite iterative:"+ex.getPageLimits().getInf()+","+ex.getPageLimits().getSup());
				   intervals=Iterative_extraction(ex,nod.getForward(),nod.getBackward());
				   logger.debug("Am gasit "+intervals.size()+"intervale");			  
				   break;
				 }
				   
			  
				 //this marks the data for a new object
				 case node_constants.ntype_categ:
				 {	
				   intervals.add(ex.getPageLimits());
									   //add a new data set
									   //if(passed_through_iterative==1)
									   //{
										 out+="---------------- New object :"+nod+"-------------\n";
										 //AddIterationObject(pr,nod);
									   //}					   
									   //else   
										  //out+="---------------- Get object :"+nod+"-------------\n";					  
									   //get current set
									   logger.debug("New Node categ:"+nod);
					//				   AddIterationObject(pr,nod);
									   //infoset=getSet(pr,nod);
								   break;				  					  
				 }
					
			   }
			 return intervals;  	   
	}
	private my_node PreOrder_searchNode(String key,my_node nod)
	{
		my_node crtnod;
		int i;
		
		if(nod==null) return null;
		
		  if(key.equals(nod.getLabel())) return nod;
		  else		
		   return PreOrder_searchNode(key,(my_node)nod.getNextNode());
	}
	public String getFieldfromFile(String FieldKey,TreeModel Rules,String HTMLFile)
	{
		String result=new String();
		TreeNode[] path=null;
		HTML_region ex=null;
		try {
		
		my_node crtnd=PreOrder_searchNode(FieldKey,(my_node) Rules.getRoot());
			
		if(crtnd!=null)
		  logger.debug(crtnd.getLabel()+" key found!");
	   else	  
	      logger.debug(FieldKey+" key NOT found!!!!!");
	    
//		getpath to root for crtnd
    if(crtnd!=null)
       path=crtnd.getPath();
	      
	    //build region from HTML file	    
	       //parse HTML
		logger.debug("Tokenizing HTML...");
		WrapInputStream(new ByteArrayInputStream(HTMLFile.getBytes()));
		//new tokens,limits														
		limits lm=new limits();
		//new HTML_regions            
		ex=new HTML_region(buf_tok);
	    //set limits (0,buf_tok.size()-1)
		lm.setInf(0);
		lm.setSup(buf_tok.size()-1);
		ex.setPageLimits(lm); 
		
		//start parsing     		    
	
			result=Parse_Tree_Path(path,ex);
			logger.debug("result1="+result);
			return result;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result=new String("");
			return result;
		}	      
	}
	private String getParsedValue(my_node nod,limits lm,HTML_region ex) throws Exception
	{		
		int i;
		String essential_result=new String();
		StringBuffer sbf=new StringBuffer();
						   if(lm.getInf()>=0 && lm.getSup()>=0)
						   {					   			   
							  for(i=lm.getInf();i<=lm.getSup();i++)
							  {				   
								  ///out+=(ex.getTokens()).get(i)+" ";
								  my_token t=(my_token)(ex.getTokens()).get(i);
								 if(t.cod!=my_token.TK_TAG && t.cod!=my_token.TK_SPECIAL) 
								{  
									if(i!=lm.getSup())//hard code REMOVE ASAP
									if (!(nod.getLabel()).equals("RecordedDate")&& !(nod.getLabel()).equals("YearBuilt"))
									{
										//essential_result+=t+" ";
										sbf.append(t);
										sbf.append(" ");
									}									 
								    else
								    {
								    	// essential_result+=t;
										sbf.append(t);    
								    }
								     
									if(i==lm.getSup())	                   			     
									{
										//  essential_result+=(ex.getTokens()).get(i);
										sbf.append((ex.getTokens()).get(i));
									}
									 				        	
								}
						
							  }			
							  essential_result=sbf.toString();   
							out+=  essential_result;
						   }
						   else
							 out+=" Missing Data: "+nod;			     
							out+="\n" ;
		
		return essential_result;		
	}
	public void TS_Recursive_parser(ParsedResponse pr,my_node nod,HTML_region ex) throws Exception
		{
			int type;
			int nr_children;    	
			Vector intervals;
			//////////////////////
			int i,j;
			//////////////////////
    	
			if(nod!=null && ex!=null)
			if(nod.isEnabled())
			{
				//logger.info("Parsing node : "+nod.getLabel()); 
				type=nod.getNodeType();
				
			 intervals=Parser_core(nod,ex);
			 
			 if(type==node_constants.ntype_categ)  	AddIterationObject(pr,nod);
			 
				nr_children=nod.getChildCount();
			
				if(nr_children>0) //intermediate node
				{
					for(i=0;i<intervals.size();i++)
					for(j=0;j<nr_children;j++)
					{
						limits lim=(limits)intervals.get(i);
						ex.setPageLimits(lim);
						
						logger.debug("Apelez parser cu limite:"+ex.getPageLimits().getInf()+","+ex.getPageLimits().getSup()); 
						TS_Recursive_parser(pr,(my_node)nod.getChildAt(j),ex);			    
					}
			    
					if(type==node_constants.ntype_categ)
					  out+="+++++++++++ end object "+nod+" ++++++++++\n";
					if(type==node_constants.ntype_iterat)
									  passed_through_iterative=0;  
				}
				else  //leaf ---> store results
				{
				   limits lm=(limits) intervals.get(0);	
				   out+=nod+":";				   
			
				   String essential_result=getParsedValue(nod,lm,ex);
					
					//store essential result
					infoset=getSet(pr,nod);
					if (infoset!=null)
					{
						logger.debug("key=["+nod.getLabel()+"] value=["+essential_result+"]");
						infoset.setAtribute(nod.getLabel(),essential_result);
					}
					else
					   logger.debug("oooops infoset is NULL :"+nod);
					
					
				}			
			}
			else
			{
				logger.debug("Null parameters in recursive parser");
			}    	    	        
		}

/*
    public void Recursive_parser(my_node nod,HTML_region ex)
    {
    	int type;
    	int nr_children;    	
    	Vector intervals;
    	//////////////////////
    	int i,j;
    	//////////////////////
    	
    	if(nod!=null && ex!=null)
    	{			
			type=nod.getNodeType();
			intervals=new Vector();
			logger.debug(nod+" Node type: "+type+" second type:"+nod.getNodeSecondType());
			switch(type)
			{
				//extract basic fields & other keys
			  case node_constants.ntype_basic:	
			  case node_constants.ntype_extract:
			  //do extraction
			  limits ret_lim=new limits();
			  builder_st2 bst2=new builder_st2();
    	 	  ret_lim.setInf(bst2.applyForward(ex,nod.getForward()));
    		  ex.setPageLimits(new limits(ret_lim.getInf(),ex.getPageLimits().getSup()));
    	 	  ret_lim.setSup(bst2.applyBackward(ex,nod.getBackward()));    	 	  
    	 	  //add extracted zone to intervals
    	 	  intervals.add(ret_lim);   			      
			  break;
			  
			  //for iterative keys ... table rows
			  case node_constants.ntype_iterat:
			     passed_through_iterative=1;
			     intervals=Iterative_extraction(ex,nod.getForward(),nod.getBackward());			  
			  break;
			  
			  //this marks the data for a new object
			  case node_constants.ntype_categ:
			    if(passed_through_iterative==1)
			      out+="---------------- New object :"+nod+"-------------\n";
			    else  
 				  out+="---------------- Get object :"+nod+"-------------\n";
			      intervals.add(ex.getPageLimits());
			  break;
			}
			
			nr_children=nod.getChildCount();
			
			if(nr_children>0) //intermediate node
			{
				for(i=0;i<intervals.size();i++)
			    for(j=0;j<nr_children;j++)
			    {
			    	limits lim=(limits)intervals.get(i);
			    	ex.setPageLimits(lim);
			    	Recursive_parser((my_node)nod.getChildAt(j),ex);			    
			    }
			    
			    if(type==node_constants.ntype_categ)
			      out+="+++++++++++ end object "+nod+" ++++++++++\n";
				if(type==node_constants.ntype_iterat)
				  passed_through_iterative=0;
								    
			}
			else  //leaf ---> store results
			{
			   limits lm=(limits) intervals.get(0);	
			   out+=nod+":";
			   
			   if(lm.getInf()>=0 && lm.getSup()>=0)
			   {			   
			   for(i=lm.getInf();i<=lm.getSup();i++)
			   out+=(ex.getTokens()).get(i)+" ";			   
			   }
			   else
			     out+="Error at Node: "+nod;
			     
				out+="\n" ;  
			}			
    	}
    	else
    	{
    		logger.debug("Null parameters in recursive parser");
    	}    	    	        
    }
    
    
    */
	/**
	 * @return String of rule names
	 */
	public String getRulesFromModel() {
		// TODO Auto-generated method stub
		String result=new String();
		
//		search leaves
     if(model!=null)
     {
		my_node root=(my_node)model.getRoot();		
		my_node leaf=(my_node)root.getFirstLeaf();
    	
	    while(leaf!=null)
				{	//print info
					//logger.debug("Nume: ["+frunza.label+"] Rule:"+frunza.rule_type);
					result+=leaf+"\n";
					leaf=(my_node)leaf.getNextLeaf();
				}
     }	
		return result;
	}
}
