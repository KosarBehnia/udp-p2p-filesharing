
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        byte[] buf = new byte[256]; // maximum buffer size is 256 for incoming packets
        String[] reqans = null;
        String received = "";
        int starserver = -1;   //storing the number of server which have the req file
        int size = 0 ; // recording number of connected servers
        Scanner stdin = new Scanner(System.in);
        String filename = null;
        //getting number of connected servers
        System.out.println("please enter number of connected servers");
        size = stdin.nextInt();
        //recording created socket for each connected server
        DatagramSocket[] serversockets = new DatagramSocket [size];
        //recording port of each server
        int[] serverport = new int[size];
        //recording IP of each server
        String[] serverIP = new String[size];
        //getting IP and Port for each server
        for(int i = 0; i < size; i++)
        {
        	System.out.println("please enter the port of server " + i);
        	int serport = stdin.nextInt();
        	// should check if the port is not reserved
        	serverport[i] = serport;
        	System.out.println("please enter the IP of server" + i);
        	String serIP = stdin.next();
        	//should check whether the IP is a valid one
        	serverIP[i] = serIP;
        }
        while (true) {
        	 new Server().start();
            // read user requested file
            System.out.println("Please your requested filename :");
            while(filename == null)
             filename = stdin.next(); // read a line from stdin(requested file)
            
            //making requested string
            String request = "Hi - i want a file - file name -" + filename;
            System.out.println(request);

            try {
                System.out.println("client sending request started :)");
                //creating socket for each server
                for(int i = 0; i < size; i++)
                {
                DatagramSocket client = new DatagramSocket();
                serversockets[i] = client;
                }
                //sending req to each of the servers
                for (int i = 0; i < size ; i++)
                serversockets[i].send(new DatagramPacket(request.getBytes(),
                		request.length(), InetAddress.getByName(serverIP[i]), serverport[i]));
                
                System.out.println("client waiting for the answer......");
                {
                
               loop: for(int i = 0; i < size ; i++)
                {
                DatagramPacket waitting = new DatagramPacket(buf, buf.length);
                serversockets[i].receive(waitting);
                 received
                = new String(waitting.getData(), 0, waitting.getLength());
                System.out.println("client recieved " + received);
                reqans = received.split("-");   //splitting answer received from the server
                if(reqans[0].equals("yes"))
                {
                	starserver = i;
                	System.out.println("server has the file IP: "+serverIP[i] + " port: " + serverport[i]);
                	break loop;
                }

                }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Sending req to selected server to send a file
            if(starserver != -1) //if at least one server has the file
            {
            	String ackfile = "send me file + " + filename;
            	 try {
					serversockets[starserver].send(new DatagramPacket(ackfile.getBytes(),
					 		ackfile.length(), InetAddress.getByName(serverIP[starserver]), serverport[starserver]));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                 
            }
            //rejecting other servers
            for(int i = 0; i < size; i++)
            {
            	if(i != starserver)
            	{
            		String ackfile = "thanks";
               	 try {
   					serversockets[i].send(new DatagramPacket(ackfile.getBytes(),
   					 		ackfile.length(), InetAddress.getByName(serverIP[i]), serverport[i]));
   				} catch (IOException e) {
   					// TODO Auto-generated catch block
   					e.printStackTrace();
   				}
            	}
            }
            //Listening to receive a file
            DatagramPacket numpacket = new DatagramPacket(buf, buf.length);
            try {
				serversockets[starserver].receive(numpacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
             String inforec
            = new String(numpacket.getData(), 0, numpacket.getLength());
            String[] infosplit = inforec.split("-");
            double num =  Double.parseDouble(infosplit[0]);
            double filelength = Double.parseDouble(infosplit[1]);
            System.out.println("file length " + filelength);
            //for recording received bytes
            byte[]file = new byte[(int) filelength];
            System.out.println("download started.....:)");
            //start receiving file
            //first receiving first 256 packets
            for(int i  = 0; i < Math.min(num, 256) ; i++)
            	{
            	 DatagramPacket filepack = new DatagramPacket(buf, buf.length);
                 try {
     				serversockets[starserver].receive(filepack);
     			} catch (IOException e) {
     				// TODO Auto-generated catch block
     				System.out.println("error in downloading file " );
     				e.printStackTrace();
     			}
                //finding which packet we recieved
                int numberpack = buf[0];
                //because byte range from -127 to 127
                if(numberpack > 0)
                {
                	//127 is number of bytes in each packet
                	for(int j = 0; j < 127 ; j++)
                	{
                		file[numberpack*127 + j ] = buf[1 + j];
                	}
                }
            	//if numberpacket < 0
                 else
                	 for(int j = 0; j < 127 ; j++)
                 	{
                 		file[-numberpack*127 + j ] = buf[1 + j];
                 	}
                }
            //receiving other packets
            if(num >= 256)
            {
            	 DatagramPacket filepack = new DatagramPacket(buf, buf.length);
                 try {
     				serversockets[starserver].receive(filepack);
     			} catch (IOException e) {
     				// TODO Auto-generated catch block
     				System.out.println("error in downloading file " );
     				e.printStackTrace();
     			}
                //finding which packet we recieved
                int numberpack = buf[0];
                //because byte range from -127 to 127
                if(numberpack > 0)
                {
                	//127 is number of bytes in each packet
                	for(int j = 0; j < 127 ; j++)
                	{
                		file[(numberpack + 255)*127 + j ] = buf[1 + j];
                	}
                }
            	//if numberpacket < 0
                 else
                	 for(int j = 0; j < 127 ; j++)
                 	{
                 		file[(-numberpack*127 + 255) + j ] = buf[1 + j];
                 	}
            	
            }
            System.out.println("download finished :)");
            System.out.println("building file started ");
            try (FileOutputStream fos = 
            		new FileOutputStream("G:\\computer networks\\project\\receviedfile.txt")) {
            	   try {
					fos.write(file);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	   //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
            	} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            System.out.println("Do you want to continue? (Y/n)");
            String answer = stdin.next();
            if (!answer.equals("Y")) {
                break;
            }
            
        }
        System.exit(0);
    }
}