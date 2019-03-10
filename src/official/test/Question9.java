package official.test;

public class Question9 {

	static int noOfConcurrentTolls = 1;
	static int SECONDS_COUNT_IN_30_MINS = 30 * 60;

	public static void main(String[] args) {
		for (int i = 0; i < SECONDS_COUNT_IN_30_MINS; i++) {
			if (i % 2 == 0 && i % 4 == 0 && i % 6 == 0 && i % 8 == 0 && i % 10 == 0 && i % 12 == 0) {
				noOfConcurrentTolls++;
				System.out
						.println("Concurrent toll at second " + i + ". Sub-total: " + noOfConcurrentTolls);
			}
		}

		System.out.println(
				"Total number of cuncurrent tolls on the 6 bells in 30 minutes(at intervals of 2, 4, 6, 8, 10, 12 seconds): " + noOfConcurrentTolls);
	}
}
