package warm.up.jump.on.clouds;

import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class Solution {

	// Complete the jumpingOnClouds function below.
	static int jumpingOnClouds(int[] c) {
		int jumpsCount = 0;
		int i = 0;
		while (i < c.length - 1) {
			if (i + 2 < c.length && c[i + 2] == 0) {
				jumpsCount++;
				i += 2;
			} else if (i + 1 < c.length && c[i + 1] == 0) {
				jumpsCount++;
				i += 1;
			} else {
				
			}
		}
		return jumpsCount;
	}

	private static final Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) throws IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));

		int n = scanner.nextInt();
		scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

		int[] c = new int[n];

		String[] cItems = scanner.nextLine().split(" ");
		scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

		for (int i = 0; i < n; i++) {
			int cItem = Integer.parseInt(cItems[i]);
			c[i] = cItem;
		}

		int result = jumpingOnClouds(c);

		bufferedWriter.write(String.valueOf(result));
		bufferedWriter.newLine();

		bufferedWriter.close();

		scanner.close();
	}
}
