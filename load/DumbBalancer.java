package load;

public class DumbBalancer {
	private static DumbBalancer INSTANCE = new DumbBalancer();

	public final static int MAX = 100;

	private int connInSec;

	public void addConn() {
		connInSec++;
	}

	private DumbBalancer() {
		connInSec = 0;
	}

	public boolean checkLoad() {
		boolean flag = connInSec < MAX;
		if (!flag) {
			System.err.println("Overload!");
		}
		return flag;
	}

	public static DumbBalancer getInstance() {
		return INSTANCE;
	}
}
