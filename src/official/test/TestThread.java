package official.test;

class SampleDemo implements Runnable{

	private SampleDemo t;
	private String threadName;

	public SampleDemo(String threadName) {
		this.threadName = threadName;
	}

	public void run() {
		int i=0;
		while (i<1000) {
			i++;
			System.out.println(threadName);
		}
	}

	public void start() {
		if (t == null) {
			t = new SampleDemo(threadName);
			t.start();
		}
	}
}

public class TestThread {
	public static void main(String[] args) {
		SampleDemo A = new SampleDemo("A");
		//SampleDemo B = new SampleDemo("B");
		//B.start();
		A.start();
	}

}