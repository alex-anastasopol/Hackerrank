package ro.cst.tsearch.servers.response;
public interface DocSplitter 
{
	void setDoc(String doc);
	String getDoc();
	int getSplitNo();
	String getNextLink();
	String getPrevLink();	
	//get docs - index starting from 0
	String getSplitDoc(int idx);
}