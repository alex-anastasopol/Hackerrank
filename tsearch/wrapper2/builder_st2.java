/*
 * 
 * author : 
 * 
 */
package ro.cst.tsearch.wrapper2;

import java.util.Vector;
import org.apache.log4j.Category;

public class builder_st2
{	
	protected static final Category logger= Category.getInstance(builder_st2.class.getName());
	
	/** constructor with webpage **/
	public builder_st2()
	{ 
	   
	}
	/** generates the rule to extract the example e from a webpage **/
	/*
	void Vector stalker(HTML_region[] e)
	{
		Vector RetVal;
		int i;
		my_rule disjunct;
		RetVal=new Vector();//vector of rules
		i=0;
		while(i==0)
		{
			disjunct=LearnDisjunct(e);
			//remove examples matched
			RetVal.add(disjunct);
			i++;
		}
		return RetVal;
	}
	*/
	/** this function generates a rule to match examples **/
	/*
	private my_rule LearnDisjunct(e)
	{
		Item seed=e.getExampleAt(0);
		Item uncovered;
		
		my_rule candidates=getInitialCandidates(seed);
		my_rule best_refiner=null;
		my_rule best_solution=null;
		do
		{
			best_refiner=getBestRefiner(candidates);
			best_solution=best_refiner;
			if (isPerfect(best_solution,e)) break;
			
			else
			{
			uncovered=getUncovered(best_solution,e);			
			candidates=Refine(best_refiner,seed,uncovered);			
		    }
		}
		while(best_refiner!=null)		
		return best_solution;
	}
	*/
	/** this function returns the "best" rule  **/
	private my_rule getBestRefiner(my_rule rls)
	{
		return rls;
	}
	/** generate initial candidates - a rule that matches the sedd**/
	/*
	private my_rule getInitialCandidates(Item seed)
	{
		//generare regula
		
		//returneaza regula
	}
	*/
	
	/** generates forward rules for set of examples **/
	public my_rule iter_Forward(HTML_region[] e)
	{
		boolean possible=true,rez_refine; 
		
		int rez;
		
		HTML_region seed=e[0];
		
		my_rule FWR=find_forward_rule(seed);
		
		int n=e.length;
		
		int i=1;
		while(i<n && possible)
		{
			if(e[i]!=null)
			{
			logger.info("Aplic pt ex"+i);
			rez=applyForward(e[i],FWR);
			if(validateForward(e[i],rez)) i++;
			else
			{
				logger.info("Needs refine");
				rez_refine=RefineFWR(FWR,e[i]);	 			
				logger.info("After refine "+FWR+" rez_refine= "+rez_refine);
				if(rez_refine==false)
				   possible=add_FWR_token(FWR,seed);
				i=0;
			}
		    }
		    else
		    break;
		}
		
		if(possible)
		    return FWR;
		else
		   return null;    
	}
			
	/** generates backward rule for examples **/
	public my_rule iter_backWard(HTML_region[] e)
	{
		int i;
			boolean possible=true,rez_refine;
		
		int rez;
		//------------------
		HTML_region bkex=new HTML_region();;
		//Vector tokens;
		limits pg;
		limits zn;
		//---------------------
		HTML_region seed=e[0];
		
		my_rule BKR=find_backward_rule(seed);
		
		int n=e.length;
		
		i=1;
		while(i<n && possible)
		{
			if(e[i]!=null)
			{
			logger.info("Aplic pt ex"+i);
			//init new example
			  pg=e[i].getPageLimits();
			  
			  zn=e[i].getZoneLimits();
		      bkex=new HTML_region(e[i].getTokens());
		      //bkex.setPageLimits(new limits(zn.getInf(),pg.getSup()));
                      bkex.setPageLimits(new limits(zn.getSup(),pg.getSup()));
		      bkex.setZoneLimits(zn);
		  //---------------------------
			//rez=applyBackward(e[i],BKR);
			rez=applyBackward(bkex,BKR);
			if(validateBackward(e[i],rez)) i++;
			else
			{
				logger.info("Needs refine");
				rez_refine=RefineBKR(BKR,e[i]);	 			
				logger.info("After refine "+BKR+" rez_refineBKR= "+rez_refine);
				if(rez_refine==false)
				   possible=add_BKR_token(BKR,seed);
				i=0;
			}
		    }
		    else
		    break;
		}
		
		if(possible)
		    return BKR;
		else
		   return null;    
		
	}
	/** Reversing an example **/
	private HTML_region Reverse(HTML_region straight)
	{
		Vector oldv,newv;
		limits old_pg_lim,new_pg_lim;
		limits old_zn_lim,new_zn_lim;
		int i,linf,lsup;
		
		oldv=straight.getTokens();
		old_pg_lim=straight.getPageLimits();
		old_zn_lim=straight.getZoneLimits();
		
		newv=new Vector();
		for(i=oldv.size()-1;i>=0;i--)
		{
			newv.add((my_token)oldv.get(i));
		}
		
		linf=oldv.size()-1-old_pg_lim.getSup();
		lsup=oldv.size()-1-old_pg_lim.getInf();		
		
		
		  new_pg_lim=new limits(linf,lsup);
		  
		if(old_zn_lim!=null)
		{  
		
  		  linf=oldv.size()-1-old_zn_lim.getSup();
		  lsup=oldv.size()-1-old_zn_lim.getInf();		 
		
		  new_zn_lim=new limits(linf,lsup);
	    }
	    else
	     new_zn_lim=new limits(-1,-1);
		
		return new HTML_region(newv,new_pg_lim,new_zn_lim);		
	}
	
	/** this function returns the index in the tokens Vector
	 *  where the Backward rule is applied; should be the end of the
	 *  zone that needs to be extracted
	 */
	public int applyBackward(HTML_region e,my_rule BKR)
	{
		int v=applyForward(e,BKR);
		
		v=v-1-BKR.getActionAt(0).getPattern().size();
		
		//logger.info("Back Searched:"+(e.getZoneLimits()).getSup()+"Found:"+v);
		
		if(v<=e.getPageLimits().getSup() && v>=e.getPageLimits().getInf())
	       return v;
	    else 
	       return -1;		
	}
	
	/** adds a token to the forward rule **/
	private boolean add_FWR_token(my_rule FWR,HTML_region seed)
	{
		//get rule size (number of actions)		
		int sz=FWR.getActions().size();
		//compare with page limits
		limits pg=seed.getPageLimits();
		limits zn=seed.getZoneLimits();
		
		int rez;
		//if
		rez=zn.getInf()-sz;
		 if(rez>pg.getInf())
		 {//ok
              Vector tokens=seed.getTokens();
              //create a new patternToken with token k
			  patternToken pt=new patternToken(patternToken.MATCH_EXACT,(my_token)tokens.get(rez-1));			
			  //create a new action with this pattern Token
		      action act=new action(action.ACT_SKIPTO);			
		      act.addToken(pt);
			  //insert the action in the rule
			   FWR.insertAction(act);
              return true;
	 	 }		   
		return false;
      }		
      
      /** ads a token to the backward rule **/
    private boolean add_BKR_token(my_rule BKR,HTML_region seed)
	{
		//get rule size (number of actions)		
		int sz=BKR.getActionAt(0).getPattern().size();
		//compare with page limits
		limits pg=seed.getPageLimits();
		limits zn=seed.getZoneLimits();
		
		//get action 
		action act=BKR.getActionAt(0);
		
		int rez;
		//if
		rez=zn.getSup()+sz;
		 if(rez<pg.getSup())
		 {//ok
              Vector tokens=seed.getTokens();
              //create a new patternToken with token k
			  patternToken pt=new patternToken(patternToken.MATCH_EXACT,(my_token)tokens.get(rez-1));						  		      
		      act.addToken(pt);
			  //set the action in the rule
			   BKR.setActionAt(act,0);
              return true;
	 	 }		   
		return false;
      }		
      
      /** refines forward rule **/
   private boolean RefineFWR(my_rule FWR,HTML_region e)
	{
		//apply the rule until a mismatch happens
		//then adjust the rule 
			//rule actions
		Vector acts;
		Vector buf_tok;
		String codes;
		
		action crt_act;
		int crt_idx;
		int i;
		boolean match;
		
		//init stuff
		acts=FWR.getActions();
		crt_idx=e.getPageLimits().getInf();
		buf_tok=e.getTokens();
		codes=Vectorize(buf_tok);
		//for each action
		for(i=0;i<acts.size();i++)
		{
			crt_act=(action)acts.get(i);
			//for quick search
			crt_act.vectorize();			
			//execute action
			
			switch(crt_act.getType())
			{
		 case action.ACT_SKIPTO:
			 match=false;  
			 do
			 {
			      //identify sequence			      
			      crt_idx=codes.indexOf(crt_act.V_pattern,crt_idx);
			      
			     //refine action 
			      if(crt_idx==-1) 
			        {
//			        	logger.info("No match: rule not good!");
                        crt_act.setType(action.ACT_SKIPWHILE);
                        acts.setElementAt(crt_act,i);
                        FWR.setActions(acts);
			        	return true;
			        }			      
			      //match sequence
			      match=true;
			      for(int j=0;j<crt_act.pattern.size();j++)
			      {
			      	// patternToken pt=(patternToken)crt_act.pattern.get(j);
			      	 match=((patternToken)crt_act.pattern.get(j)).match((my_token)buf_tok.get(crt_idx+j));
			      	 if(!match) 
			      	 {
			      	 	crt_idx+=1;
			      	 	//missmatch adjust			      	 	
			      	 	  break;			      	 	
			      	 	//boolean rez=pt.nextMatchType();
			      	 	//if(rez)
			      	 	//{
			      	 	//	crt_act.pattern.setElementAt(pt,j);
			      	 		//acts.setElementAt(crt_act,i);
                            //FWR.setActions(acts);                        
			        	    //return true;
			      	 	//}
			      	 	  
			      	 	
			      	 }			      	 
			      }
			      
			      if(match==true)
			      {
//			      	logger.info("idx found="+rez_start+" lgth="+crt_act.V_pattern.length());
		          //   return rez_start+crt_act.V_pattern.length()-1;		
		            crt_idx+=crt_act.V_pattern.length();
			      }			      
			   } 
			   while(!match);				
				break;
		case action.ACT_SKIPWHILE:		     
			 do
			 {			      			      			     
			      //match sequence
			      match=true;
			      for(int j=0;j<crt_act.pattern.size();j++)
			      {
			      	 match=((patternToken)crt_act.pattern.get(j)).match((my_token)buf_tok.get(crt_idx+j));
			      	 if(!match) 
			      	  	    break;			      	 
			      }
			      
			      if(match==true)
			      {
//			      	logger.info("idx found="+rez_start+" lgth="+crt_act.V_pattern.length());
		          //   return rez_start+crt_act.V_pattern.length()-1;		
		            crt_idx+=crt_act.V_pattern.length();
			      }			      
			   } 
			   while(match);			      
		break;
			}			 
	    }
	    
	    //check for early matches
	    limits zn=e.getZoneLimits();
	    limits pg=e.getPageLimits();		 
	    int k,l;
	    logger.info("Refine early:crt_idx="+crt_idx+" zn:="+zn.getInf());
	    if(crt_idx<zn.getInf()) //try to refine 
	    {
	    	//get last SKIPTO action
	    	logger.info("Inside Refine early");
	    	k=acts.size();
	    	//if there are more tokens than add a new token
	    	if((pg.getInf()+k-1)<zn.getInf()) return false;
	    	
	    	crt_act=(action)acts.get(k-1);
	    	while(k>0)
	    	{
	    		k--;
	    		crt_act=(action)acts.get(k);
	    		if(crt_act.getType()==action.ACT_SKIPTO) break;	    		
	    	}	 
	    	logger.info("Inside Refine early action"+crt_act);   	
	    	if(k>=0)
	    	{
	    	    l=crt_act.pattern.size();
	    	    //try to refine the last token
	    	    while(l>0)
	    	    {
	    	    	l--; 		    		    
	    		    patternToken pt2=(patternToken)crt_act.pattern.get(l);	    		    
			      	if(pt2.nextMatchType())
			      	 	{
			      	 		crt_act.pattern.setElementAt(pt2,l);
			      	 		acts.setElementAt(crt_act,k);
                            FWR.setActions(acts);
			        	    return true;
			      	 	}			      	
	    	    }	
	    	    if(l==0) //al tokens are classes
	    	       {
	    	       	if(crt_act.getType()==action.ACT_SKIPTO)
	    	       	{
	    	       	  crt_act.setType(action.ACT_SKIPWHILE);
			      	  acts.setElementAt(crt_act,k);
                      FWR.setActions(acts);
			          return true;
			        } 
			        else return false;
	    	       }
	    	}
	    		    	
	    }
	    //logger.info("aaaaaaaaaaaaaaaaaaaaaaaaa");
		return false;
	}
	
	
	/** refines the backward rule **/
	private boolean RefineBKR(my_rule BKR,HTML_region e)
	{
		//apply the rule until a mismatch happens
		//then adjust the rule 
			//rule actions
		Vector acts;
		Vector buf_tok;
		String codes;
		
		action crt_act;
		int crt_idx;
		int i;
		boolean match;
		
		//init stuff
		acts=BKR.getActions();//only 1 actually
		//crt_idx=e.getPageLimits().getInf();
                crt_idx=e.getZoneLimits().getInf();
		buf_tok=e.getTokens();
		codes=Vectorize(buf_tok);
		//for each action
		for(i=0;i<acts.size();i++)
		{
			crt_act=(action)acts.get(i);
			//for quick search
			crt_act.vectorize();			
			//execute action
			
			switch(crt_act.getType())
			{
		 case action.ACT_SKIPTO: //only this type is allowed
			 match=false;  
			 do
			 {
			      //identify sequence			      
			      crt_idx=codes.indexOf(crt_act.V_pattern,crt_idx);
			      
			     //refine action 
			      if(crt_idx==-1) 
			        {
			        	logger.info("BACK: rule not good!");                        
			        	return false;
			        }			      
			      //match sequence
			      match=true;
			      for(int j=0;j<crt_act.pattern.size();j++)
			      {
			      	 patternToken pt=(patternToken)crt_act.pattern.get(j);
			      	 match=((patternToken)crt_act.pattern.get(j)).match((my_token)buf_tok.get(crt_idx+j));
			      	 if(!match) 
			      	 {
			      	 	crt_idx+=1;
			      	 	//missmatch adjust
			      	 	
			      	 	boolean rez=pt.nextMatchType();
			      	 	if(rez)
			      	 	{
			      	 		crt_act.pattern.setElementAt(pt,j);
			      	 		acts.setElementAt(crt_act,i);
                                                BKR.setActions(acts);
			        	    return true;
			      	 	}
			      	 }			      	 
			      }
			      
			      if(match==true)
			      {
//			      	logger.info("idx found="+rez_start+" lgth="+crt_act.V_pattern.length());
		          //   return rez_start+crt_act.V_pattern.length()-1;		
		            crt_idx-=1;		            
			      }			      
			   } 
			   while(!match);				
				break;	
			}//switch		
	    }//for
	    
	    //check for early matches
	    limits zn=e.getZoneLimits();		 
	    //int k,l;
	    logger.info("Refine early:crt_idx="+crt_idx+" zn:="+zn.getInf());
	    if(crt_idx<zn.getSup()) //try to refine 	    	    	
		    return false;
		return false;    
	}
	
	/** generates a forward rule for a certain example (seed)**/
	
	public my_rule find_forward_rule(HTML_region s)
	{
		Vector tokens=s.getTokens();
		limits pg=s.getPageLimits();
		limits zn=s.getZoneLimits();
		
		my_rule rule;
		patternToken pt;
		action act;
		
		int k,rez;
		
		rule=new my_rule("test rule");
		rule.setType(my_rule.RULE_EXTRACT);
		
		k=zn.getInf()-1;
		
		while(k>=pg.getInf())
		{
			//create a new patternToken with token k
			pt=new patternToken(patternToken.MATCH_EXACT,(my_token)tokens.get(k));			
			//create a new action with this pattern Token
		    act=new action(action.ACT_SKIPTO);			
		    act.addToken(pt);
			//insert the action in the rule
			rule.insertAction(act);
			
			//optimize rule
			//optimize_FWR(rule);
			
			//apply rule
			rez=applyForward(s,rule);			
			//if result ok then break
			if(validateForward(s,rez)) break;
			//next step
			k--;
		}
		return rule;		
	}
	/** this function optimizes the rule by eliminating unnecesary
	 *  pattern tokens
	 ***/
	private void optimize_FWR(my_rule r)
	{
		Vector acts;
		action crt_act,other_act;
		Vector tokens,other_tok;
		patternToken pt,pt2;
		Vector removed;
		int i,j;
		boolean found;
		
		if(r!=null)
		{
			removed=new Vector();
			acts=r.getActions();
			for(i=1;i<acts.size()-1;i++)
			{
				crt_act=(action)acts.get(i);
				tokens=crt_act.getPattern();
				pt=(patternToken)tokens.get(0);
				found=false;
				for(j=0;j<acts.size();j++) //for each token
				{
					if(j!=i)
					{
						other_act=(action)acts.get(j);
				        other_tok=other_act.getPattern();
				        pt2=(patternToken)other_tok.get(0);
				        if(pt.toString().equals(pt2.toString()))
				         {
				         	found=true;
				         	break;
				         }
					}					
				}
				if(!found)
				    removed.add(new Integer(i));												
			}
			for(i=0;i<removed.size();i++)
			{
				j=((Integer)removed.get(i)).intValue();
				r.removeAction(j);
			}
		}
	}
	/** generates the backward rule for a certain example **/
	public my_rule find_backward_rule(HTML_region s)
	{
		Vector tokens=s.getTokens();
		limits pg=s.getPageLimits();
		limits zn=s.getZoneLimits();
		
		HTML_region bkex;
		
		my_rule rule;
		patternToken pt;
		action act;
		
		int k,rez;
		
		//init new example
		bkex=new HTML_region(tokens);
		bkex.setPageLimits(new limits(zn.getInf(),pg.getSup()));
		bkex.setZoneLimits(zn);
		
		
		
		rule=new my_rule("test backward rule");
		rule.setType(my_rule.RULE_EXTRACT);
		//create a new action 
		    act=new action(action.ACT_SKIPTO);			
		    rule.addAction(act);
		    
		k=zn.getSup()+1;
		
		while(k<pg.getSup())
		{
			//create a new patternToken with token k
			pt=new patternToken(patternToken.MATCH_EXACT,(my_token)tokens.get(k));						
		    act.addToken(pt);
			//set the new action in the rule
			rule.setActionAt(act,0);
			//apply rule
			rez=applyBackward(bkex,rule);			
			//if result ok then break
			if(validateBackward(s,rez)) break;
			//next step
			logger.info("First back rule: "+rule);
			k++;
		}
		logger.info("First back rule: "+rule);
		return rule;		
	}
	
	/** this function returns the index in the tokens Vector
	 *  where the Forward rule is applied; should be the begining of the
	 *  zone that needs to be extracted
	 */
	
	public int applyForward(HTML_region e,my_rule r)
	{
		//rule actions
		Vector acts;
		Vector buf_tok;
		String codes;
		
		action crt_act;
		int crt_idx;
		int i;
		boolean match=false;
		
		//init stuff
		acts=r.getActions();
		crt_idx=e.getPageLimits().getInf();
		
		//test limita start
		if(crt_idx<0)  //cautare invalida
		   return crt_idx;
		   
		buf_tok=e.getTokens();
		//codes=Vectorize(buf_tok);
		codes=e.getVectorized();
		//for each action
		for(i=0;i<acts.size();i++)
		{
			crt_act=(action)acts.get(i);
			//for quick search
			crt_act.vectorize();			
			//execute action
			
			switch(crt_act.getType())
			{
		 case action_constants.ACT_SKIPTO:
			 match=false;  
			 do
			 {
			      //identify sequence			      
			      crt_idx=codes.indexOf(crt_act.V_pattern,crt_idx);
			      
			     //  logger.info("i="+rez_start);
			      if(crt_idx==-1) 
			        {
//			        	logger.info("No match: rule not good!");
			        	return crt_idx;
			        }			      
			      //match sequence
			      match=true;
			      for(int j=0;j<crt_act.pattern.size();j++)
			      {
			      	 match=((patternToken)crt_act.pattern.get(j)).match((my_token)buf_tok.get(crt_idx+j));
			      	 if(!match) 
			      	 {
			      	 	crt_idx+=1;
			      	    break;
			      	 }   
			      }
			      
			      if(match==true)
			      {
//			      	logger.info("idx found="+rez_start+" lgth="+crt_act.V_pattern.length());
		          //   return rez_start+crt_act.V_pattern.length()-1;		
		            crt_idx+=crt_act.V_pattern.length();
			      }			      
			   } 
			   while(!match);				
				break;
		case action_constants.ACT_SKIPWHILE:	
		    logger.info("Entering SkipWhile..");	     
			 do
			 {			      			      			     
			      //match sequence
			      match=true;			      
			      for(int j=0;j<crt_act.pattern.size();j++)
			      {
			      	 match=((patternToken)crt_act.pattern.get(j)).match((my_token)buf_tok.get(crt_idx+j));
			      	 if(!match) 
			      	  	    break;			      	 
			      }
			      
			      if(match==true)
			      {
			      	logger.info("Skipwhile idx found");
		          //   return rez_start+crt_act.V_pattern.length()-1;		
		            crt_idx+=crt_act.V_pattern.length();		            
			      }			      
			   } 
			   while(match);			      
		break;
		case action_constants.ACT_SKIPUNTIL:
		     do
		     {
		     	match=true;
//				identify sequence			      
				 crt_idx=codes.indexOf(crt_act.V_pattern,crt_idx);
				if(crt_idx==-1)
								{
									match=false;
									break;				
								}
				 //try to match
				for(int j=0;j<crt_act.pattern.size();j++)
				{
					 match=((patternToken)crt_act.pattern.get(j)).match((my_token)buf_tok.get(crt_idx+j));
					 if(!match)
					 {
					 	crt_idx+=1;
						break; 
					 }
				}		     	
		     	//if match true than stop at that index		     	
		     }
		     while(!match);		
		break;
		case action_constants.ACT_SKIPNR:		    
		     crt_idx+=2*crt_act.V_pattern.length();
			// logger.info("Skip NR  idx found="+crt_idx);
		break;
		case action_constants.ACT_SKIPNRFW:		    
					 crt_idx+=crt_act.V_pattern.length();
					// logger.info("Skip NR  idx found="+crt_idx);
				break;				
			}
			 
	    }
	    //crt_idx<=e.getPageLimits().getSup() && 
	    if(crt_idx>e.getPageLimits().getInf())
	       return crt_idx;
	    else 
	       return -1;//notfound
	}	
		
	/** returns a String of HTML token codes **/
	private String Vectorize(Vector HTML_tokens)
	{
		String rez=new String();		
		int i;
		my_token m;		
		for(i=0;i<HTML_tokens.size();i++)
		{
		  m=(my_token) HTML_tokens.get(i);	
		  rez+=new Integer(m.cod).toString();
		}  
		return rez;
	}
	/** validate a rule application**/
	private boolean validateForward(HTML_region e,int idx)
	{
		limits zn=e.getZoneLimits();
		return (idx==zn.getInf());
	}
	/** validate a rule application**/
	private boolean validateBackward(HTML_region e,int idx)
	{
		limits zn=e.getZoneLimits();
		return (idx==zn.getSup());
	}
	/** optimizing a solution **/
	/*
	private boolean postProcess(HTML_region exs[],my_rule fwr,my_rule bkr)
	{
		boolean optimized=false;
		Vector old_actions,new_actions;//actions of a rule
		int i,j;
		//optimize fwd rule trying to merge 2 actions
		
		
		return optimized;
	}*/
}