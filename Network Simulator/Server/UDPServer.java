/////////////////////////////////////////////////////////////
//
//	Author: Mario Enriquez
//
//	ID: A00909441
//
//	COMP 7005
//
//	Client Emulator
//  
//  Part of a network simulator. Simulates the server part
//  While the packets are Datagrams (UDP), the packets are
//  set to simulate a TCP connection. The server receives 
//  packets and logs results before sending packets with 
//  Acknowledgments
//
/////////////////////////////////////////////////////////////

import java.io.*; 
import java.net.*;
 import java.util.*; 

class UDPServer {    

	public DatagramSocket serverSocket;
	public InetAddress IPAddress;
	public RandomAccessFile fileReceived;
	public RandomAccessFile fileToSend;
	public RandomAccessFile logFile;
	public File file;
	public long fileSize;
	public String UDPNetworkAdress;
	public String path;
	public  int UDPNetworkPort;
	public  int windowSize;
	public  int bufferLength;
	public  int timeout;
	public String config_file= "config.txt"; 
	public String server_file; 
	public String client_file;
	public String log_file;
	public int mode; //1 send, 0 receive
	public String receivedFileSize;
	public boolean waitingToStart;

	public int receivedType;
	public int receivedSeq;
	public int receivedDataSize;
	public byte[] receivedData;
	
	public static final int syn=0; //seqNo = 0
	public static final int synack=1; //seqNo = 0
	public static final int ack=2; // seqNo same as data,synack or finack packets
	public static final int data=3; //seqNo depending on the one is sent
	public static final int eot=4; //works as data, reported in the log
	public static final int eof=5; //works as data, reported in the log
	public static final int dupack=6; // seqNo same as data,synack or finack packets. Works the sae as an ACK
	public static final int fin=7; //seqNo = finalseqNo+1
	public static final int finack=8; //seqNo = finalseqNo+1
	
	public String getConfig(){ // function to get the configuration parameters
		String config = "";
		try{
			path =System.getProperty("user.dir");
			BufferedReader br = new BufferedReader(new FileReader(  path + "/" + config_file));
			String line = new String();
			while((line = br.readLine()) != null ){
				config+=line;
			} 
		}catch(FileNotFoundException f){
			System.out.println("FileNotFoundException: " + f);
		}catch(IOException io){
			System.out.println("IOException: " + io);
		}
		
		return config;
	}
	
	public void set_server_params(String config){ //function to set the configuration parameters related to the client
		String[] config_params = config.split(";");
		String temp = "";
		String newValue;
		
		for(int counter=0; counter<config_params.length;counter++){
			if(config_params[counter].contains("port_network")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				UDPNetworkPort = Integer.parseInt(newValue);
			} else if(config_params[counter].startsWith("networkIP")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				UDPNetworkAdress = newValue;
			} else if(config_params[counter].startsWith("windowSize")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				windowSize = Integer.parseInt(newValue);
			} else if(config_params[counter].startsWith("bufferLength")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				bufferLength = Integer.parseInt(newValue);
			} else if(config_params[counter].startsWith("timeout")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				timeout = Integer.parseInt(newValue);
			} else if(config_params[counter].startsWith("server_file")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				server_file = newValue;
			} else if(config_params[counter].startsWith("client_file")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				client_file = newValue;
			} else if(config_params[counter].startsWith("log_file")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				log_file = newValue;
			}
		}
	}
	
	public void sendPacket(int type, int sequence,byte[] data){ //Sends packet
		try{
			byte[] sendData = createPacket(type,sequence,data.length,data);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, UDPNetworkPort);       
			logFile.write(("Sending Packet: " + type + " Sequence No: " + sequence + " Size: " + sendData.length + " To: " + IPAddress + " Port: " + UDPNetworkPort + "\n").getBytes());
			serverSocket.send(sendPacket);       
		} catch(IOException i){
			System.out.println(""+i);
		}
	}
	
	public int receivePacket(){ //Receives packet
		try {		
			byte[] receiveData = new byte[bufferLength+15];       
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket); 
			UDPNetworkPort=receivePacket.getPort();
			retrievePacketData(receivePacket.getData());
			logFile.write(("Receiving Packet: " + receivedType + " Sequence No: " + receivedSeq + " Size: " + receivePacket.getLength() + " From: " + receivePacket.getAddress() + " Port: " + receivePacket.getPort() + "\n").getBytes());
		}catch(SocketTimeoutException s){
			System.out.println(""+s);
			return 0;
		}catch(IOException i){
			System.out.println(""+i);		
			return -1;
		}
		return 1;
	}
	
	public byte[] createPacket(int packetType, int sequenceNo, int dataSize, byte[] data){ //Creates packet
		String sPacketType = packetType+"";
		String sSequence = (sequenceNo + "          ").substring(0,10);
		String sData= (dataSize + "    ").substring(0,4);
		byte[] auxByteArray = (sPacketType + sSequence + sData).getBytes();
		byte[] byteArray = new byte[auxByteArray.length+bufferLength];
		System.arraycopy(auxByteArray, 0, byteArray, 0, auxByteArray.length); 		
		if(dataSize>0){
			System.arraycopy(data, 0, byteArray, auxByteArray.length, dataSize); 		
		}
		return byteArray;
	} 
	
	public void retrievePacketData(byte[] packet){ //Retrieves data from packet for analysis
		String aux = ((new String(packet)).substring(0,1));
		receivedType = Integer.parseInt(aux);
		aux = (new String(packet)).substring(1,11).replaceAll("\\s+","");
		receivedSeq = Integer.parseInt(aux);
		aux = (new String(packet)).substring(11,15).replaceAll("\\s+","");
		receivedDataSize = Integer.parseInt(aux);
		if((receivedType==syn || receivedType==synack) && receivedDataSize>0){
			receivedFileSize = (new String(packet)).substring(15,15 + receivedDataSize);
		} else{
			if(receivedDataSize>0){
				receivedData= new byte[receivedDataSize];
				System.arraycopy(packet, 15, receivedData, 0, receivedDataSize); 	
			}
		}
	}
	
		public void sendDataMode(){ //Mode for the server to send data
		try{
			System.out.println("Sending File: " + server_file + " Size: " + fileSize);
			fileToSend = new RandomAccessFile((path + "/server_files/" + server_file),"r");
			int followingPacket=0;
			int resend;
			do{
				ArrayList<Integer> acks = new ArrayList<Integer>();
				ArrayList<Integer> dataSize= new ArrayList<Integer>();
				byte[] packetData;
				if((followingPacket+(windowSize*bufferLength))<fileSize){
					logFile.write(("Sending window: "+ followingPacket + " Size: " + (windowSize*bufferLength) +"\n").getBytes());
					for (int i=0;i<windowSize;i++){
						acks.add(followingPacket);
						dataSize.add(bufferLength);
						followingPacket+=bufferLength;
					}
				} else {
					int aux= (int) ((fileSize-followingPacket)/bufferLength);
					logFile.write(("Sending window: "+ followingPacket + " Size: " + (fileSize-followingPacket) +"\n").getBytes());
					aux++;
					for (int i=0;i<aux;i++){
						acks.add(followingPacket);
						if((followingPacket+bufferLength)<fileSize){
							dataSize.add(bufferLength);
							followingPacket+=bufferLength;
						}else{
							dataSize.add((int)fileSize-followingPacket);
							followingPacket=(int)fileSize;
						}
					}
				}
				do{
					serverSocket.setSoTimeout(0);
					for(int j=0;j<acks.size();j++){
						int aux=acks.get(j);
						int aux2=dataSize.get(j);
						packetData=new byte[aux2];
						fileToSend.seek(aux);
						fileToSend.read(packetData,0,aux2);
						if(j==(acks.size()-1)){
							System.out.println("Sending Packet: "+ aux + " Type: " + eot + " Size: " + aux2);
							sendPacket(eot,acks.get(j),packetData);
						} else{
							System.out.println("Sending Packet: "+ aux + " Type: " + data + " Size: " + aux2);
							sendPacket(data,acks.get(j),packetData);
						}						
					}
					resend=0;
					do{
						serverSocket.setSoTimeout(timeout);
						resend=receivePacket();
						if(resend==1){
							System.out.println("Received Packet: "+ receivedSeq + " Type: " + receivedType + " Size: " + receivedDataSize);
							int removeObj=acks.indexOf(receivedSeq);
							if(removeObj>-1){
								acks.remove(removeObj);
								dataSize.remove(removeObj);
							}
						}
						if(resend==-1){
							return;
						}else {
							if(resend==0){
								logFile.write(("Resending...\n").getBytes());
							}
						}
					}while(resend!=0 && acks.size()>0);
				}while(acks.size()>0);
			}while(followingPacket<fileSize);
			do{
				System.out.println("Sending Packet: "+ followingPacket + " Type: " + eof + " Size: " + 0);
				serverSocket.setSoTimeout(0);
				sendPacket(eof,followingPacket,new byte[0]);
				serverSocket.setSoTimeout(timeout);
				resend=receivePacket();
			} while(resend==0);
			fileToSend.close();
		}catch(FileNotFoundException f){
			System.out.println(""+f);
		}catch(SocketException s){
			System.out.println(""+s);
		}catch(IOException i){
			System.out.println(""+i);
		}
		
	}
	
	public void receiveDataMode(){ //Mode for the server to receive data
		try{
			int resend=0;
			fileReceived = new RandomAccessFile((path + "/server_files/" + client_file),"rw");
			fileReceived.setLength(Long.parseLong(receivedFileSize));
			ArrayList<Integer> acks = new ArrayList<Integer>();
			ArrayList<Integer> respond = new ArrayList<Integer>();
			ArrayList<Integer> dup = new ArrayList<Integer>();
			
			do{
				serverSocket.setSoTimeout(0);
				do{
					resend=receivePacket();
					System.out.println("PacketType: " + receivedType + " PacketNo: " + receivedSeq + " Size: " + receivedDataSize);
					respond.add(receivedSeq);
					if(!acks.contains(receivedSeq)){
						fileReceived.seek(receivedSeq);
						fileReceived.write(receivedData,0,receivedDataSize);
					} else{
						System.out.println("duplicate");
						dup.add(receivedSeq);
					}
				}while(receivedType!=eot && receivedType!=eof);
				do{
					if(dup.isEmpty()){
						sendPacket(ack,respond.get(0),new byte[0]);
						acks.add(respond.get(0));
					} else {
						if(!dup.contains(respond.get(0))){
							sendPacket(ack,respond.get(0),new byte[0]);	
							acks.add(respond.get(0));
						} else {
						System.out.println("dupack");
							sendPacket(dupack,respond.get(0),new byte[0]);
							int removeObj=dup.indexOf(respond.get(0));
							dup.remove(removeObj);
						}
					}
					respond.remove(0);
				}while(respond.size()>0);
				if(receivedType==eof){
					System.out.println("EOF");
				}
			}while(receivedType!=eof);
			fileReceived.close();
		}catch(FileNotFoundException f){
			System.out.println(""+f);
		}catch(SocketException s){
			System.out.println(""+s);
		}catch(IOException i){
			System.out.println(""+i);
		}
	}
	
	public void serverConnection(){ //Establishes connection with client, manages transmission and closes connection
		int currentMode=0;
		int resend;
		byte[] data;
		receivedFileSize="-1";
		try{
			serverSocket.setSoTimeout(0);
			System.out.println("Waiting...");
			receivePacket();
			waitingToStart=false;
			System.out.println("...Received SYN from the Client.");
			if(receivedDataSize>0){
				data=new byte[0];
				currentMode=0;
			} else {
				file = new File(path+"/server_files/"+server_file);
				fileSize = file.length();
				data=(fileSize+"").getBytes();
				currentMode=1;
			}
			do{
				System.out.println("Sending SYNACK to the Client...");
				sendPacket(synack,0,data);
				serverSocket.setSoTimeout(timeout);
				resend=receivePacket();
			}while(resend==0);
			if(resend==-1){
				return;
			}
			System.out.println("...Received ACK from the Client.");
			//SendDataMode
			
			if(currentMode==1){
				sendDataMode();
			} else{
				receiveDataMode();
			}
			
			//ReceiveData
			serverSocket.setSoTimeout(0);
			receivePacket();
			System.out.println("...Received FIN from the Client.");
			do{
				System.out.println("Sending FINACK to the Client...");
				sendPacket(finack,0,new byte[0]);
				serverSocket.setSoTimeout(timeout);
				resend=receivePacket();
			}while(resend==0);
			System.out.println("...Received ACK from the Client.");
		}catch(SocketException s){
			System.out.println(""+s);
		}
	}
	
	public static void main(String args[]) throws Exception       {    
		UDPServer udpServer = new UDPServer();
		String config=udpServer.getConfig();
		udpServer.set_server_params(config);
		udpServer.serverSocket = new DatagramSocket(udpServer.UDPNetworkPort);             
		byte[] receiveData = new byte[udpServer.bufferLength+15];             
		byte[] sendData = new byte[udpServer.bufferLength+15];             
		udpServer.IPAddress = InetAddress.getByName(udpServer.UDPNetworkAdress);       
		File logs= new File(udpServer.path + "/server_files/" + udpServer.log_file);
		logs.delete();
		logs.createNewFile();
		udpServer.logFile = new RandomAccessFile((udpServer.path + "/server_files/" + udpServer.log_file),"rw");		
		while(true){        
			udpServer.serverConnection();
		}       
	} 
}