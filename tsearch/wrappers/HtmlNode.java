package ro.cst.tsearch.wrappers;
import java.util.ArrayList;
/////////////////////////////////////////////////////////////////
public class HtmlNode
{
	private Rules mData;
	private String msName;
	private int miInfSet;
	private ArrayList maiFullPath;
	private ArrayList children;
	//
	public HtmlNode(ArrayList aiFullPath, Rules Data,String sName, int iInfSet)
	{
		mData= Data;
		msName=sName;
		miInfSet=iInfSet;
		this.maiFullPath= CopyArray(aiFullPath, false);
		children= new ArrayList();
	}
	//
	public HtmlNode findChild(ArrayList aiPath)
	{
		HtmlNode node= null;
		if (aiPath.size() != 0)
		{
			Integer p=(Integer) aiPath.get(0);
			node= ((HtmlNode) children.get(p.intValue())).findChild(CopyArray(aiPath,true));
		}
		else
			node= this;
		return node;
	}
	//
	public HtmlNode findChild(int index)
	{
		return (HtmlNode) children.get(index);
	}
	//
	public HtmlNode addChild(HtmlNode node)
	{
		children.add(node);
		return node;
	}
	//
	public int getChildrenCount()
	{
		return children.size();
	}
	private ArrayList CopyArray(ArrayList ai, boolean bRemoveFirst)
	{
		ArrayList ainew=new ArrayList();
		int iStart;
		if (bRemoveFirst)
			iStart= 1;
		else
			iStart= 0;
		for (int i= iStart; i < ai.size(); i++)
			ainew.add(ai.get( i));
		return ainew;
	}
	/**
	 * Returns the data.
	 * @return Rules
	 */
	public Rules getData()
	{
		return mData;
	}
	
	/**
	 * Returns the infSet.
	 * @return int
	 */
	public int getInfSet()
	{
		return miInfSet;
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName()
	{
		return msName;
	}
	public int getPosition()
	{
		if (maiFullPath.size()==0)//root
			return 0;
		else
			return ((Integer)maiFullPath.get( maiFullPath.size() -1)).intValue() ;
	}
}
