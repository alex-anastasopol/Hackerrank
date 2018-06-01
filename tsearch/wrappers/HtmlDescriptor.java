package ro.cst.tsearch.wrappers;

import java.util.ArrayList;

public class HtmlDescriptor
{
	private HtmlNode root;
	//
	public HtmlDescriptor(Rules Data)
	{
		ArrayList t= new ArrayList();
		root= new HtmlNode(t,Data,"HTML",0);
	}
	//
	public HtmlNode addNode(ArrayList aiFullPath2Parent,Rules Data,String sName, int iInfSet)
	{
		int i;
		HtmlNode parent= root.findChild(aiFullPath2Parent);
		if (parent != null)
		{
			ArrayList ainew= new ArrayList();
			for (i= 0; i < aiFullPath2Parent.size(); i++)
				ainew.add( aiFullPath2Parent.get(i));
			ainew.add(new Integer(parent.getChildrenCount()));
			return parent.addChild(new HtmlNode(ainew,Data,sName, iInfSet));
		}
		return null;
	}
	public HtmlNode getNode(ArrayList aiFullPath)
	{
		return root.findChild(aiFullPath);
	}
	
}