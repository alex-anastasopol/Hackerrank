
/*
 * 
 * 
 */
package ro.cst.tsearch.wrapper2;

import java.util.Vector;
import org.apache.log4j.Category;

/** this class is used to store examples for rule genreation algorithm;
 *  it is also used to extract data from a HTML file
 ***/
public class HTML_region
{
	protected static final Category logger= Category.getInstance(HTML_region.class.getName());
	
	/** the HTML tokens of the entire webpage **/
	Vector tokens;
	/** the page limits where the search is to be applied**/
	limits pg_limits;
	/** the zone limits that needs to be identified (for rule generation only) **/
	limits zone;
	
	/** vectorized tokens **/
	String V_tokens;
	
	public HTML_region()
	{
		
	}
	public HTML_region(Vector toks)
	{
		tokens=toks;
		vectorize();
	}
	public HTML_region(Vector toks,String V_toks,limits pg)
	{
		tokens=toks;
		V_tokens=V_toks;
		pg_limits=new limits(pg.getInf(),pg.getSup());
	}
	public HTML_region(Vector tok,limits pg,limits zn)
	{
		tokens=tok;
		pg_limits=pg;
		zone=zn;
		vectorize();
	}
	public HTML_region(Vector tok,int pg_linf,int pg_lsup,int zn_inf,int zn_sup)
	{
		tokens=tok;
		pg_limits=new limits(pg_linf,pg_lsup);
		zone=new limits(zn_inf,zn_sup);
		vectorize();
	}	
	/** returns the tokens of this HTML_region **/
	public Vector getTokens()
	{
		return tokens;
	}
	public void setTokens(Vector toks)
	{
		tokens=toks;
		vectorize();
	}
	public limits getPageLimits()
	{
		return pg_limits;
	}
	public void setPageLimits(limits l_pg)
	{
		pg_limits=l_pg;
	}
	public void setZoneLimits(limits l_zn)
	{
	    zone=l_zn;
	}
	public limits getZoneLimits()
	{
		return zone;
	}
	/** generates the String of HTML token types **/
	public void vectorize()
	{				
		int i;
		
		//logger.info("Apelez HTML_region vectorize");
		my_token m;
		StringBuffer sbf=new StringBuffer();		
		V_tokens=new String();		
		for(i=0;i<tokens.size();i++)
		{
		  m=(my_token)tokens.get(i);	
		  //V_tokens+=new Integer(m.cod).toString();
		  sbf.append((new Integer(m.cod)).toString());
		}  
		V_tokens=sbf.toString();
	}
	/** returns the vectorized tokens **/
	public String getVectorized()
	{
		return V_tokens;
	}
    public String toString()
    {
            String result=new String();
            if(zone!=null)
            {
            for(int i=zone.getInf();i<=zone.getSup();i++)
                result+=(my_token)tokens.get(i)+" ";            
            }
            else
              result+="NO_ZONE_DEFINED";
            return result;            
    }
}