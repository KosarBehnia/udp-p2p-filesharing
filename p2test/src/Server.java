

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server extends Thread {
    private DatagramSocket socket;
    String[] ack;
    InetAddress address;
    int port ;
    private boolean running;
    private byte[] buf = new byte[256]; // maximum buffer size is 256 for incoming packets
    private byte[] res = new byte[1024]; // maximum buffer size is 1024 for http results
    private byte[] reqanswer = new byte[256];
    String receivedack = "";
    String answer = ""; //for answering  whether we have a req file or not
    //array to store file names we have
    private String[] fileava = {"hello.txt", "kosar.txt", "kosarkub.txt"};
    // TODO: packet segmentation

    /**
     * Creates new proxy server that listens on 1373/udp and binds on loopback interface
     * TODO: bind on all interfaces
     */
    public Server() {
        try {
            socket = new DatagramSocket(1373, InetAddress.getLoopbackAddress());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        running = true;

        while (running) {
            // in each iteration waits for new packet then handles it
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            try {
            	//while(packet == null)
                socket.receive(packet);
                System.out.println("server recieved ");
            } catch (IOException e) {
                e.printStackTrace();
                continue; // try to read a new packet
            }
            
            //getting ip address and port of a client
            address = packet.getAddress();
            port = packet.getPort(); // finds sender port in order to answer on

          
            // to find Host option
            String received
                    = new String(packet.getData(), 0, packet.getLength());

            // logs the arrived packet
            System.out.printf("Packet arrived from: %s:%d with content %s\n", address.toString(), port, received);
            
            //splitting to find file name
            String[] filereq = received.split("-");
            String host = "";
            String reqfile = filereq[3];
            System.out.println("filereq name" + filereq[3]);
            if(filereq[3] == "")
            {
            	//it is not a valid request
            	System.out.println("not a valid req");
            }
            
            //searching to see whether we have  a requested file
            for(int i = 0; i < fileava.length ; i++)
            {
            	if(reqfile.equals(fileava[i]))
            	{
            		System.out.println("we have req file -" + reqfile);
            		answer  = ("yes-" + reqfile);
            	}
            }
            
           reqanswer = answer.getBytes(); 
           packet = new DatagramPacket(reqanswer, reqanswer.length, address, port);
           try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("error in sending anser to client");
		}
        System.out.println("answer send to the client");
        //waitting for client to reack :)
        DatagramPacket packet1
        = new DatagramPacket(buf, buf.length);
        try {
			socket.receive(packet1);
			receivedack
             = new String(packet1.getData(), 0, packet1.getLength());
			 System.out.println("server received : " + receivedack );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        ack = receivedack.split("-");
        if(!(ack[0].equals("thanks")))
        {
        	System.out.println("I will send a file");
        	sendfile();
        }

        
            
        }
    }
    private void sendfile()
    {
    	System.out.println("sending a file started :)");
    	Path fileLocation = Paths.get("G:\\computer networks\\project\\file.txt");
		byte[] data = null;
		try {
			data = Files.readAllBytes(fileLocation);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	//packeting started :)
    	double numpacket = Math.ceil((data.length)/(127.0));
        //////////////////////////////////////////////////////////////////
        //sending number of packets.
    	String numpack = Double.toString(numpacket) + "-" + data.length;
    	//converting number of packets to byte
    	byte[]num =  numpack.getBytes();
    	DatagramPacket packet = new DatagramPacket(num, num.length , address, port);
    	try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("error in sending number of packets");
			e.printStackTrace();
		}
    	System.out.println("data size" + data.length);
    	//packeting.......:)
    	//start packeting except the last packet which size is variable
    	for(int i = 0; i < numpacket - 1; i++)
    	{
    		byte[] buffer = new byte[128];
    		buffer[0] = (byte) (i % 256);
    		for(int j = 0; j < 127; j++)
    		{
    			buffer[1 + j] = data[j + i*127];
    		}
    		DatagramPacket packet1 = new DatagramPacket(buffer, buffer.length , address, port);
    		 try {
				socket.send(packet1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("error in sending packets");
				e.printStackTrace();
			}
    	}
		byte[] buffer = new byte[((data.length) % 127) + 1];
		buffer[0] = (byte) ((numpacket - 1)%256);
		//filling the last packet
		for(int i = 0; i < ((data.length) % 127); i++)
		{
			buffer[1 + i] = data[(int) (127*(numpacket -1) + i)];
		}
		DatagramPacket packet1 = new DatagramPacket(buffer, buffer.length , address, port);
		try {
			socket.send(packet1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("error in sending last packet");
			e.printStackTrace();
		}
		System.out.println("sending file finished...:)");
		
    
    	
    }
}

            // Lets connect to the host that is found.
            /*
            try {
                Socket httpSocket = new Socket(host, 81);
                httpSocket.getOutputStream().write(buf); // write client packet without any change

                // read all of the http response
                int read = 0;
                int size = 0;
                do {
                    read += size;
                    size = httpSocket.getInputStream().read(res, read, res.length);
                } while (size > 0 && size < buf.length);

                // as you can google response is larger than 1024 so you must do segmentation
                System.out.printf("%s Response: %s\n", host, new String(res));

                // Check response status code
                // 404 indicates page not found and we must return a valid error
                // 301 and 302 indicate redirection and we must redirect to a given destination

            } catch (IOException e) {
                e.printStackTrace();
            }

            packet = new DatagramPacket(res, res.length, address, port); // answer packet

            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        socket.close();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        this.running = false;
    }
}*/