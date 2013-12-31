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
import java.util.zip.CRC32;

public class CatchingFileSender {
	
	private DatagramSocket socket = null;
	private FileObject[] fileGather = null;	// muss dasselbe FileObject-Klasse sein wie beim Empfänger

	private int port = 4711;
	private int packetSum;
	private int curSeq = 0;
	private int timeout = 100000;
	private boolean ack = false;
	
	private static final int PACKET_SIZE = 1024 * 63;
	private String hostname = "localhost";
	private String sourceFile = "C:/Users/T500/file2send";
	
	public void sendUdpLoop() {
		
		try {
			CRC32 crc = new CRC32();
			socket = new DatagramSocket();
			InetAddress ipAddress = InetAddress.getByName(hostname);
			boolean responseAck = false;
			fileGather = getFileObject(PACKET_SIZE, sourceFile);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			byte[] data = {0};
			DatagramPacket sendPacket = new DatagramPacket(data, curSeq);
			
			do{
				if (ack == responseAck) {
					// set ACK
					ack = !ack;
					fileGather[curSeq].setAck(ack);
					// set Checksum
					crc.update(serializeObject(fileGather[curSeq]));
					fileGather[curSeq].setChecksum(crc.getValue());

					// create fileObject
					oos.writeObject(fileGather[curSeq]);
					data = bos.toByteArray();
					sendPacket = new DatagramPacket(data, data.length, ipAddress, port);
					curSeq++;
				}

				// send
				socket.send(sendPacket);
				System.out.println("Packet sent");
				
				// receive response
				byte[] responseBuffer = new byte[1024];
				DatagramPacket incomingPacket = new DatagramPacket(responseBuffer, responseBuffer.length);
				
				socket.setSoTimeout(timeout);
				socket.receive(incomingPacket);
				
					// auswerten
				byte[] responseData = incomingPacket.getData();
				responseAck = responseData[0] != 0;	
				
				
			} while (curSeq <= fileGather.length);

	    } catch (UnknownHostException e) {
	        e.printStackTrace();
	    } catch (SocketException e) {
	        e.printStackTrace();
	        //kein ack
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	// gibt ein fileObject[] zurück
	// checksum noch nicht gesettet
	private FileObject[] getFileObject(int packetSize, String sourceFile) {
	
		// fileName = path
		File file = new File(sourceFile);
		FileObject[] fileGather = null;
		
		// testet obs eine file ist
		if (file.isFile()) {
			int fileSize = (int) file.length();
			System.out.println("Filesize: " + fileSize);
			
			packetSum = (int) Math.ceil(fileSize/packetSize);
			System.out.println("packetSum: " + packetSum);
		
			try{
				DataInputStream dis = new DataInputStream(new FileInputStream(file));
				
				//File split
				fileGather = new FileObject[packetSum];
				byte[] objectData = new byte[packetSize];
				
				// last packet incl?
				for (int i = 0; i < packetSum; i++) {
					dis.read(objectData, 0, packetSize);
					
					fileGather[i] = new FileObject();
					// setting FileObject
					fileGather[i].setData(objectData);
					fileGather[i].setFileSize(file.length());
					fileGather[i].setFileName(sourceFile);
					fileGather[i].setSeqnum(i);
				
					
				}
				dis.close();
				
			} catch (Exception e) {
                e.printStackTrace();
            }
		}
		
		return fileGather;
	}
	
	/**
	 * @param fileObject
	 * @return
	 * @throws IOException
	 */
	private byte[] serializeObject(FileObject fileObject) throws IOException {
		ByteArrayOutputStream bais = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bais);  
		oos.writeObject(fileObject);
		return bais.toByteArray();
	}
	
	 public static void main(String... args) {
	        CatchingFileSender fileSender = new CatchingFileSender();
	        fileSender.sendUdpLoop();
	    }
}
