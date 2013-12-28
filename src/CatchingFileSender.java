import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class CatchingFileSender {
	
	private DatagramSocket socket = null;
	private FileObject fileObject = null;	// muss dasselbe FileObject-Klasse sein wie beim Empfänger
	private String hostname = "localhost";
	private int port = 9876;
	// brauchen keine sources - einfach die datei im selben verzeichnis wie das progg
		
	public void sendUdpLoop() {
		
		try {
			
			socket = new DatagramSocket();
			InetAddress ipAddress = InetAddress.getByName(hostname);
			byte[] incomingData = new byte[1024];
			fileObject = readObject();
			ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(byteArrayOS);
			os.writeObject(fileObject);
			byte[] data = byteArrayOS.toByteArray();
			DatagramPacket sendPacket = new DatagramPacket(data,data.length,ipAddress, port);
			socket.send(sendPacket);
			System.out.println("File sent");
			DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
			socket.receive(incomingPacket);
			String response = new String(incomingPacket.getData());
			System.out.println("Response from server:"+response);
			Thread.sleep(2000);
			System.exit(0);

	    } catch (UnknownHostException e) {
	        e.printStackTrace();
	    } catch (SocketException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	// gibt ein fileObject zurück
	private FileObject readObject() {
		FileObject fileObject = new FileObject();
		String fileName = "fileToSend";
	
		// fileName = path
		File file = new File(fileName);
		
		// testet obs eine file ist
		if (file.isFile()) {
		
			try{
				DataInputStream diStream = new DataInputStream(new FileInputStream(file));
				long len = (int)file.length();
				byte[] fileBytes = new byte[(int) len];
				int read = 0;
				int numRead = 0;
				while(read < fileBytes.length && (numRead = diStream.read(fileBytes,read,fileBytes.length - read)) >= 0){
					read = read +numRead;
				}
				fileObject.setFileSize(len);
				fileObject.setData(fileBytes);
				
			} catch (Exception e){
                e.printStackTrace();
            }
		}
		
		return fileObject;
	}
	
	 public static void main(String... args) {
	        CatchingFileSender fileSender = new CatchingFileSender();
	        fileSender.sendUdpLoop();
	    }
}
