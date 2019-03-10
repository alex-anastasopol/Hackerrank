package official.test;
public class Question6SuperStack {

	private static Integer[] stack;
	private static int stackTopIdx = -1;
	private static final int MAX_NO_OF_OPERATIONS = 20000;
	private static final int MAX_STACK_SIZE = 10000;

	static int push(int k) {
		stack[++stackTopIdx] = k;
		return k;
	}

	static Integer pop() {
		stack[stackTopIdx] = null;
		stackTopIdx--;
		if (stackTopIdx == -1) {
			return null;
		}
		return stack[stackTopIdx];
	}

	static Integer inc(int e, int k) {
		for (int i = 0; i < e && i <= stackTopIdx; i++) {
			stack[i] = stack[i] + k;
		}
		if (stackTopIdx == -1) {
			return null;
		}

		return stack[stackTopIdx];
	}

	static void superStack(String[] operations) {
		if (operations.length < MAX_NO_OF_OPERATIONS) {
			stack = new Integer[MAX_STACK_SIZE];

			for (String operation : operations) {
				if (operation.startsWith("push")) {
					String kStr = operation.substring("push".length()).trim();
					int k = Integer.parseInt(kStr);
					System.out.println(push(k));
				} else if (operation.equals("pop")) {
					Integer popResult = pop();
					if (popResult == null) {
						System.out.println("EMPTY");
					} else {
						System.out.println(popResult);
					}
				} else if (operation.startsWith("inc")) {
					String[] params = operation.substring("inc".length() + 1).split("\\s+", -1);
					String eStr = params[0];
					String kStr = params[1];
					int e = Integer.parseInt(eStr);
					int k = Integer.parseInt(kStr);

					Integer incResult = inc(e, k);
					if (incResult == null) {
						System.out.println("EMPTY");
					} else {
						System.out.println(incResult);
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		String[] operations = { "push 4", "pop", "push 3", "push 5", "inc 3 1", "pop", "push 1", "inc 2 2", "push 4",
				"pop", "pop" };
		Question6SuperStack a = new Question6SuperStack();
		a.superStack(operations);
	}
}
