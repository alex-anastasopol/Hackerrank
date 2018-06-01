package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;

import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * ShelbyEPParcelIDModuleStatesIterator
 */
public class ShelbyEPParcelIDModuleStatesIterator 
	extends ModuleStatesIterator {

		/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
		@SuppressWarnings("unused")
		private static final Category logger = Category.getInstance(ShelbyEPParcelIDModuleStatesIterator.class.getName());
		@SuppressWarnings("unused")
		private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX + ShelbyEPParcelIDModuleStatesIterator.class.getName());
	
		private static Hashtable<String, String> replacements = new Hashtable<String, String>();
		
		static {
			replacements.put("000","0");
			replacements.put("(00[a-zA-Z1-9])0(\\w+)$","$1$2");
			replacements.put("000000","0000");
			replacements.put("(\\w+)-(\\w+)","$1$2");
		}
	
		private static HashMap<Pattern, String> equivs = new HashMap<Pattern, String>();
		static {
			equivs.put(Pattern.compile("(\\d{6})00(\\d{5})0"), "$1 $2");
			equivs.put(Pattern.compile("(\\d{6})0([A-Za-z]\\d{5})0"), "$1 $2");
			equivs.put(Pattern.compile("([A-Za-z]\\d{4}[A-Za-z])0([A-Za-z]\\d{5})0"), "$1 $2");
			equivs.put(Pattern.compile("([A-Za-z]\\d{4})000(\\d{5})0"), "$1 $2");
			equivs.put(Pattern.compile("([A-Za-z]\\d{4})00([A-Za-z]\\d{5})0"), "$1 $2");
			equivs.put(Pattern.compile("([A-Za-z]\\d{4})00([A-Za-z]\\d{5})([A-Za-z])"), "$1 $2$3");
			equivs.put(Pattern.compile("(\\d{6})00(\\d{5}[A-Za-z])"), "$1 $2");
			equivs.put(Pattern.compile("([A-Za-z]\\d{4})000(\\d{5}[A-Za-z])"), "$1 $2");
		} 
		/**
		 * 
		 * Parcel number list.
		 */	
		private List<String> parcelIDList = new ArrayList<String>();
	
		/**
		 * Default constructor.
		 */
		private long searchId=-1;
		public ShelbyEPParcelIDModuleStatesIterator(long searchId){
			
			super(searchId);
			this.searchId = searchId;
		}

		/**
		 * @return the parcelIDList
		 */
		public List<String> getParcelIDList() {
			if(parcelIDList == null)
				parcelIDList = new ArrayList<String>();
			return parcelIDList;
		}

		/**
		 * @param parcelIDList the parcelIDList to set
		 */
		public void setParcelIDList(List<String> parcelIDList) {
			this.parcelIDList = parcelIDList;
		}

		protected void initInitialState(TSServerInfoModule initial){
			super.initInitialState(initial);
			SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
			for (String pid : buildParcelIDList((String) sa.getObjectAtribute(SearchAttributes.LD_PARCELNO))) {
				getParcelIDList().add(pid);
			}
		}

		protected void setupStrategy() {
			StatesIterator si ;
			si = new DefaultStatesIterator(getParcelIDList());
			setStrategy(si);
		}
	
		public Object current(){
			String instr = ((String) getStrategy().current());
			TSServerInfoModule crtState = new TSServerInfoModule(initialState);

			for (int i =0; i< crtState.getFunctionCount(); i++){
				TSServerInfoFunction fct = crtState.getFunction(i);
				if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PARCELID_FAKE){
					fct.setParamValue(instr);
				}
			}
			return  crtState ;
		}
			
		private HashSet<String> buildParcelIDList(String parcelID) {
			HashSet<String> hashSet = new HashSet<String>();
			if(!StringUtils.isStringBlank(parcelID)) {
				if (parcelID.length() == 14){
					//PID boostrapped from TR, try and convert it to EP and AO
					Iterator<Pattern> i = equivs.keySet().iterator();
					while (i.hasNext()){
						Pattern p = i.next();
						Matcher m = p.matcher(parcelID);
						if (m.matches()){
							hashSet.add(m.replaceAll(equivs.get(p)));
						}
					}
				}
				hashSet.add(parcelID);
				if(parcelID.matches("0000") && !parcelID.matches("00000"))
					hashSet.add(parcelID.replaceAll("0000", "000000"));
				Enumeration<String> e = replacements.keys();
				String PID = parcelID;
				PID = PID.substring(0, PID.length()/2) + "0" + PID.substring(PID.length()/2, PID.length());
	            PID += "0";             
	            hashSet.add(PID);
	            if(PID.matches("0000") && !PID.matches("00000"))
					hashSet.add(PID.replaceAll("0000", "000000"));
				while(e.hasMoreElements()) {
					String pat = (String)e.nextElement();
					String repl= (String)replacements.get(pat);
					String newParcelID = new String(parcelID);
					newParcelID = newParcelID.replaceAll("(\\w+)0$","$1");
					newParcelID = newParcelID.replaceAll(pat,repl);
					hashSet.add(newParcelID); 
					if(newParcelID.matches("0000") && !newParcelID.matches("00000"))
						hashSet.add(newParcelID.replaceAll("0000", "000000"));
				}
			}
			return hashSet;
		}
}
