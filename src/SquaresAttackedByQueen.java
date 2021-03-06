import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SquaresAttackedByQueen {
	// static Logger logger = Logger.getLogger("Logger");
	static String obstaclesStr = "";

	// Complete the queensAttack function below.
	static int queensAttack(int n, int k, int r_q, int c_q, int[][] obstacles) {
		for (int i = 0; i < obstacles.length; i++) {
			obstaclesStr += obstacles[i][0] + "_" + obstacles[i][1] + ".";
		}
		// logger.log(Level.SEVERE, "entered queensAttack");
		return countAttackedOnRow(n, r_q, c_q, obstacles) + countAttackedOnCol(n, r_q, c_q, obstacles)
				+ countAttackedOnDiagNWtoSE(n, r_q, c_q, obstacles) + countAttackedOnDiagNEtoSW(n, r_q, c_q, obstacles);
	}

	static boolean squareIsNotObstacle(int r, int c, int[][] obstacles) {
		// for (int i = 0; i < obstacles.length; i++) {
		// if (obstacles[i][0] == r && obstacles[i][1] == c) {
		// return false;
		// }
		// }
		if (obstaclesStr.indexOf(r + "_" + c) >= 0) {
			return false;
		}
		return true;
	}

	static int countAttackedOnCol(int n, int r_q, int c_q, int[][] obstacles) {
		int count = 0;
		for (int i = r_q - 1; i > 0 && squareIsNotObstacle(i, c_q, obstacles); i--) {
			count++;
		}

		for (int i = r_q + 1; i <= n && squareIsNotObstacle(i, c_q, obstacles); i++) {
			count++;
		}
		// logger.log(Level.SEVERE, "countAttackedOnRow -> " + count);
		return count;
	}

	static int countAttackedOnRow(int n, int r_q, int c_q, int[][] obstacles) {
		int count = 0;
		for (int i = c_q - 1; i > 0 && squareIsNotObstacle(r_q, i, obstacles); i--) {
			count++;
		}

		for (int i = c_q + 1; i <= n && squareIsNotObstacle(r_q, i, obstacles); i++) {
			count++;
		}
		// logger.log(Level.SEVERE, "countAttackedOnCol -> " + count);
		return count;
	}

	static int countAttackedOnDiagNWtoSE(int n, int r_q, int c_q, int[][] obstacles) {
		int count = 0;
		for (int i = r_q - 1, j = c_q - 1; i > 0 && j > 0 && squareIsNotObstacle(i, j, obstacles); i--, j--) {
			count++;
		}

		for (int i = r_q + 1, j = c_q + 1; i <= n && j <= n && squareIsNotObstacle(i, j, obstacles); i++, j++) {
			count++;
		}
		// logger.log(Level.SEVERE, "countAttackedOnDiagNWtoSE -> " + count);
		return count;
	}

	static int countAttackedOnDiagNEtoSW(int n, int r_q, int c_q, int[][] obstacles) {
		int count = 0;
		for (int i = r_q - 1, j = c_q + 1; i > 0 && j <= n && squareIsNotObstacle(i, j, obstacles); i--, j++) {
			count++;
		}

		for (int i = r_q + 1, j = c_q - 1; i <= n && j > 0 && squareIsNotObstacle(i, j, obstacles); i++, j--) {
			count++;
		}
		// logger.log(Level.SEVERE, "countAttackedOnDiagNEtoSW -> " + count);
		return count;
	}

	private static final Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) throws IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));

		String[] nk = scanner.nextLine().split(" ");

		int n = Integer.parseInt(nk[0]);

		int k = Integer.parseInt(nk[1]);

		String[] r_qC_q = scanner.nextLine().split(" ");

		int r_q = Integer.parseInt(r_qC_q[0]);

		int c_q = Integer.parseInt(r_qC_q[1]);

		int[][] obstacles = new int[k][2];

		for (int i = 0; i < k; i++) {
			String[] obstaclesRowItems = scanner.nextLine().split(" ");
			scanner.skip("(\r\n|[\n\r\u2028\u2029\u0085])?");

			for (int j = 0; j < 2; j++) {
				int obstaclesItem = Integer.parseInt(obstaclesRowItems[j]);
				obstacles[i][j] = obstaclesItem;
			}
		}

		int result = queensAttack(n, k, r_q, c_q, obstacles);

		bufferedWriter.write(String.valueOf(result));
		bufferedWriter.newLine();

		bufferedWriter.close();

		scanner.close();
	}
}
