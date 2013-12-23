
/**
 * Thread for simultant time stop.
 * 
 * @author Long Mathias Yan & Nhu-Huy Le
 *
 */
public class TimeThread implements Runnable {

	private boolean stop = false;
	private int duration = 0;
	
	public TimeThread (int durance) {
		
		this.duration = durance;
	}
	
	@Override
	public void run () {
		
		try {
			Thread.sleep (duration);
			stop = true;
		} catch (InterruptedException e) {
			e.printStackTrace ();
		}
	}
	
	public boolean getStop () {
		
		return stop;
	}
}
