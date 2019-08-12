package warm.up.repeated.string;

import java.io.*;
import java.math.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class Solution {

	// Complete the repeatedString function below.
   static long repeatedString(String s, long n) {
       int idxStringLength = 0;
       long numberOfAsInS = 0;
       long totalNumberOfAs = 0;
       while (idxStringLength < s.length()) {
           if (s.charAt(idxStringLength++) == 'a') {
               numberOfAsInS++;
           }
       }

       totalNumberOfAs = numberOfAsInS * (n / s.length());

       long remainder = n % s.length();
       int idx = -1;
       if (remainder > 0) {
          // System.out.println("remainder: "+remainder);
           while (++idx < remainder) {
              // System.out.println("idx="+idx+"; s.charAt(idx) == 'a'");
               if (s.charAt(idx) == 'a') {
                   totalNumberOfAs++;
               }
           }
       }

       return totalNumberOfAs;
   }

	private static final Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) throws IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));

		String s = scanner.nextLine();

		long n = scanner.nextLong();
		scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

		long result = repeatedString(s, n);

		bufferedWriter.write(String.valueOf(result));
		bufferedWriter.newLine();

		bufferedWriter.close();

		scanner.close();
	}
}