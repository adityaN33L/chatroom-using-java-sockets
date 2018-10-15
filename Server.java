import java.io.*;
import java.util.*;
import java.net.*;

public class Server{
    public static HashMap<String, Socket> user2Socket = new HashMap<String, Socket>();
    public static HashMap<String, String> user2ChatRoom = new HashMap<String, String>();
    public static HashMap<String,List<String> > chatRoom2Users = new HashMap<String,List<String> >();
    
    public static void main(String[] args) throws IOException{
    	int clients = Integer.parseInt(args[0]);
    	String server_ip = args[1];
    	int server_port = Integer.parseInt(args[2]);

        ServerSocket ss = new ServerSocket(server_port);
        System.out.println("Server online...");
        while(true){
            Socket s = null;
            try{
            	if(clients >= 0){
            		s = ss.accept();
            		clients--;	
            		DataInputStream dis = new DataInputStream(s.getInputStream());
            		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            		Scanner scn = new Scanner(System.in);
            		Thread t = new ClientHandler(s,dis,dos,scn,user2Socket,user2ChatRoom,chatRoom2Users,clients);
            		t.start();	
            	}        		 
            }
            catch(Exception e){
                s.close();
                e.printStackTrace();
            }
        }
    }
}

class ClientHandler extends Thread{
    final DataInputStream dis;
    final DataOutputStream dos;
    final Scanner scn;
    final Socket s;
    public static HashMap<String, Socket> user2Socket;
    public static HashMap<String, String> user2ChatRoom;
    public static HashMap<String,List<String> > chatRoom2Users;
    private int i,l;
    private String strRcvd,username = "",chatroom = "";
    public static int clients;

    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos,Scanner scn,HashMap<String, Socket> user2Socket,HashMap<String, String> user2ChatRoom,HashMap<String,List<String> > chatRoom2Users,int clients){
        this.s = s;
        this.dis = dis;
        this.dos = dos;
        this.scn = scn;
        this.user2Socket = user2Socket;
        this.user2ChatRoom = user2ChatRoom;
        this.chatRoom2Users = chatRoom2Users;
        this.clients = clients;
    }

    public void run(){

    	try{
	    	if(clients < 0) {
	    		dos.writeUTF("No more clients can be accepted...");
	    		this.s.close();
	    		return;
	    	}	
    	}
    	catch (IOException e) {
            e.printStackTrace();
        }
    	
    	
        while(true){
            try{
                strRcvd = dis.readUTF();
                
                System.out.println("client: => " + strRcvd);
                String[] splitedMessage = strRcvd.split("\\s+");

                //---------------------------- get username from client------------------------------//

                if(splitedMessage[0].equals("username:")){
                    username = splitedMessage[1];
                    if(!user2Socket.containsKey(username)){
                        user2Socket.put(username,s);
                        dos.writeUTF(splitedMessage[1] + " registered...");    
                    }
                    else
                        dos.writeUTF("Username: " + username + " already exists...");                    
                }

                //--------------------------------------- create chatroom -------------------------------------//

                else if(splitedMessage[0].equals("create") && splitedMessage[1].equals("chatroom")){
                    if(user2ChatRoom.containsKey(username)){
                        dos.writeUTF("The user is already in a chatroom...");
                        continue;
                    }
                    chatroom = splitedMessage[2];
                    List<String> userlist = chatRoom2Users.get(chatroom);
                    if(userlist == null) {
                        userlist = new ArrayList<String>();
                        chatRoom2Users.put(chatroom, userlist);
                        userlist.add(username);
                        user2ChatRoom.put(username,chatroom);
                        dos.writeUTF(chatroom + " is successfully created and you (" + username + ") are added as the first member...");
                    }
                    else
                        dos.writeUTF(chatroom + " already exists...");   
                }

                //----------------------------------------- list chatrooms -----------------------------------------//

                else if(splitedMessage[0].equals("list") && splitedMessage[1].equals("chatrooms")){
                    String listChatRooms = "";
                    for(String key : chatRoom2Users.keySet())
                        listChatRooms += key + ", ";

                    if(!listChatRooms.equals(""))
                    	listChatRooms = listChatRooms.substring(0, listChatRooms.length() - 2);
                    dos.writeUTF("List of chat rooms: " + listChatRooms);   
                }

                //------------------------------------------ join chatroom -----------------------------------------//

                else if(splitedMessage[0].equals("join")){
                    chatroom = splitedMessage[1];
                    if(user2ChatRoom.containsKey(username)){
                        dos.writeUTF(username + " is already in an existing chatroom...");
                        continue;   
                    }
                    if(chatRoom2Users.containsKey(chatroom)){
                        List<String> userlist = chatRoom2Users.get(chatroom);
                        userlist.add(username);
                        user2ChatRoom.put(username,chatroom);
                        dos.writeUTF("You are now a member of "+ chatroom + "...");

                        //------- broadcast in the group that a new user has been added to the chatroom-------//

                        l = userlist.size();
                        DataOutputStream dosNew = null;
                        for(i = 0;i<l;++i){
                            String member = userlist.get(i);
                            if(member == username)
                                continue;
                            Socket sockInfo = user2Socket.get(member);
                            dosNew = new DataOutputStream(sockInfo.getOutputStream());
                            dosNew.writeUTF(username + " is now a member of " + chatroom + "...");
                            dosNew.flush();
                        }                      
                        
                    }
                    else
                        dos.writeUTF(chatroom + " does not exist...");
                }

                //------------------------------ Leave chatroom ------------------------------//

                else if(splitedMessage[0].equals("leave")){
                    if(!user2ChatRoom.containsKey(username)){
                        dos.writeUTF(username + " is not part of any chatroom...");
                        continue;
                    }

                    chatroom = user2ChatRoom.get(username);

                    if(chatRoom2Users.containsKey(chatroom)){
                        List<String> userlist = chatRoom2Users.get(chatroom);
                        userlist.remove(username);
                        if(userlist.size() == 0)
                            chatRoom2Users.remove(chatroom);
                        user2ChatRoom.remove(username);
                        dos.writeUTF("You have left " + chatroom + "...");
                        
                        //------------ broadcast in the group that the user has left the chatroom ------------//

                        DataOutputStream dosNew = null;
                        l = userlist.size();
                        for(i = 0;i<l;++i){
                            String member = userlist.get(i);
                            Socket sockInfo = user2Socket.get(member);
                            dosNew = new DataOutputStream(sockInfo.getOutputStream());
                            dosNew.writeUTF(username + " has left " + chatroom + "...");
                            dosNew.flush();
                        }

                    }
                    else
                        dos.writeUTF(chatroom + "does not exist...");
                }

                //----------------------------------- List users in the chatroom -------------------------------//

                else if(splitedMessage[0].equals("list") && splitedMessage[1].equals("users")){
                    chatroom = user2ChatRoom.get(username);
                    if(chatroom == null){
                        dos.writeUTF("You are not a member of any chatroom...");
                        continue;
                    }
                    String userlist = "";
                    List<String> temp = chatRoom2Users.get(chatroom);
                    for(i = 0;i<temp.size();++i)
                        userlist += temp.get(i) + ", ";

                    userlist = userlist.substring(0, userlist.length() - 2);
                    dos.writeUTF("List of users in " + chatroom + " : " + userlist);
                }

                //------------------------------------- Add new user to chatroom -------------------------------//

                else if(splitedMessage[0].equals("add")){
                	if(user2ChatRoom.get(username) == null){
                		dos.writeUTF("You cannot add a member unless you are a member of some group...");
                		continue;
                	}
                    String newuser = splitedMessage[1];
                    if(!user2Socket.containsKey(newuser)){
                        dos.writeUTF(newuser + " is offline...");
                        continue;
                    }
                    if(user2ChatRoom.containsKey(newuser)){
                        dos.writeUTF(newuser + " already a member of a group...");
                        continue;
                    }

                    chatroom = user2ChatRoom.get(username);
                    
                    if(chatroom != null){
                        user2ChatRoom.put(newuser,chatroom);
                        List<String> userlist = chatRoom2Users.get(chatroom);
                        userlist.add(newuser);
                        dos.writeUTF(newuser + " is now a member of " + chatroom);

                        Socket newSocket = user2Socket.get(newuser);
                        DataOutputStream dosNew = new DataOutputStream(newSocket.getOutputStream());
                        dosNew.writeUTF("You are now a member of " + chatroom);
                        dosNew.flush();
                    }
                }

                //----------------------------------- File transfer will take place ------------------------------//

                else if(splitedMessage[0].equals("FileAttributes:")){

					int filesize = Integer.parseInt(splitedMessage[1]);
					String filename = splitedMessage[2];
					String protocol = splitedMessage[3];

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


		        	List<Socket> memberSockets = new ArrayList<Socket>();
					List<String> userlist = chatRoom2Users.get(chatroom);

					l = userlist.size();
					for(i = 0;i < l;i++){
						String member = userlist.get(i);
						if(member == username)
							continue;
						memberSockets.add(user2Socket.get(member));
					}
					l--;					

					String filepath = System.getProperty("user.dir") + "/" + filename;
                    File myFile = new File (filepath);
                    DataOutputStream dosNew = null;
                    String fileAttributes = "FileAttributes: " + Long.toString(myFile.length()) + " " + filename + " " +protocol + " " + username;

                    for(i = 0;i<l;++i){    
                    	Socket sockInfo = memberSockets.get(i);
                        dosNew = new DataOutputStream(sockInfo.getOutputStream());
                        dosNew.writeUTF(fileAttributes);	
                        dosNew.flush();               	
	        
	        			mybytearray  = new byte [(int)myFile.length()];
						FileInputStream fis = new FileInputStream(myFile);
						BufferedInputStream bis = new BufferedInputStream(fis);
						bis.read(mybytearray,0,mybytearray.length);
						OutputStream os = sockInfo.getOutputStream();
						os.write(mybytearray,0,mybytearray.length);
						os.flush();
						bis.close();
                    }
                    myFile.delete();
				}                

                //------------------------------- valid message to be broadcasted in the group --------------------------//

                else if(splitedMessage[0].equals("reply")){

                	int msgLen = splitedMessage.length;
					String protocol = splitedMessage[msgLen-1];
					
					chatroom = user2ChatRoom.get(username);
					if(chatroom == null){
					    dos.writeUTF("You are not member of any chatroom...");
					    continue;
					}
					List<Socket> memberSockets = new ArrayList<Socket>();
					List<String> userlist = chatRoom2Users.get(chatroom);

					l = userlist.size();
					for(i = 0;i < l;i++){
						String member = userlist.get(i);
						if(member == username)
							continue;
						memberSockets.add(user2Socket.get(member));
					}
					l--;
					// simple message to be broadcasted in the chatroom
					if(!protocol.equals("tcp") && !protocol.equals("udp")){
						splitedMessage[0] = username + ":";
						String msg2All = String.join(" ", splitedMessage);
					    DataOutputStream dosNew = null;
					    
					    for(i = 0;i < l;++i){
					        Socket sockInfo = memberSockets.get(i);
					        dosNew = new DataOutputStream(sockInfo.getOutputStream());
					        dosNew.writeUTF(msg2All);
					        dosNew.flush();
					    }
					}
                }

                //--------------------------------- client exiting -------------------------------------//

                else if(strRcvd.equals("exit")){
                    user2Socket.remove(username);
                    this.s.close();
                    clients += 1;
                    System.out.println("$$ " + clients);
                    break;
                }

                //--------------------------------- Invalid Command ---------------------------------//

                else
                	dos.writeUTF("Invalid command...");

            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }        
    }
}
