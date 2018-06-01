/*
 * 
 * author : 
 * deprecated
 */
package ro.cst.tsearch.instructor;

import java.io.*;
import java.util.*;
import javax.swing.tree.*;

import ro.cst.tsearch.wrapper2.HTML_region;
import ro.cst.tsearch.wrapper2.action;
import ro.cst.tsearch.wrapper2.builder_st2;
import ro.cst.tsearch.wrapper2.limits;
import ro.cst.tsearch.wrapper2.my_node;
import ro.cst.tsearch.wrapper2.my_parser;
import ro.cst.tsearch.wrapper2.my_rule;
//import ro.cst.tsearch.wrapper2.my_token;
import ro.cst.tsearch.wrapper2.patternToken;

import org.apache.log4j.Logger;

/** this class is used to generate rules of extraction
 * from a tree model 
 * 
 */
class builder
{
	private static final Logger logger = Logger.getLogger(builder.class);
	/**
	 * tree model root - this represents the entire HTML*/
 	my_node root;
 	/** this node is a leaf ; this is the goal of extraction **/
     	my_node frunza;
    /** the path = nodes that need to be extrated to reach the leaf **/ 	
     	TreeNode path[];
    /**  the tree model **/ 	
     	DefaultTreeModel treeModel;	

    /**   leafs vector **/
      Vector leafs;
	/** parser engine **/
	   my_parser mp;
	/** result tokens after parsing **/  
	  Vector buf_tok;
	/** vector of rules **/
	  Vector rules;
	
	/** set of rules for a path **/
	  Vector set_rules;
	
	/**   vector node_rules **/
	  Vector node_rules=null;
	
	
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
	  
          public builder(DefaultTreeModel tm) {
              treeModel=tm;            
          }
    
    
    public void build_rules2(Vector tokens[],my_node mn,int maxp)
    {
    	builder_st2 bst2=new builder_st2();
    	HTML_region ex[]=new HTML_region[maxp];
    	int i;
    	int idx_start;
    	int idx_end;
    	limits lims;
    	
    	
    	
    	my_node parinte;
    	parinte=(my_node)mn.getParent();
    	if(mn.getType()==0) //extraction
    	{
    		if(parinte!=null)
    	{    		
    	    logger.info("Construiesc reguli pt. ["+mn.label+"]");
    	    
    	    //pentru fiecare profile generez un exemplu
    	    for(i=1;i<=maxp;i++)
    	    {
    	    	 ex[i-1]=new HTML_region(tokens[i]);
    	         //setez limitele parintelui
    		     idx_start=parinte.cds[i][0];
    		     idx_end=parinte.cds[i][1];	
    		     lims=new limits(idx_start,idx_end);
    		     ex[i-1].setPageLimits(lims);
    		     //setez limitele exemplului
    		     idx_start=mn.cds[i][0];
    		     idx_end=mn.cds[i][1];	
    		     lims=new limits(idx_start,idx_end);
    		     ex[i-1].setZoneLimits(lims);    		     
    	    }    	        		    		
    	    //forward generation
    	    my_rule r=bst2.iter_Forward(ex);
    	    mn.rule_fwd=r;
    	    logger.info("Regula FWR:"+r);
    	    r.setName(mn.label);
    	
    		
    		//backward generation
    		my_rule bkr=bst2.iter_backWard(ex);
    		mn.rule_bkd=bkr;
    		logger.info("Regula BKR:"+bkr);
    		bkr.setName(mn.label);
    	    		    		    		    		    		
    	}
    	}
    	else //iteration node rules already generated
    	{
    		mn.rule_fwd.type=my_rule.RULE_ITERAT;
    		mn.rule_bkd.type=my_rule.RULE_ITERAT;
    		
    		mn.rule_fwd.setName(mn.label);
    		mn.rule_bkd.setName(mn.label);
    		
    	
    	}    	    	
    }
        
  
    public void gen_rules2(Vector tokens[],int maxp)
    {
        
        //set root 
              root=(my_node)treeModel.getRoot();
         	//Get_leafs_from_tree
    	leafs=new Vector();

    	
    	frunza=(my_node)root.getFirstLeaf();
    	while(frunza!=null)
    	{
    		leafs.add((my_node)frunza);
    		//print info
    		logger.info("Nume: ["+frunza.label+"] Rule:"+frunza.rule_type);
    		frunza=(my_node)frunza.getNextLeaf();
    	}
        int	i=0;
        

    	//for each leaf    	
    	for (i=0;i<leafs.size();i++)
    	{
    	
    	
    		frunza=(my_node)leafs.get(i);    	    	
    		path=treeModel.getPathToRoot(frunza);
    		set_rules=new Vector();    		
            for(int j=1;j<path.length;j++)
    	    {    	    
    	          build_rules2(tokens,(my_node)path[j],maxp);    	              	    
    	    }                	    
     	}    
    }
    
    private void get_rules_from_tree()
    {
    	//set root 
              root=(my_node)treeModel.getRoot();
                       
        //Get_leafs_from_tree
    	leafs=new Vector();    	
    	
    	frunza=(my_node)root.getFirstLeaf();
    	while(frunza!=null)
    	{
    		leafs.add((my_node)frunza);
    		//print info
    		logger.info("Nume: ["+frunza.label+"] Rule:"+frunza.rule_type);
    		frunza=(my_node)frunza.getNextLeaf();
    	}
        int	i=0;
        
        node_rules=new Vector();
    	//for each leaf    	
    	for (i=0;i<leafs.size();i++)
    	{
    		frunza=(my_node)leafs.get(i);    	    	
    		path=treeModel.getPathToRoot(frunza);
    		set_rules=new Vector();    		
            for(int j=1;j<path.length;j++)
    	    {    
    	         rules=new Vector();
    	         my_node mn=(my_node)path[j];	
      		     rules.add(mn.rule_fwd);    		
    		     rules.add(mn.rule_bkd);
    	   		set_rules.add(rules);    
    	    
    	    }            
    	    node_rules.add(set_rules);        
     	}                	
    }
    public void save_rules2(String file_name)
    {
    	//int i;    	    	
    	//get rules fromtree model
    	get_rules_from_tree();
    	//save
    	try
    	{
    		FileOutputStream ostream = new FileOutputStream(file_name);
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            p.writeObject(node_rules);
            p.close();
    	}
    	catch(Exception e)
    	{
    		logger.error("Save failed:"+e);
    	}
	
        logger.info("done");    	
    }

}