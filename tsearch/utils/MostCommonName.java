/*
 * Created on Jan 13, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ro.cst.tsearch.utils;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * @author george
 */
public class MostCommonName {
	
	private static final Set<String> commonLastPrefixes = new HashSet<String>();
	private static final Set<String> commonFirstPrefixes = new HashSet<String>();
		
	static {
		
		Hashtable<String,String> MCLN = new Hashtable<String,String>();
		Hashtable<String,String> MCFN = new Hashtable<String,String>();
		
		MCLN.put("smith","1");		MCLN.put("johnson","2");		MCLN.put("williams","3");
		MCLN.put("jones","4");		MCLN.put("brown","5");			MCLN.put("davis","6");
		MCLN.put("miller","7");		MCLN.put("wilson","8");			MCLN.put("moore","9");
		MCLN.put("taylor","10");	MCLN.put("anderson","11");		MCLN.put("thomas","12");
		MCLN.put("jackson","13");	MCLN.put("white","14");			MCLN.put("harris","15");
		MCLN.put("martin","16");	MCLN.put("thompson","17");		MCLN.put("garcia","18");
		MCLN.put("martinez","19");	MCLN.put("robinson","20");		MCLN.put("clark","21");
		MCLN.put("rodriguez","22");	MCLN.put("lewis","23");			MCLN.put("lee","24");
		MCLN.put("walker","25");	MCLN.put("hall","26");			MCLN.put("allen","27");
		MCLN.put("young","28");		MCLN.put("hernandez","29");		MCLN.put("king","30");
        MCLN.put("samuel","31");    MCLN.put("williamson","32");	MCLN.put("jefferson","33");
        MCLN.put("simmons","34");	MCLN.put("adams","35");			MCLN.put("banks","36");
        MCLN.put("bernard","37");	MCLN.put("black","38");			MCLN.put("bond","39");
        MCLN.put("bullock","40");	MCLN.put("campbell","41");		MCLN.put("charles","42");
        MCLN.put("charleston","43"); MCLN.put("crawford","44");		MCLN.put("davenport","45");
        MCLN.put("davidson","46");	MCLN.put("douglas","47");		MCLN.put("edmonds","48");
        MCLN.put("fisher","49");	MCLN.put("ford","50");			MCLN.put("fox","51");
        MCLN.put("frank","52");		MCLN.put("franklin","53");		MCLN.put("fuller","54");
        MCLN.put("garett","55");	MCLN.put("gibson","56");		MCLN.put("hanks","57");
        MCLN.put("harper","58");	MCLN.put("henderson","59");		MCLN.put("hilton","60");
        MCLN.put("huston","61");	MCLN.put("houston","62");		MCLN.put("irwin","63");
        MCLN.put("james","64");		MCLN.put("jefferson","65");		MCLN.put("kelly","66");
        MCLN.put("lancaster","67");	MCLN.put("lopez","68");			MCLN.put("michaels","69");
        MCLN.put("montgomery","70"); MCLN.put("morgan","71");		MCLN.put("morris","72");
        MCLN.put("mc donald","73");	MCLN.put("newton","74");		MCLN.put("nicholson","75");
        MCLN.put("norris","76");	MCLN.put("norton","77");		MCLN.put("parker","78");
        MCLN.put("peterson","79");	MCLN.put("powell","80");		MCLN.put("quinn","81");
        MCLN.put("redford","82");	MCLN.put("reeves","83");		MCLN.put("roberts","84");
        MCLN.put("robertson","85");	MCLN.put("simpson","86");		MCLN.put("smithson","87");
        MCLN.put("stallone","88");	MCLN.put("stewart","89");		MCLN.put("swanson","90");
        MCLN.put("thurman","91");	MCLN.put("turner","92");		MCLN.put("tyson","93");
        MCLN.put("walter","94");	MCLN.put("walters","95");		MCLN.put("wayne","96");
        MCLN.put("ward","97");		MCLN.put("wesley","98");		MCLN.put("williamson","99");
        MCLN.put("willis","100");	MCLN.put("winston","101");		MCLN.put("walker","102");
        MCLN.put("walkers","103");	MCLN.put("woods","104"); 		MCLN.put("woods","105");
        MCLN.put("hill", "106");     MCLN.put("dawson", "107");     MCLN.put("owen", "108");
        MCLN.put("lawrence", "109"); MCLN.put("philips", "110");    MCLN.put("phillips", "111");
        MCLN.put("scott","112");     MCLN.put("hud","113");         MCLN.put("iglesias", "114");
        MCLN.put("green", "115");    MCLN.put("fernandez", "116");  MCLN.put("garza", "117");
        MCLN.put("nelson", "118");   MCLN.put("gonzales", "119");   MCLN.put("gordon", "120");
        MCLN.put("gibson", "121");   MCLN.put("gilbert", "122");    MCLN.put("gonzalez", "123");
        MCLN.put("gonzalez", "124");
        
        
		MCFN.put("james", "1");    	MCFN.put("john", "2");			MCFN.put("robert", "3");     
		MCFN.put("michael", "4");	MCFN.put("william", "5");		MCFN.put("david", "6");
		MCFN.put("richard", "7");   MCFN.put("charles", "8");		MCFN.put("joseph", "9"); 
		MCFN.put("thomas", "10"); 	MCFN.put("christopher", "11");	MCFN.put("daniel", "12");  
		MCFN.put("paul", "13");  	MCFN.put("mark", "14");			MCFN.put("donald", "15"); 
		MCFN.put("george", "16"); 	MCFN.put("kenneth", "17");		MCFN.put("steven", "18");    
		MCFN.put("edward", "19");  	MCFN.put("brian", "20");		MCFN.put("ronald", "21");   
		MCFN.put("anthony", "22");  MCFN.put("kevin", "23");		MCFN.put("jason", "24");  
		MCFN.put("matthew", "25");  MCFN.put("gary", "26");			MCFN.put("timothy", "27"); 
		MCFN.put("jose", "28");  	MCFN.put("larry", "29");		MCFN.put("jeffrey", "30");
		MCFN.put("aaron", "31");  	MCFN.put("adam", "32");			MCFN.put("albert", "33");
		MCFN.put("alan", "34");  	MCFN.put("alfred", "35");		MCFN.put("andrew", "36");
		MCFN.put("arthur", "37");  	MCFN.put("bernard", "38");		MCFN.put("bradley", "39");
		MCFN.put("carl", "40");  	MCFN.put("clinton", "41");		MCFN.put("craig", "42");
		MCFN.put("dustin", "43");  	MCFN.put("earl", "44");			MCFN.put("frank", "45");
		MCFN.put("fred", "46");  	MCFN.put("gregory", "47");		MCFN.put("harry", "48");
		MCFN.put("jack", "49");  	MCFN.put("jerry", "50");		MCFN.put("keith", "51");
		MCFN.put("lawrence", "52"); MCFN.put("lyle", "53");			MCFN.put("marvin", "54");
		MCFN.put("nicholas", "55"); MCFN.put("patrick", "56");		MCFN.put("peter", "57");
		MCFN.put("phillip", "58");  MCFN.put("raymond", "59");		MCFN.put("roger", "60");
		MCFN.put("roy", "61");  	MCFN.put("samuel", "62");		MCFN.put("sean", "63");
		MCFN.put("stanley", "64");  MCFN.put("stewart", "65");		MCFN.put("stuart", "66");
		MCFN.put("tyler", "67");  	MCFN.put("victor", "68");		MCFN.put("vincent", "69");
		MCFN.put("wallace", "70");  MCFN.put("wayne", "71");        MCFN.put("jacob", "72");
		MCFN.put("wesley", "73");  	MCFN.put("owen", "74");         MCFN.put("eric", "75"); 
		MCFN.put("luis", "76");     MCFN.put("walter", "77");       MCFN.put("joe", "78");
		MCFN.put("juan", "79");     MCFN.put("jonathan", "80");     MCFN.put("justin", "81");
		MCFN.put("terry", "82");    MCFN.put("gerald", "81");       MCFN.put("ralph", "82");
		MCFN.put("benjamin", "83"); MCFN.put("bruce", "84");        MCFN.put("brandon", "85");
		MCFN.put("randy", "86");    MCFN.put("howard", "87");       MCFN.put("eugene", "88");
		MCFN.put("carlos", "89");   MCFN.put("russell", "90");      MCFN.put("bobby", "91");
		MCFN.put("martin", "92");   MCFN.put("ernest", "93");       MCFN.put("todd", "94");
		MCFN.put("jesse", "95");    MCFN.put("shawn", "96");        MCFN.put("philip", "97");
		MCFN.put("chris", "98");    MCFN.put("johnny", "99");       MCFN.put("antonio", "100");
		MCFN.put("leonard", "101"); MCFN.put("nathan", "102");      MCFN.put("curtis", "103");
		MCFN.put("jorge", "104");   MCFN.put("pedro", "105");       MCFN.put("maurice", "106");
		
		
		MCFN.put("mary", "1");  	MCFN.put("patricia", "2");		MCFN.put("linda", "3");  
		MCFN.put("barbara", "4");  	MCFN.put("elizabeth", "5");		MCFN.put("jennifer", "6");  
		MCFN.put("maria", "7");  	MCFN.put("susan", "8");			MCFN.put("margaret", "9");  
		MCFN.put("dorothy", "10");  MCFN.put("lisa", "11");			MCFN.put("nancy", "12");  
		MCFN.put("karen", "13");  	MCFN.put("betty", "14");		MCFN.put("helen", "15");  
		MCFN.put("sandra", "16");  	MCFN.put("donna", "17");		MCFN.put("carol", "18");  
		MCFN.put("ruth", "19");  	MCFN.put("sharon", "20");		MCFN.put("michelle", "21");  
		MCFN.put("laura", "22");  	MCFN.put("sarah", "23");		MCFN.put("kimberly", "24"); 
		MCFN.put("deborah", "25"); 	MCFN.put("jessica", "26");		MCFN.put("shirley", "27");  
		MCFN.put("cynthia", "28");  MCFN.put("angela", "29");		MCFN.put("melissa", "30");
		MCFN.put("rosa", "31");
		MCFN.put("agnes", "32");  	MCFN.put("alice", "33");		MCFN.put("anita", "34");
		MCFN.put("amanda", "35");  	MCFN.put("amy", "36");			MCFN.put("anna", "37");
		MCFN.put("audrey", "38");  	MCFN.put("beatrice", "39");		MCFN.put("berenice", "40");
		MCFN.put("beth", "41");  	MCFN.put("beverly", "42");		MCFN.put("bonnie", "43");
		MCFN.put("brenda", "44");  	MCFN.put("carolyn", "45");		MCFN.put("carla", "46");
		MCFN.put("carmen", "47");  	MCFN.put("connie", "48");		MCFN.put("catherine", "49");
		MCFN.put("cindy", "50");  	MCFN.put("clara", "51");		MCFN.put("dana", "52");
		MCFN.put("daisy", "53");  	MCFN.put("debbie", "54");		MCFN.put("debra", "55");
		MCFN.put("dolores", "56");  MCFN.put("edna", "57");			MCFN.put("edith", "58");
		MCFN.put("eleanor", "59");  MCFN.put("eliza", "60");		MCFN.put("florence", "61");
		MCFN.put("hillary", "62");  MCFN.put("grace", "63");		MCFN.put("jane", "64");
		MCFN.put("janice", "65");  	MCFN.put("julie", "66");		MCFN.put("kelly", "67");
		MCFN.put("marjorie", "68"); MCFN.put("martha", "69");		MCFN.put("pamela", "70");
		MCFN.put("paula", "71");  	MCFN.put("rachel", "72");		MCFN.put("rebecca", "73");
		MCFN.put("valerie", "74");  MCFN.put("vanessa", "75");		MCFN.put("victoria", "76");
		MCFN.put("virginia", "77"); MCFN.put("vivian", "78");		MCFN.put("wilma", "79");
		MCFN.put("rose", "80");     MCFN.put("ann", "81");          MCFN.put("stephanie", "82");
		MCFN.put("christine", "83"); MCFN.put("janet", "84");       MCFN.put("frances", "85");
		MCFN.put("joyce", "86");    MCFN.put("diane", "87");        MCFN.put("heather", "88");
		MCFN.put("teresa", "89");   MCFN.put("doris", "90");        MCFN.put("gloria", "91");
		MCFN.put("evelyn", "92");   MCFN.put("cheryl", "93");       MCFN.put("mildred", "94");
		MCFN.put("ashley", "95");   MCFN.put("janice", "96");       MCFN.put("judith", "97");
		MCFN.put("nicole", "98");   MCFN.put("judy", "99");         MCFN.put("irene", "100");
		MCFN.put("andrea", "101");  MCFN.put("sara", "102");        MCFN.put("wanda", "103");
		MCFN.put("julia", "104");   MCFN.put("emily", "105");       MCFN.put("robin", "106");
		MCFN.put("tina", "107");    MCFN.put("norma", "108");       MCFN.put("tracy", "109");
		MCFN.put("tiffany", "110"); MCFN.put("megan", "111");       MCFN.put("gail", "112");
		MCFN.put("jill", "113");    MCFN.put("erin", "114");        MCFN.put("lauren", "115");
		MCFN.put("cathy", "116");   MCFN.put("joann", "117");       MCFN.put("lynn", "118");
		MCFN.put("sally", "119");   MCFN.put("erica", "120");       MCFN.put("samantha", "121");
		
		for(String first: MCLN.keySet()){
			for(int i=2, n=first.length(); i<=n; i++){
				commonLastPrefixes.add(first.substring(0,i).toUpperCase());
			}
		}
		
		for(String last: MCFN.keySet()){
			for(int i=2, n=last.length(); i<=n; i++){
				commonFirstPrefixes.add(last.substring(0,i).toUpperCase());
			}
		}
		
	}
	
	public static boolean isMCLastName(String lastName){
		return commonLastPrefixes.contains(lastName.toUpperCase());
	}
	
	public static boolean isMCFirstName(String firstName){
		return commonFirstPrefixes.contains(firstName);
	}
	
}
