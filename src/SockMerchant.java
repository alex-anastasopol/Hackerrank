import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SockMerchant {

	// Complete the sockMerchant function below.
	static int sockMerchant(int n, int[] ar) {
		int noOfPairs = 0;
		Map<Integer, Integer> valueToOccurenceCount = new HashMap<Integer, Integer>();
		for (int i = 0; i < n; i++) {
			Integer occurrences = valueToOccurenceCount.get(ar[i]);
			if (occurrences == null) {
				occurrences = 1;
			} else {
				occurrences++;
			}

			valueToOccurenceCount.put(ar[i], occurrences);
		}

		for (Integer value : valueToOccurenceCount.keySet()) {
			Integer noOfOccurrences = valueToOccurenceCount.get(value);
			noOfPairs += noOfOccurrences / 2;
		}

		return noOfPairs;
	}

	private static final Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) throws IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));

		int n = Integer.parseInt(scanner.nextLine());
		//scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

		int[] ar = new int[n];

		String[] arItems = scanner.nextLine().split(" ");
		//scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

		for (int i = 0; i < n; i++) {
			int arItem = Integer.parseInt(arItems[i]);
			ar[i] = arItem;
		}

		int result = sockMerchant(n, ar);

		bufferedWriter.write(String.valueOf(result));
		bufferedWriter.newLine();

		bufferedWriter.close();

		scanner.close();
	}
}
