import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Random;

public class UdpClient {
	public static void main(String[] args) {
		try (Socket socket = new Socket("18.221.102.182", 38005)){
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			PrintStream ps = new PrintStream(os);
			
			byte[] message = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
			ps.write(makeIP4(4, message));
			System.out.print("Handshake response: ");
			fromServer(is);
			int portNum = getPortNumb(socket);
			double sentTime = 0, timeRan = 0, avg = 0, elapsed = 0;
			System.out.println("Port number received: " + portNum);
			int size = 1;
			
			for (int i = 0; i < 12; ++i){
			size <<= 1;
				byte[] data = getData(size);		                                                               
				System.out.println("Data length: " + size);
				
				ps.write(makeIP4(size+8, UdpHeader(portNum, data)));
				sentTime = System.currentTimeMillis();
				System.out.print("Response: ");
				fromServer(is);
				
				timeRan = System.currentTimeMillis();
				elapsed = timeRan - sentTime;
				System.out.println("RTT: " + elapsed + "ms\n");
				avg += elapsed;
			}
			System.out.println("Average RTT: " + (avg/12) + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

	public static byte[] makeIP4(int size, byte[] data){
		short length = (short) (20+size);
		byte[] packet = new byte[length];
		
		packet[0] = 0x45; 	
		packet[1] = 0x0;
		packet[2] = (byte) ((length >> 8) & 0xFF);
		packet[3] = (byte) (length & 0xFF); 	
		packet[4] = 0x0;	
		packet[5] = 0x0;
		packet[6] = (0x1 << 6); 	
		packet[7] = 0x0;	
		packet[8] = 0x32; 	
		packet[9] = 0x11; 	
		packet[10] = 0x0; 	
		packet[11] = 0x0;
		packet[12] = (byte) 192; 	
		packet[13] = (byte) 168;	
		packet[14] = (byte) 1;	
		packet[15] = (byte) 64;		
		packet[16] = (byte) 18;	
		packet[17] = (byte) 221;
		packet[18] = (byte) 102;
		packet[19] = (byte) 182;

		short cksum = checksum(packet);
		packet[10] = (byte) ((cksum >> 8) & 0xFF);
		packet[11] = (byte) (cksum & 0xFF);
		
		for (int i = 20; i < 20+data.length; ++i){
			packet[i] = data[i-20];
		}
		return packet;
	}
	
	public static byte[] UdpHeader(int port, byte[] data){
		int length = 8 + data.length;	
		byte[] UDP = new byte[length];		
		UDP[0] = (byte) 0xFF;
		UDP[1] = (byte) 0xFF;			
		UDP[2] = (byte) ((port & 0xFF00) >>> 8);
		UDP[3] = (byte) (port & 0x00FF);
		UDP[4] = (byte) (length >> 8);	
		UDP[5] = (byte) length;
		UDP[6] = 0; 
		UDP[7] = 0;	
		for (int i = 8; i < length; ++i){	
			UDP[i] = data[i-8];
		}	
		short checksum = pseudoHeader(length, port, data);
		UDP[6] = (byte) ((checksum >> 8) & 0xFF);
		UDP[7] = (byte) (checksum & 0xFF);
		
		return UDP;
	}

	public static short pseudoHeader(int UdpLength, int port, byte[] data){
		int length = 20 + UdpLength;
		byte[] packet = new byte[length];
		packet[0] = (byte) 192; 	
		packet[1] = (byte) 168;	
		packet[2] = (byte) 1;	
		packet[3] = (byte) 64;	
		packet[4] = (byte) 18;	
		packet[5] = (byte) 221;
		packet[6] = (byte) 102;
		packet[7] = (byte) 182;	
		packet[8] = 0x0;
		packet[9] = 0x11;		
		packet[10] = (byte) (UdpLength >> 8);	
		packet[11] = (byte) (UdpLength & 0xFF);	
		packet[12] = (byte) 0xFF;	           
		packet[13] = (byte) 0xFF;	
		packet[14] = (byte) ((port & 0xFF00) >>> 8);	             
		packet[15] = (byte) (port & 0x00FF);	
		packet[16] = (byte) (UdpLength >> 8); 	           
		packet[17] = (byte) (UdpLength & 0xFF);
		
		for (int i = 0; i < data.length; ++i){
			packet[i+18] = data[i];
		}
		return checksum(packet);
	}

	
	public static byte[] getData(int size){
		Random rand = new Random();
		byte[] data = new byte[size];
		for (int i = 0; i < size; ++i){
			data[i] = (byte) rand.nextInt(256);
		}
		return data;
	}
	
	public static int getPortNumb(Socket socket){
		  try {
	            int portNumb = -1;
	            InputStream is = socket.getInputStream();
	            byte[] received = new byte[2];
	            received[0] = (byte) is.read();
	            received[1] = (byte) is.read();
	            portNumb = ((received[0] & 0xFF) << 8) | (received[1] & 0xFF);
	            return portNumb;
	        } catch (Exception e) { }
	        return -1;
	}
	public static void fromServer(InputStream is){
		try {
			System.out.print("0x");
			byte fromServer = 0;
			for (int i = 0; i < 4; ++i){
				fromServer = (byte) is.read();
				System.out.print(Integer.toHexString(fromServer & 0xFF).toUpperCase());
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println();
	}
	 public static short checksum(byte[] bytes) {
	        int length = bytes.length;
	        int index = 0;
	        long sum = 0;
	       while (length > 1) {
	            sum += (((bytes[index]<<8) & 0xFF00) | ((bytes[index + 1]) & 0xFF));
	            if ((sum & 0xFFFF0000) > 0){
	                sum = sum & 0xFFFF;
	                sum += 1;
	            }
	            index += 2;
	            length -= 2;
	        }
	        if (length > 0) {
	            sum += (bytes[index]<<8 & 0xFF00);
	            if ((sum & 0xFFFF0000) > 0){
	                sum = sum & 0xFFFF;
	                sum += 1;
	            }
	        }
	        sum = sum & 0xFFFF;
	        return (short)~sum;
	    }
}