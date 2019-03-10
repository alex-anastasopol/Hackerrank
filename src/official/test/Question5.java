package official.test;

public class Question5 {

	private int x = 2;
	protected int y = 3;
	private int z = 4;
	protected int t = 5;

	public static void main(String[] args) {
		int x = 6;
		int y = 7;
		int z = 8;
		int t = 9;
		new Question5().new Secret().go();
	}

	class Secret {
		void go() {
			System.out.println(x + " " + y + " " + z + " " + t);
		}
	}
}
