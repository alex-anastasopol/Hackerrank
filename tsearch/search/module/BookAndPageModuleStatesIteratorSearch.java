package ro.cst.tsearch.search.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Category;

import ro.cst.tsearch.Search;
import ro.cst.tsearch.bean.SearchAttributes;
import ro.cst.tsearch.propertyInformation.Instrument;
import ro.cst.tsearch.search.FunctionStatesIterator;
import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.StatesIterator;
import ro.cst.tsearch.search.filter.FilterResponse;
import ro.cst.tsearch.search.filter.newfilters.legal.GenericInstrumentFilter;
import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.info.TSServerInfoFunction;
import ro.cst.tsearch.servers.info.TSServerInfoModule;
import ro.cst.tsearch.titledocument.abstracts.FormatSa;
import ro.cst.tsearch.utils.InstanceManager;
import ro.cst.tsearch.utils.Log;
import ro.cst.tsearch.utils.StringUtils;

/**
 * BookAndPageModuleStatesIteratorSearch
 * 
 */
public class BookAndPageModuleStatesIteratorSearch extends ModuleStatesIterator {

	private static final Category logger = Category.getInstance(BookAndPageModuleStatesIteratorSearch.class.getName());

	private static final Category loggerDetails = Category.getInstance(Log.DETAILS_PREFIX
			+ BookAndPageModuleStatesIteratorSearch.class.getName());

	/**
	 * Instrument number list.
	 */
	private List instrList = new ArrayList();

	/**
	 * Default constructor.
	 */
	public BookAndPageModuleStatesIteratorSearch(long searchId) {
		super(searchId);
	}

	protected void initInitialState(TSServerInfoModule initial) {
		super.initInitialState(initial);
		SearchAttributes sa = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext().getSa();
		instrList = extractBookPageList(sa.getAtribute(initial.getSaObjKey()));
		if(!searchIfPresent){
			instrList = ModuleStatesIterator.removeAlreadyRetrieved(instrList, searchId);
		}
	}

	protected void setupStrategy() {
		StatesIterator si;
		si = new DefaultStatesIterator(instrList);
		setStrategy(si);
	}

	public Object current() {
		Instrument instr = ((Instrument) getStrategy().current());
		TSServerInfoModule crtState = new TSServerInfoModule(initialState);

		Search ss = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		SearchAttributes sa = ss.getSa();
		String cntyName = FormatSa.getPCountyName(sa);
		List<FilterResponse> allFilters = crtState.getFilterList();
		GenericInstrumentFilter gif = null;
		HashMap<String, String> filterCriteria = null;
		if(allFilters != null) {
			for (FilterResponse filterResponse : allFilters) {
				if (filterResponse instanceof GenericInstrumentFilter) {
					gif = (GenericInstrumentFilter) filterResponse;
					filterCriteria = new HashMap<String, String>();
					gif.clearFilters();
				}
			}
		}
		for (int i = 0; i < crtState.getFunctionCount(); i++) {
			TSServerInfoFunction fct = crtState.getFunction(i);
			if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_BOOK_SEARCH) {
				String bookNo = instr.getBookNo().matches("0+$") ? "" : instr.getBookNo();
				fct.setParamValue(bookNo);
				if(filterCriteria != null) {
					filterCriteria.put("Book", bookNo);
				}
			}
			if (fct.getIteratorType() == FunctionStatesIterator.ITERATOR_TYPE_PAGE_SEARCH) {
				String pageNo = instr.getPageNo().matches("0+$") ? "" : instr.getPageNo();
				fct.setParamValue(pageNo);
				if(filterCriteria != null) {
					filterCriteria.put("Page", pageNo);
				}
			}

			if (cntyName.equals("Knox") && fct.getParamName().equals("BookType")) {
				if (!instr.getBookPageType().equals(""))
					fct.setParamValue(instr.getBookPageType());
			}

		}
		if(gif != null) {
			gif.addDocumentCriteria(filterCriteria);
		}
		return crtState;
	}

	public  List extractBookPageList(String originalInstrNo) {
		
		List instrList = new ArrayList();

		if (StringUtils.isStringBlank(originalInstrNo)) {
			return instrList;
		}

		String[] instrs = originalInstrNo.split(",");

		if (instrs == null) {
			return instrList;
		}
		
		Search ss = InstanceManager.getManager().getCurrentInstance(searchId).getCrtSearchContext();
		SearchAttributes sa = ss.getSa();
		String cntyName = FormatSa.getPCountyName(sa);
		
		for (int i = 0; i < instrs.length; i++) {
			if (!StringUtils.isStringBlank(instrs[i])) {
				String[] bp = instrs[i].split("[-,_]");
				if (bp != null && bp.length >= 2) {
					Instrument crtInst = new Instrument();
					crtInst.setBookNo(bp[0].trim());
					crtInst.setPageNo(bp[1].trim());
					instrList.add(crtInst);

					if ((cntyName.equals("Williamson") || cntyName.equals("Wilson")) && bp[0].trim().charAt(0) != 'P') {
						Instrument crtInstP = new Instrument();
						crtInstP.setBookNo("P" + bp[0].trim());
						crtInstP.setPageNo(bp[1].trim());
						instrList.add(crtInstP);
					}
				}
			}
		}
		
		if (cntyName.equals("Knox")) {
			List insList = (List) (sa.getObjectAtribute(SearchAttributes.INSTR_LIST));
			for (int i = 0; i < insList.size(); i++) {
				Instrument instr = (Instrument) insList.get(i);
				if(!instrList.contains(instr))
					instrList.add(instr);
			}
		}
	
		return instrList;
	}
}