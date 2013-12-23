import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * @author Nhu-Huy Le & Mathias Long Yan
 * 
 * Sends packets to a target with measuring options.
 * Several settings possible.
 *
 */
public class MeasureClient {
	
	private double startTime = 0;
	private byte[] sendData = new byte[1400];
	private int sentSum = 0;
	private StringBuffer sb = new StringBuffer ();
	private String hostname = "localhost";
	private int counter = 0;
	private int timeOutPacket = 10;
	int port = 4711;
	int duration = 30000;
//	in milliseconds
	private int delay = 10;

	/**
	 * Reads a file and sends it to a target.
	 * 
	 * @param args0						UDP or TCP.
	 * @param args1						Hostname: Target for packets to send.
	 * @param args2						Packet-Size: In Byte.
	 * @param args3						N Packet-Group: Number of packets sent without delay.
	 * @param args4						k Delay: Delay between packet groups.
	 * @param args5						Port: Socket sending port.
	 * @param args6						Duration: Sending time.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main (String[] args) throws IOException, InterruptedException {
		
		MeasureClient mc = new MeasureClient ();
//		in milliseconds
		String protocol = args[0];
		mc.hostname = args[1];
		mc.sendData = new byte[Integer.parseInt (args[2])];
		mc.timeOutPacket = Integer.parseInt (args[3]);
		mc.delay = Integer.parseInt (args[4]);
		mc.port = Integer.parseInt (args[5]);
		mc.duration = Integer.parseInt (args[6]);
		
		
		try (
			FileReader fr = new FileReader ("packet.txt");
			BufferedReader br = new BufferedReader (fr);
			) {		
			TimeThread time = new TimeThread (mc.duration);
			new Thread (time).start ();			
			if (protocol.equals ("UDP")) mc.loopingSendUDP (time, br, mc.port, mc.delay);
			else if ( protocol.equals ("TCP")) mc.loopingSendTCP (time, br, mc.port, mc.delay);	
			else System.out.println ("Protocol unknown: UDP or TCP ?");			
			mc.consoleOut ();
		}
		catch (FileNotFoundException f) {
			System.out.println ("No Packet found. Create packet.txt in directory.");
		}
	}

	/**
	 * Sends over UDP.
	 * 
	 * @param time							Thread: Duration time thread.
	 * @param bufferedReader				Readerstream of the file to send.		
	 * @param port							Sending port.
	 * @param delay							Delay between packet groups.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void loopingSendUDP (TimeThread time, BufferedReader bufferedReader, int port, int delay) throws InterruptedException, IOException {
		
		DatagramSocket clientSocket;
		InetAddress IPAddress;
		DatagramPacket sendPacket;
		
		System.out.print ("sending UDP ...");
		while (!time.getStop ()) {
			if (counter == timeOutPacket) {
				Thread.sleep (delay);
				counter = 0;
			}			
		    clientSocket = new DatagramSocket ();
		    IPAddress = InetAddress.getByName (hostname);	
		    for (String line = bufferedReader.readLine (); line != null; line = bufferedReader.readLine ()) {
				sb.append (line + "\n");
			}
		    sendData = sb.toString ().getBytes ();
		    sendPacket = new DatagramPacket (sendData, sendData.length, IPAddress, port);
		    clientSocket.send (sendPacket);
		    if (startTime == 0) startTime = System.currentTimeMillis ();
		    clientSocket.close ();
		    sentSum++;
		    counter++;
		}
		System.out.println ("stopped.");
	}
	
	/**
	 * Sends over TCP.
	 * 
	 * @param time							Thread: Duration time thread.
	 * @param bufferedReader				Readerstream of the file to send.		
	 * @param port							Sending port.
	 * @param delay							Delay between packet groups.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void loopingSendTCP (TimeThread time, BufferedReader bufferedReader, int port, int delay) throws InterruptedException, IOException {
		
		Socket clientSocket;
		OutputStream out;
		PrintWriter pw;
		
		System.out.print ("sending TCP ...");
		while (!time.getStop ()) {
			if (counter == timeOutPacket) {
				Thread.sleep (delay);
				counter = 0;
			}
			clientSocket = new Socket (hostname, port);
			out = clientSocket.getOutputStream ();
			pw = new PrintWriter (new OutputStreamWriter (out));		
			for (String line = bufferedReader.readLine (); line != null; line = bufferedReader.readLine ()) {
				sb.append (line + "\n");
			}
			sendData = sb.toString ().getBytes ();
			pw.println (sb);
			pw.flush ();
		    if (startTime == 0) startTime = System.currentTimeMillis ();
		    clientSocket.close ();
		    out.close ();
		    pw.close ();
		    sentSum++;
		    counter++;
		}
		System.out.println ("stopped.");
	}

	/**
	 * Calculates the data sending rate, showing it on the console.
	 */
	private void consoleOut () {
		
		double endTime = System.currentTimeMillis ();
		double sendTime = (endTime - startTime) / 1000;
		double dataPerSecond = (sentSum * ((sendData.length * 8)) / 1024) / sendTime;
		PrintStream out = System.out;
		
		out.println ("Packets sent: " + sentSum);		
		out.println ("Packet sending size (in byte): " + sendData.length);
		out.println ("Sending time elapsed (in s): " + sendTime);
		out.println ("Data sending rate (in kbit/s): " + dataPerSecond);
	}
}
