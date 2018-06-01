package ro.cst.tsearch.search.filter.newfilters.name;

/**
 * @author Cristi Stochina
 */
public abstract class BackTrackingIntQueue  {
	protected int stackLevel=0;
	protected int stackMaxLevel=0;
	protected int stack[]=null;
	protected boolean as= false;
	protected boolean ev = false;
	
	protected abstract void 	init();
	protected abstract boolean 	goToSuccesor();
	protected abstract boolean 	valid();
	protected abstract boolean 	solutie();
	
	public BackTrackingIntQueue(int size){
		stackMaxLevel = size-1;
		stack =  new int[size];
		if(size>0){
			init();
		}
	}
	
	/* (non-Javadoc)
	 * @see tests.BackTracking#getNext()
	 */
	public int[] getNext(){
		while(	stackLevel	>	-1 ){
			do{
				as = goToSuccesor();
				if(as){
					ev = valid();
				}
			}
			while(  as && ( !as || !ev )  )   ;
			if(as){
				if(solutie()){
					return stack;
				}
				else{
					stackLevel++;
					init();
				}
			}
			else{
				stackLevel--;
			}
		}
		return null;
	}
	
	
}
