package ro.cst.tsearch.utils;

import java.util.Random;

public class RandomGenerator {
	private Random random =  null;
	
	private RandomGenerator(){
		random = new Random();
	}
	
	private static class SingletonHolder {
		private static RandomGenerator instance = new RandomGenerator();
	}
	
	public static RandomGenerator getInstance(){
		return SingletonHolder.instance;
	}
	
	public int getInt(){
		return random.nextInt();
	}
	
	public long getLong(){
		return random.nextLong();
	}

}
