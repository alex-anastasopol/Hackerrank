package ro.cst.tsearch.search.iterator;


import ro.cst.tsearch.search.ModuleStatesIterator;
import ro.cst.tsearch.search.module.ConfigurableNameIterator;
import ro.cst.tsearch.search.module.ConfigurableSkipModuleNameIterator;
import ro.cst.tsearch.search.module.ConfigurableSkipModuleNameIteratorIfNoLegalFound;
import ro.cst.tsearch.search.module.InstrumentNoPrefixDateLetterIterator;
import ro.cst.tsearch.search.module.MultipleYearIterator;
import ro.cst.tsearch.search.module.SubdivisionIterator;
import ro.cst.tsearch.servers.info.TSServerInfoModule;

public class ModuleStatesIteratorFactory {
	
	public static ModuleStatesIterator getConfigurableNameIterator (TSServerInfoModule module, long searchId, String derivPatern[], int lastNameLength){
		ConfigurableNameIterator msi = new ConfigurableNameIterator( searchId , derivPatern, lastNameLength);
		msi.init(module);
		return msi;
	}
	
	public static ModuleStatesIterator getConfigurableNameIterator ( boolean useNickNames, TSServerInfoModule module, long searchId, String derivPatern[]){
		ConfigurableNameIterator msi = new ConfigurableNameIterator( searchId , useNickNames, derivPatern);
		msi.init(module);
		return msi;
	}

	public static ModuleStatesIterator getConfigurableNameIterator (TSServerInfoModule module, long searchId, String derivPatern[]){
		ConfigurableNameIterator msi = new ConfigurableNameIterator( searchId , derivPatern);
		msi.init(module);
		return msi;
	}
	
	public static ModuleStatesIterator getConfigurableNameIterator (TSServerInfoModule module, boolean doNotInit, long searchId, String derivPatern[]){
		ConfigurableNameIterator msi = new ConfigurableNameIterator( searchId , derivPatern);
		return msi;
	}
	
	public static ModuleStatesIterator getConfigurableSkipModuleNameIterator (long searchId, String derivPatern[], String ... skipModuleIfSourceTypesAvailable ){
		ConfigurableSkipModuleNameIterator msi = new ConfigurableSkipModuleNameIterator( searchId , derivPatern, skipModuleIfSourceTypesAvailable);
		return msi;
	}
	
	public static ModuleStatesIterator getConfigurableSkipModuleNameIteratorifNoLegalFound (long searchId, String derivPatern[], String ... skipModuleIfSourceTypesAvailable ){
		ConfigurableSkipModuleNameIteratorIfNoLegalFound msi = new ConfigurableSkipModuleNameIteratorIfNoLegalFound( searchId , derivPatern, skipModuleIfSourceTypesAvailable);
		return msi;
	}
	
	public static ModuleStatesIterator getConfigurableNameIterator (TSServerInfoModule module, long searchId, String derivPatern[],boolean treatAsCorporate){
		ConfigurableNameIterator msi = new ConfigurableNameIterator( searchId , derivPatern,treatAsCorporate);
		msi.init(module);
		return msi;
	}
	public static ModuleStatesIterator getConfigurableNameIterator (TSServerInfoModule module, long searchId, boolean skipInitial, String derivPatern[]){
		ConfigurableNameIterator msi = new ConfigurableNameIterator( searchId , derivPatern);
		msi.setSkipInitial( skipInitial );
		msi.init(module);
		return msi;
	}
	
	public static ModuleStatesIterator getSubdivisionIterator( TSServerInfoModule module, long searchId){
		SubdivisionIterator si = new SubdivisionIterator(searchId);
		si.init(module);
		return si;
	}
	
	public static ModuleStatesIterator getInstrumentIteratorFromNDBToROForILKane(TSServerInfoModule module, long searchId){
		InstrumentNoPrefixDateLetterIterator si = new InstrumentNoPrefixDateLetterIterator(searchId, "K", true, 6);
		si.setSearchIfPresent(false);
		si.setInitAgain(true);
		si.init(module);
		return si;
	}
	
	public static ModuleStatesIterator getConfigurableNameIterator (int numberOfYearsAllowed, TSServerInfoModule module, long searchId, String derivPatern[]){
		ConfigurableNameIterator msi = new ConfigurableNameIterator( searchId , derivPatern);
		msi.setNumberOfYearsAllowed(numberOfYearsAllowed);
		msi.init(module);
		return msi;
	}
	
	public static ModuleStatesIterator getMultipleYearIterator (TSServerInfoModule module, long searchId, int maxYearsNumberAllowed, int currentTaxYear){
		MultipleYearIterator myi = new MultipleYearIterator(searchId, maxYearsNumberAllowed, currentTaxYear);
		myi.init(module);
		return myi;
	}
	
}
