package ro.cst.tsearch.wrappers;
import java.util.ArrayList;
public class Landmark
{
	public static final int TYPE_SKIP_TO= 0;
	private ArrayList mTokens= new ArrayList();
	private int miLandMarkType;
	public void addToken(String t)
	{
		mTokens.add(t);
	}
	public String getToken(int index)
	{
		return (String) mTokens.get(index);
	}
	public int getTokensCount()
	{
		return mTokens.size();
	}
	public String getBody()
	{
		String rtrn= "";
		int i;
		for (i= 0; i < mTokens.size(); i++)
			rtrn += " " + mTokens.get(i);
		if (i!=0)
			rtrn=rtrn.substring( 1);
		return rtrn;
	}
	/**
	 * Returns the landMarkType.
	 * @return int
	 */
	public int getLandMarkType()
	{
		return miLandMarkType;
	}
	/**
	 * Sets the landMarkType.
	 * @param landMarkType The landMarkType to set
	 */
	public void setLandMarkType(int landMarkType)
	{
		miLandMarkType= landMarkType;
	}
}
