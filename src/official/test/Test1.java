package official.test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class Test1 {

	public static void main(String[] args) {
		// int i=010;
		// int j=07;
		// System.out.println(i+" "+j);
		try {
			int exceptionTest = exceptionTest();
			System.out.println(exceptionTest);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static int exceptionTest() throws Exception {
		try {
			throw new IOException("..");
		} catch (RuntimeException re) {
			throw new RuntimeException();
		} finally {
			return -1;
		}
	}
}
