package regex.application.detect.emails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Solution {
	static Scanner sc = new Scanner(System.in);
	static Pattern emailPattern = Pattern.compile("(?i)\\b[_a-z][_a-z0-9]*@[_a-z][_a-z0-9]*\\.[_a-z][_a-z0-9]*\\b");

	private static Comparator<String> ALPHABETICAL_ORDER = new Comparator<String>() {
		public int compare(String str1, String str2) {
			int res = String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
			if (res == 0) {
				res = str1.compareTo(str2);
			}
			return res;
		}
	};

	private static List<String> sortStringListAlphabetically(Set<String> set) {
		List<String> list = new ArrayList<String>();
		for (String string : set) {
			list.add(string);
		}

		Collections.sort(list, ALPHABETICAL_ORDER);

		return list;
	}

	private static String formatMatchingEmails(List<String> emailsSet) {
		String formattedEmails = "";
		for (String email : emailsSet) {
			formattedEmails += ";" + email;
		}

		return formattedEmails.length() == 0 ? "" : formattedEmails.substring(1);
	}

	private static Set<String> getMatchingEmails(String[] rows) {
		Set<String> emailsSet = new HashSet<String>();
		for (int i = 0; i < rows.length; i++) {
			Matcher emailMatcher = emailPattern.matcher(rows[i]);
			while (emailMatcher.find()) {
				emailsSet.add(emailMatcher.group());
			}
		}
		return emailsSet;
	}

	private static String[] readRows(int rowsCount) {
		String[] rows = new String[rowsCount];
		try {
			for (int i = 0; i < rowsCount; i++) {
				rows[i] = sc.nextLine();
			}
			return rows;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static int readRowCount() {
		try {
			return Integer.valueOf(sc.nextLine());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static void main(String[] args) {
		System.out.println(
				formatMatchingEmails(sortStringListAlphabetically(getMatchingEmails(readRows(readRowCount())))));
	}
}