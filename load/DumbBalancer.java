package load;

import java.util.Timer;
import java.util.TimerTask;

public class DumbBalancer {
	private static DumbBalancer INSTANCE = new DumbBalancer();

	public final static int MAX = 100;

	private int connInSec = 0;

	public void addConn() {
		connInSec++;
	}

	private DumbBalancer() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				connInSec = 0;
			}
		}, 0, 1000);
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