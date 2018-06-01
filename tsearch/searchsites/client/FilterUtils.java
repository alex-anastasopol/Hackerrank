package ro.cst.tsearch.searchsites.client;

import java.util.Vector;


public class FilterUtils {
	
	public static final String ALL ="ALL";
	public static final String NONE ="NONE";
	
	public static GWTDataSite[]  filter(GWTDataSite[] siteData,String community, String state,String county,String type){
		
		Vector v = new Vector();
		
		boolean ignoreState = ALL.equals(state);
		boolean ignoreCounty = ALL.equals(county);
		boolean ignoreType = ALL.equals(type);
		
		for(int i=0;i<siteData.length;i++){
			GWTDataSite curData = siteData[i];			
			boolean isGood = curData!=null 
				&& (curData.getStateAbrv().equals(state) || ignoreState) 
				&& (curData.getCountyName().equals(county) || ignoreCounty)  
				&& (curData.getSiteTypeAbrv().equals(type)|| ignoreType);
			if(isGood ){
				v.add(curData);
			}
		}
		
		GWTDataSite[] data = new GWTDataSite[v.size()];
		
		for(int i=0;i<data.length;i++){
			data[i] =((GWTDataSite) v.get(i));
			data[i].setCommunity(community);
		}
		
		return data;
	}
	
}
