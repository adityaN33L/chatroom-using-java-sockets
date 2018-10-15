import java.io.*;
import java.net.*;
import java.util.*;

public class Client{
    public static void main(String[] args) throws UnknownHostException, IOException{

    	String username = args[0];
    	String server_ip = args[1];
    	int server_port = Integer.parseInt(args[2]);

        Socket s = new Socket(server_ip,server_port);
        DataInputStream dis = new DataInputStream(s.getInputStream());
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        Scanner scn = new Scanner(System.in);

        // Send username to the server
        dos.writeUTF("username: " + username);
        
        // send message thread
        Thread sendMessage = new Thread(new Runnable(){
            public void run(){
                while (true){
                    // read the message to deliver.
                    String msgSend = scn.nextLine();
                    try {
                        // write on the output stream
                        dos.writeUTF(msgSend);

                        // --------------------------------Client exit condition -------------------------------//

                        if(msgSend.equals("exit")){
                        	System.out.println("Exiting...");
                        	s.close();
                        	return;
                        }
                      	
                        String[] splitedMessage = msgSend.split("\\s+");
                        String protocol = splitedMessage[splitedMessage.length - 1];

                        //------------------------- Send file attributes and then transfer File ---------------------------//

                        if(protocol.equals("tcp") || protocol.equals("udp")){
                        	String filename = splitedMessage[1];
	                        String filepath = System.getProperty("user.dir") + "/" + filename;
	                        File myFile = new File (filepath);
	                        if(myFile.exists()){
	                        	String fileAttributes = "FileAttributes: " + Long.toString(myFile.length()) + " " + filename + " " +protocol;
		                        dos.writeUTF(fileAttributes);

		                        System.out.println("Sending "+filename+"...");
								byte [] mybytearray  = new byte [(int)myFile.length()];
								FileInputStream fis = new FileInputStream(myFile);
								BufferedInputStream bis = new BufferedInputStream(fis);
								bis.read(mybytearray,0,mybytearray.length);
								OutputStream os = s.getOutputStream();
								os.write(mybytearray,0,mybytearray.length);
								os.flush();
								bis.close();
								System.out.println("Sent file...");	
	                        }
	                        else
	                        	System.out.println(filename + " does not exist in the required path...");                        
                        }

                    } 
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
         
        // readMessage thread
        Thread readMessage = new Thread(new Runnable(){
            public void run() {
                while (true) {
                    try {
                        // read the message sent to this client
                        String msgRcv = dis.readUTF();
                        if(msgRcv.equals("No more clients can be accepted...")){
                        	s.close();
                        	break;
                        }
                        String[] splitedMessage = msgRcv.split("\\s+");
                        int filesize;
                        String filename,protocol,sender;

                        //-----------------------Recieve file attribuites and then the file ------------------------//

                        if(splitedMessage[0].equals("FileAttributes:")){ 
                        	filesize = Integer.parseInt(splitedMessage[1]);
							filename = splitedMessage[2];
							protocol = splitedMessage[3];
							sender = splitedMessage[4];
							System.out.println("Recieving " + filename + " from " + sender + "...");

							int current = 0, tillNow = 0;
							String destFile = System.getProperty("user.dir") + "/" + filename;
							byte [] mybytearray  = new byte [6022386];
					        InputStream is = s.getInputStream();
					        FileOutputStream fos = new FileOutputStream(destFile);
					        BufferedOutputStream bos = new BufferedOutputStream(fos);
					        
				        	while ((current = is.read(mybytearray)) > 0) {
					        	bos.write(mybytearray, 0, current);
					        	tillNow += current;
					        	if(tillNow >= filesize)
					        		break;
					        }
				        	bos.flush();
				        	bos.close();
				        	System.out.println("Recieved " + filename + " from " + sender + "...");
				        	continue;
                        }
                        System.out.println("=> " + msgRcv); 

                    } 
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        sendMessage.start();
        readMessage.start();
    }
}