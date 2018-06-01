package ro.cst.tsearch.titledocument.abstracts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Address;
import ro.cst.tsearch.propertyInformation.Family;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.propertyInformation.Owner;
import ro.cst.tsearch.propertyInformation.Person;
import ro.cst.tsearch.servers.parentsite.County;
import ro.cst.tsearch.servers.parentsite.State;
import ro.cst.tsearch.user.UserAttributes;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.StringUtils;

public class FormatSa
{
	protected static final Category logger= Category.getInstance(FormatSa.class.getName());
	
	public static String getAgentName(SearchAttributes sa){
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCrtSearchContext().getAgent();
		if(ua!=null){
			return ua.getFIRSTNAME() + " " + ua.getMIDDLENAME()+ " " + ua.getLASTNAME();
			
		}
		return "";
	}
	
	public static String getAgentCompanyName(SearchAttributes sa){
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(sa.getSearchId()).getCrtSearchContext().getAgent();
		if(ua!=null){
			return (ua.getCOMPANY()==null || ua.getCOMPANY().equals(""))?getAgentName(sa):ua.getCOMPANY();
		}
		return "";
	}
	
	public static String getAgentWorkAddressForCurrentSearch(long searchId){
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent();
		if(ua!=null){
			String address = (String )ua.getWADDRESS();
			if(address!=null){
				return address;
			}
		}
		return "";
	}
	
	
	public static String getAgentWorkZipCodeForCurrentSearch(long searchId){
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent();
		if(ua!=null){
			String address = (String )ua.getWZCODE();
			if(address!=null){
				return address;
			}
		}
		return "";
	}
	
	public static String getAgentWorkCountyForCurrentSearch(long searchId){
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent();
		if(ua!=null){
			String address = (String )ua.getWCONTRY();
			if(address!=null){
				return address;
			}
		}
		return "";
	}
	
	public static String getAgentWorkCityForCurrentSearch(long searchId){
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent();
		if(ua!=null){
			String address = (String )ua.getWCITY();
			if(address!=null){
				return address;
			}
		}
		return "";
	}
	
	public static String getAgentWorkStateForCurrentSearch(long searchId){
		UserAttributes ua = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getAgent();
		if(ua!=null){
			String address = (String )ua.getWSTATE();
			if(address!=null){
				return address;
			}
		}
		return "";
	}
	
	/**
		 * Method getPropertyAddress.
		 * @param searchAttributes
		 * @return String
		 */
		public static String getPropertyAddress(SearchAttributes sa)
		{
			Address a = sa.getPropertyAddress();
			String rez = a.toString(Address.FORMAT_DEFAULT);
			return rez;
		}

	public static String getPCountyName(SearchAttributes sa){
		String rez = "";
		try {
			rez = County.getCounty(new BigDecimal(sa.getAtribute(SearchAttributes.P_COUNTY))).getName();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("error retriving county from db" , e);
		}
		return rez;
	}

	public static String getPStateNameAbbrev(SearchAttributes sa){
		return getStateNameAbbrev(sa.getAtribute(SearchAttributes.P_STATE));
	}

	public static String getStateNameAbbrev(String stateId){
		String rez = "";
		if (!StringUtils.isStringBlank(stateId)){
			try {
				rez = State.getState(new BigDecimal(stateId)).getStateAbv();
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("error retriving state from db" , e);
			}
		}
		return rez;
	}

	public static void setBookAndPage(SearchAttributes sa, String bookandPage, boolean platBookNew, boolean isPlat) {
		List books = new ArrayList();
		List pages = new ArrayList();
		splitBooksPages(bookandPage, books, pages);
		
		String book = "";
		String page = "";
		int last = books.size() -1; 
		if (last>=0){
			book = ((String) books.get(last)).trim();
			page = ((String) pages.get(last)).trim();
		}
		if(platBookNew){
			sa.setAtribute(SearchAttributes.LD_BOOKNO_1, book);
			sa.setAtribute(SearchAttributes.LD_PAGENO_1, page);
			
			//these needs for Search Page
			sa.setAtribute(SearchAttributes.LD_BOOKNO, book);
			sa.setAtribute(SearchAttributes.LD_PAGENO, page);
		}
		else if(isPlat){
			sa.setAtribute(SearchAttributes.LD_BOOKNO, book);
			sa.setAtribute(SearchAttributes.LD_PAGENO, page);
		} else {
			sa.setAtribute(SearchAttributes.LD_BOOKPAGE,combineBooksPages(books, pages));
		}
		
	}
	
	public static void addBookAndPage(SearchAttributes sa, String book, String page, boolean platBookNew, boolean isPlat) {
		if (StringUtils.isStringBlank(book) || StringUtils.isStringBlank(page)){
			return ;
		}

		page = page.replaceAll( "(?is)(\\d+)-(\\d+)" , "$1");
		
		String newBookPage = book.trim() + "-" + page.trim();
		
		ArrayList l = new ArrayList(StringUtils.splitAfterDelimitersList( sa.getAtribute(SearchAttributes.LD_BOOKPAGE), new String[] {","}));

		if (!l.contains(newBookPage)){
			l.add(newBookPage);
			setBookAndPage(sa, StringUtils.join(l,","), platBookNew, isPlat);
		}
	}

	public static void removeBookAndPage(SearchAttributes sa, String book, String page) {
		if (StringUtils.isStringBlank(book) || StringUtils.isStringBlank(page)){
			return ;
		}

		ArrayList l = new ArrayList(StringUtils.splitAfterDelimitersList( sa.getAtribute(SearchAttributes.LD_BOOKPAGE), new String[] {","}));
		l.remove(book.trim() + "-" + page.trim());
		setBookAndPage(sa, StringUtils.join(l,","), false, false);
	}

	private static String combineBooksPages(List books, List pages) {
		List l = new ArrayList();
		for (int i=0; i<books.size(); i++){
			String book = ((String) books.get(i)).trim();
			String page = ((String) pages.get(i)).trim();
			l.add (book + "-" + page);
		}
		 return StringUtils.join(l, ",");
	}

	private static void splitBooksPages(String s, List books, List pages) {
		List l = StringUtils.splitAfterDelimitersList( s, new String[] {","});
		
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			String bookpage = (String) iter.next();
			String[] l1 = bookpage.split("[-,_]");
			if (l1.length == 2){
				books.add(l1[0].trim());
				pages.add(l1[1].trim());
			}
		}
	}
}
