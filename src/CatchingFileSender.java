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

//created by Nhu Huy Le & Matthias Yan Long

public class CatchingFileSender {
	
	private DatagramSocket socket = null;
	
	// muss diesselbe FileObject-Klasse sein wie beim Empfänger
	private FileObject[] fileGather = null;	
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
			byte[] data;
			DatagramPacket sendPacket = null;
			
			// read and split
			fileGather = getFileObject(PACKET_SIZE, sourceFile);
			
			do{
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);

				if (ack == responseAck) {
					System.out.println();
					System.out.println("ack: " + ack);

					// set ACK
					ack = !ack;
					fileGather[curSeq].setAck(ack);

					// set Checksum
					crc.update(serializeObject(fileGather[curSeq]));
					fileGather[curSeq].setChecksum(crc.getValue());
					crc.reset();
					
					System.out.println("Packet-Ack: " + fileGather[curSeq].getAck());
					System.out.println("Packet-SeqNo: " + fileGather[curSeq].getSeqnum());
					System.out.println("Packet-Size: " + fileGather[curSeq].getData().length);
					System.out.println("Packet-CRC: " + fileGather[curSeq].getChecksum());
					
					// create fileObject
					oos.writeObject(fileGather[curSeq]);
					data = bos.toByteArray();
					sendPacket = new DatagramPacket(data, data.length, ipAddress, port);
					curSeq++;
					System.out.println("TO: " + sendPacket.getSocketAddress());
				}

				// send
				System.out.print("packet sending...");
				socket.send(sendPacket);
				System.out.println("done.");

				// receive response
				System.out.print("receive response...");
				byte[] responseBuffer = new byte[1024];
				DatagramPacket incomingPacket = new DatagramPacket(responseBuffer, responseBuffer.length);
				
				socket.setSoTimeout(timeout);
				socket.receive(incomingPacket);
				System.out.println("done.");
				
				// auswerten
				byte[] responseData = incomingPacket.getData();
				responseAck = responseData[0] != 0;
				System.out.println("responseAck: " + responseAck);
				
			} while (curSeq < fileGather.length);

	    } catch (UnknownHostException e) {
	        e.printStackTrace();
	    } catch (SocketException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	// gibt ein fileObject[] zurück
	// checksum noch nicht gesettet
	private FileObject[] getFileObject(int packetSize, String sourceFile) {
	
		// load fileName = path
		System.out.print("loading file...");
		File file = new File(sourceFile);
		System.out.println("done.");
		System.out.println("Source: " + sourceFile);
		
		FileObject[] fileGather = null;
		
		// testet obs eine file ist
		if (file.isFile()) {
			int fileSize = (int) file.length();
			System.out.println("File-Size: " + fileSize);
			
			// -- -1 (last packet lost)
				// -- ceiling wrong ?
			packetSum = (int) Math.ceil(fileSize/packetSize) + 1;
			System.out.println("Packet Sum: " + packetSum);
		
			try{
				DataInputStream dis = new DataInputStream(new FileInputStream(file));
				
				//File split
				fileGather = new FileObject[packetSum];
				byte[] objectData;

				for (int i = 0; i < packetSum; i++) {					
					objectData = new byte[packetSize];					
					dis.read(objectData);
					
					// setting FileObject
					fileGather[i] = new FileObject();
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
	        fileSender.hostname = args[0];
	        fileSender.sourceFile = args[1];
	        fileSender.sendUdpLoop();
	    }
}
