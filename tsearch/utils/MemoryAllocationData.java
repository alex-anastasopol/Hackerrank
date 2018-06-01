package ro.cst.tsearch.utils;

public class MemoryAllocationData {
	
	private long searchesStarted = 0;
	private long searchesEnded = 0;
	private long tsrsStarted = 0;
	private long tsrsEnded = 0;
	private long peakMemUsage = 0;
	private long totalAllocatedSearches = 0;
	
	public long getSearchesStarted() {
		return searchesStarted;
	}

	public long getTotalAllocatedSearches() {
		return totalAllocatedSearches;
	}
	
	public long getSearchesEnded() {
		return searchesEnded;
	}

	public long getTsrsEnded() {
		return tsrsEnded;
	}


	public long getTsrsStarted() {
		return tsrsStarted;
	}

	public long getPeakMemUsage() {
		return peakMemUsage;
	}
	
	public synchronized void increaseSearchesStarted () {
		this.searchesStarted ++ ;
	}
	
	public synchronized void increaseSearchesEnded() {
		this.searchesEnded ++;
	}
	
	public synchronized void increaseTsrsStarted() {
		this.tsrsStarted ++;
	}
	
	public synchronized void increaseTsrsEnded() {
		this.tsrsEnded ++;
	}
	
	public synchronized void increaseAllocatedSearches() {
		this.totalAllocatedSearches ++;
	}
	
	public synchronized void updatePeakMemory( long memValue ){
		if ( peakMemUsage < memValue ) {
			peakMemUsage = memValue;
		}
	}
}