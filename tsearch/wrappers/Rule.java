package ro.cst.tsearch.wrappers;

import java.util.ArrayList;

public class Rule
{
	private ArrayList mLandmarks=new ArrayList();
	public void addLandmark(Landmark l)
	{
		mLandmarks.add(l);
	}
	public Landmark getLandmark(int index)
	{
		return (Landmark)mLandmarks.get (index);
	}
	public int getLandmarksCount()
	{
		return mLandmarks.size() ;
	}
}
