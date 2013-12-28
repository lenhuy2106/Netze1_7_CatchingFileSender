import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class CatchingFileSender {
	
	
	private DatagramSocket socket = null;
	
	// muss dasselbe FileObject-Klasse sein wie beim Empfänger
	private FileObject fileObject = null;
	
	// brauchen keine sources - einfach die datei im selben verzeichnis wie das progg
	
	private String hostname = "localhost";
	
	public void sendUdpLoop() {
		
		try {
			socket = new DatagramSocket();
			InetAddress ipAddress = InetAddress.getByName(hostname);
			byte[] data = new byte[1024];
			fileObject = readObject();
			
			
	// -- attribute setzen
			
			
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
}
