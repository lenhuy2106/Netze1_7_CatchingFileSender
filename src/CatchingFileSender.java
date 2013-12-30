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
	private FileObject[] fileObject = null;	// muss dasselbe FileObject-Klasse sein wie beim Empfänger
	private String hostname = "localhost";
	private int port = 9876;
	private int n_packets;
	private int seqnum;
	private int timeout = 1000;
	
	private static final int PACKET_SIZE = 1024;
	// brauchen keine sources - einfach die datei im selben verzeichnis wie das progg
		
	
	public void sendUdpLoop() {
		
		try {
			
			socket = new DatagramSocket();
			InetAddress ipAddress = InetAddress.getByName(hostname);
			
			// fileObject Array, weil ich glaub, dass ich sonst nur 1 paket hab. und jetz pakete mit größe PACKET_SIZE erstellen kann
			// nich sicher, ob ich header setzen muss, oder autogen.
			for(int i = 0; i < n_packets ;i++){
				fileObject[i] = readObject(PACKET_SIZE);
				fileObject[i].setSeqnum(i);
				fileObject[i].setChecksum(PACKET_SIZE);
			}
			
			do{
			ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(byteArrayOS);
			os.writeObject(fileObject[seqnum]);
			byte[] data = byteArrayOS.toByteArray();
			byte[] incomingData = new byte[1024];
			DatagramPacket sendPacket = new DatagramPacket(data,data.length,ipAddress, port);
			socket.send(sendPacket);
			System.out.println("Packet sent");
			DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
			socket.setSoTimeout(timeout);
			socket.receive(incomingPacket);
			String response = new String(incomingPacket.getData());
			System.out.println("Response from server:"+response);

			
			//--Incoming Packet/timeout auswerten und prüfen ob ack or nak. -> seqnum++ or do nothing.
			seqnum++;
			
			
			}while(seqnum <= n_packets);
			

	    } catch (UnknownHostException e) {
	        e.printStackTrace();
	    } catch (SocketException e) {
	        e.printStackTrace();
	        //kein ack
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	
	// gibt ein fileObject zurück
	private FileObject readObject(int packetSize) {
		FileObject fileObject = new FileObject();
		String fileName = "fileToSend";
	
		// fileName = path
		File file = new File(fileName);
		n_packets = (int) Math.ceil(file.length()/packetSize);
		
		// testet obs eine file ist
		if (file.isFile()) {
		
			try{
				DataInputStream diStream = new DataInputStream(new FileInputStream(file));
				long len = (int)file.length();
				//byte[] fileBytes = new byte[(int) len];		// 
//				int read = 0;
//				int numRead = 0;
//				while(read < fileBytes.length && (numRead = diStream.read(fileBytes,read,fileBytes.length - read)) >= 0){
//					read = read +numRead;
//				}
				
				// erzeugt fileObjects mit größe PACKET_SIZE
				byte[] fileBytes = new byte[PACKET_SIZE];
				byte[] trash = new byte[PACKET_SIZE];
				diStream.read(trash,0,PACKET_SIZE);
				diStream.read(fileBytes,0,(int)len - PACKET_SIZE * seqnum);
				fileObject.setFileSize(len);
				fileObject.setData(fileBytes);
				diStream.close();
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
