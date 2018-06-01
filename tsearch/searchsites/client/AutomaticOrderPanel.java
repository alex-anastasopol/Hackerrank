package ro.cst.tsearch.searchsites.client;

import java.util.HashMap;
import java.util.Vector;

import ro.cst.tsearch.searchsites.client.SearchSitesBuckets.SearchSitesBucket;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

public class AutomaticOrderPanel extends HorizontalPanel{
	   
	 //  private Vector allSitesNames ;
	   private Vector<Object> vecCountyLabels;
	   private int specialPozition;
	   Button butLeft = new Button(" <- ");
	   Button butRight = new Button(" -> ");
	   {
		   butLeft.setStyleName("button");
		   butRight.setStyleName("button");
	   }
	   
//	   AutomaticOrderPanel (Vector<String> allSitesNames,int specialPozition){
//		   this.specialPozition = specialPozition;
//		   //this.allSitesNames = allSitesNames;
//		   
//		   vecCountyLabels =  transformINCountyLabels(allSitesNames);
//		   ((Anchor)vecCountyLabels.get(specialPozition)).addStyleName("redLink");
//		   
//		   reset();
//		   butLeft.addClickListener(new ClickListener(){
//			   public void onClick(Widget arg0) {
//				   moveLeft();
//			   }
//		   });
//		   butRight.addClickListener(new ClickListener(){
//			   public void onClick(Widget arg0) {
//				   moveRight();
//			   }
//		   });
//	   }
	   
	   HashMap<String, SearchSitesBucket> buckets = new HashMap<String, SearchSitesBucket>();
	   
	   AutomaticOrderPanel (Vector<GWTDataSite> allSites,int specialPozition){
		   this.specialPozition = specialPozition;
		   //this.allSitesNames = allSitesNames;
		   
		   Vector<String> allSitesNames = new Vector<String>();
		   
		   for(GWTDataSite ds : allSites){
			   allSitesNames.add(ds.getStateAbrv() + ds.getCountyName() + ds.getSiteTypeAbrv() );
			   buckets.put(ds.getStateAbrv() + ds.getCountyName() + ds.getSiteTypeAbrv() , SearchSitesBuckets.getSiteBucket(ds.getType()));
		   }
		   
		   vecCountyLabels =  transformINCountyLabels(allSitesNames);
		   ((Anchor)vecCountyLabels.get(specialPozition)).addStyleName("redLink");
		   
		   reset();
			butLeft.addClickListener(new ClickListener() {
	
				@Override
				public void onClick(Widget sender) {
					moveLeft();
				}
			});
	
			butRight.addClickListener(new ClickListener() {
				public void onClick(Widget arg0) {
					moveRight();
				}
			});
	   }
	   
	   
	   private Vector<Object> transformINCountyLabels(Vector<String> allSitesNames ){
			Vector<Object> v = new Vector<Object>();
			for(int i=0;i<allSitesNames.size();i++){
		    	Anchor siteNameLink = new Anchor(allSitesNames.get(i)+"", true);
		    	siteNameLink.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						Anchor source = (Anchor) event.getSource();
						int selectedPos = vecCountyLabels.indexOf(source);
						((Anchor)vecCountyLabels.get(specialPozition)).removeStyleName("redLink");
						source.addStyleName("redLink");
						specialPozition = selectedPos;
					}
		    	});
		    	v.add(siteNameLink);
			}
			return v;
		}
	   
	   private void reset(){
		   super.clear();
		   for(int i=0;i<vecCountyLabels.size();i++){
			   super.add((Widget)vecCountyLabels.get(i));
		   }
		   
		   super.add(butLeft);
		   super.add(butRight);
		   
	   }
	   
		private int  moveLeft ( ){ // within bucket
			
			int newpoz = specialPozition;
			
//			if( specialPozition == 0){
//				Object obj = vecCountyLabels.remove(specialPozition);
//				vecCountyLabels.add(obj );
//				newpoz = vecCountyLabels.size() -1;
//			}
//			else 
			if (specialPozition > 0 && specialPozition <= vecCountyLabels.size() -1){
				newpoz = specialPozition-1;
				
				if(buckets.containsKey(((Anchor)vecCountyLabels.get(newpoz)).getText()) && buckets.containsKey(((Anchor)vecCountyLabels.get(specialPozition)).getText())){
					if(buckets.get(((Anchor)vecCountyLabels.get(newpoz)).getText()).getLowIndex() == buckets.get(((Anchor)vecCountyLabels.get(specialPozition)).getText()).getLowIndex()){
						Object temp = vecCountyLabels.remove(specialPozition);
						vecCountyLabels.add(newpoz , temp);
						
						reset();
						specialPozition = newpoz;
					}
				}
			}
			
			return specialPozition;
		}
		
	 private int  moveRight ( ){
			
			int newpoz = specialPozition;
			
//			if( specialPozition == vecCountyLabels.size() -1){
//				Object obj = vecCountyLabels.remove(specialPozition);
//				vecCountyLabels.add(0,obj );
//				newpoz = 0;
//			}
//			else 
				
			if (specialPozition >= 0 && specialPozition < vecCountyLabels.size() -1){
				newpoz = specialPozition+1;
				
				if(buckets.containsKey(((Anchor)vecCountyLabels.get(newpoz)).getText()) && buckets.containsKey(((Anchor)vecCountyLabels.get(specialPozition)).getText())){
					if(buckets.get(((Anchor)vecCountyLabels.get(newpoz)).getText()).getLowIndex() == buckets.get(((Anchor)vecCountyLabels.get(specialPozition)).getText()).getLowIndex()){
						Object temp = vecCountyLabels.remove(specialPozition);
						vecCountyLabels.add(newpoz , temp);
						
						reset();
						specialPozition = newpoz;
					}
				}
			}
			
			return specialPozition;
		}
	 
	 public String [] getSitesNamesOrdered(){
		 String[] ret = new String[vecCountyLabels.size()];
		 for(int i=0;i<vecCountyLabels.size();i++){
			 ret[i] = ((Anchor)vecCountyLabels.get(i)).getText();
		 }
		 return ret;
	 }
	 
}
