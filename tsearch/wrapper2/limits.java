/*
 * 
 * author:
 */
package ro.cst.tsearch.wrapper2;


/** this class stores 2 indexes for an interval **/
public class limits
{
	/** inferior limit **/
	public int inf_limit;
	/** superior limit **/
	public int sup_limit;
	
	public limits()
	{
	}		
	public limits(int inf,int sup)
	{
		inf_limit=inf;
		sup_limit=sup;
	}
	public int getInf()
	{
		return inf_limit;
	}
	public int getSup()
	{
		return sup_limit;
	}
	public void setInf(int k)
	{
		inf_limit=k;
	}
	public void setSup(int k)
	{
        sup_limit=k;
	}
}