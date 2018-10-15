# Chatroom Using Java Sockets

### Problem Statement:
Implement a client server model for communication amongst multiple connected clients.
Generate client/server chat room with sockets.  
### Execution directions:
##### For server:
    bash Server.sh <limit> <server_ip> <port>
	eg. bash Server.sh 2 127.0.0.1 2550
	
##### For client:
    bash Client.sh <username> <server_ip> <port>
    eg. bash Client.sh aditya 127.0.0.1 2550
	
##### Features implemented:
-- Support upto N clients
-- Commands
- create chatroom
- list chatrooms
- join chatroom
- leave chatroom
- list users in a chatroom
- add user to chatoroom

-- Message and file braodcast in the group using both TCP and UDP.