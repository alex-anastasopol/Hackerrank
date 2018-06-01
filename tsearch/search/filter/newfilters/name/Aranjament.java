package ro.cst.tsearch.search.filter.newfilters.name;

/**
 * @author Cristi Stochina
 */
public class Aranjament extends BackTrackingIntQueue{

	private int n=0;
	
	public Aranjament(int n){
		super(n);
		this.n = n-1;
	}
	
	public Aranjament(int n, int p){
		super(p);
		this.n = n-1;
	}
	
	@Override
	protected void init() {
		stack[stackLevel] = -1;
	}

	@Override
	protected boolean solutie() {
		return ( stackLevel	==	stackMaxLevel );
	}

	@Override
	protected boolean goToSuccesor() {
		if( stack[stackLevel]< n ){
			stack[stackLevel]++;
			return true;
		}
		return false;
	}

	@Override
	protected boolean valid() {
		for(int i=0;	i<stackLevel	;i++){
			if(	stack[stackLevel]==stack[i]	){
				return false;
			}
		}
		return true;
	}
	
	public static void main(String[] args) {
		Aranjament aranj = new Aranjament(5,3);
		int a[]=null;
		while(  (a=aranj.getNext() )!= null ){
			for(int i=0;i<a.length;i++){
				System.out.print(a[i]+" ");
			}
			System.out.println("");
		}
	}

}
