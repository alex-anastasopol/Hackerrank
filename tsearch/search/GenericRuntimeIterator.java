package ro.cst.tsearch.search;

import java.util.List;

import ro.cst.tsearch.search.strategy.DefaultStatesIterator;
import ro.cst.tsearch.servers.bean.DataSite;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public abstract class GenericRuntimeIterator<T> extends ModuleStatesIterator {

	private static final long serialVersionUID = -9087527443416937061L;
	
	protected List<T> list = null;
	
	public GenericRuntimeIterator(long searchId) {
		super(searchId); 
		setInitAgain(true);
	}
	
	public GenericRuntimeIterator(long searchId, DataSite dataSite) {
		super(searchId, dataSite); 
		setInitAgain(true);
	}

	protected void setupStrategy() {
		StatesIterator si ;
		si = new DefaultStatesIterator(list);
		setStrategy(si);
	}
	
	@SuppressWarnings("unchecked")
	public Object current(){
		TSServerInfoModule crtState = new TSServerInfoModule(initialState);
		loadDerrivation(crtState, (T)getStrategy().current());
		return crtState;
	}
	
	protected void initInitialState(TSServerInfoModule initial){
		super.initInitialState(initial);
		list = createDerrivations();
		list = cleanDerivationsList(list, initial);
	}
	
	protected List<T> cleanDerivationsList(List<T> list, TSServerInfoModule initial) {
		return list;
	}
	
	protected abstract List<T> createDerrivations();
	protected abstract void loadDerrivation(TSServerInfoModule module, T state); 
	
	public int size() {
		if(list == null) {
			return 0;
		}
		return list.size();
	}

	public List<T> getList() {
		return list;
	}
	
}
