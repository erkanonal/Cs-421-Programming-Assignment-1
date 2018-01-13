import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class InstantMessenger {

	static ArrayList<String> uNames = new ArrayList<String>(); // A dynamic array for usernames
	static ArrayList<String> uIPs = new ArrayList<String>(); // A dynamic array for IP addresses
	static ArrayList<String> uPorts = new ArrayList<String>(); // A dynamic array for ports

	public static void main(String[] args) {
		// System.out.println("Messenger");
		
		String username = args[0];
		String serverURL = args[1]; 
		String mode = args[2];

		
		/*String username = "kemal";
		String serverURL = "127.0.0.1";
		String mode = "send";// send or listen*/

		try {
			if (mode.equals("listen")) {
				// Peer to peer connection with UDP
				// Open server socket
				DatagramSocket serverSocket = new DatagramSocket();
				int port = serverSocket.getLocalPort();

				// Communication with server
				boolean isUserNameCorrect;
				do {
					//System.out.println("Server communication started");
					Socket s = new Socket(serverURL, 80);
					BufferedReader inToClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
					DataOutputStream outToServer = new DataOutputStream(s.getOutputStream());

					// Register the user who entered in listen mode
					String message = "REGISTER " + username + "@" + InetAddress.getLocalHost().getHostAddress() + ":"
							+ port + "\r\n";
					String msg_out = "POST /userlist.txt HTTP/1.1\r\n" + "Host: " + serverURL + "\r\n"
							+ "content-type: text/html\r\n" + "content-length: " + message.length() + "\r\n"
							+ "Connection: close\r\n\r\n" + message + "\r\n";

					// Send HTTP POST message to server to register
					outToServer.writeBytes(msg_out);

					// Get first line of server message
					String modifiedSentence = inToClient.readLine();

					// Check whether username is correct
					isUserNameCorrect = modifiedSentence.substring(9, 12).equals("200");

					while (modifiedSentence != null) {
						//System.out.println(modifiedSentence);
						modifiedSentence = inToClient.readLine();
					}
					// Take username until it enters a correct one
					Scanner sc = new Scanner(System.in);
					if (!isUserNameCorrect) {
						boolean isUsernameValid;
						do {
							isUsernameValid = true;

							username = sc.nextLine();

							for (int i = 0; i < username.length(); i++) {
								if (username.charAt(i) == ' ' || username.charAt(i) == ':'
										|| username.charAt(i) == '@') {
									isUsernameValid = false;
								}
							}
							if (!isUsernameValid)
								System.out.println("Username should not contain space, '@', ':' charecters,"
										+ "please enter a proper username\n");
						} while (!isUsernameValid);
					}
					// sc.close();
					s.close();
				} while (!isUserNameCorrect); // Stay in the loop until user enters a correct username

				while (true) {
					// Peer to peer connection server side
					DatagramSocket server = new DatagramSocket();
					byte[] receiveData = new byte[1024];
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					serverSocket.receive(receivePacket);

					// Get the actual data in receive data, not empty spaces
					byte[] realData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

					// Turn bytes into string
					String str = new String(realData);
					System.out.println(str);
				}
			} else if (mode.equals("send")) {
				// Communication with server
				// Send HTTP GET and get the userlist.txt
				updateRegistry(uNames, uIPs, uPorts, serverURL);

				// Test if you get list correctly
				for (int i = 0; i < uNames.size(); i++) {
					//System.out.println(uNames.get(i) + " " + uIPs.get(i) + " " + uPorts.get(i));
				}
				//System.out.println("\n");

				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String command = "", response = "";
				while (!command.equals("exit")) {
					command = br.readLine();
					//get the first word: command
					String [] arrOfStr = command.split(" ", 0);
					//System.out.println("Splitted: " + arrOfStr[0] + "\n");
				
					Scanner lineScanner = new Scanner(command);

					if (arrOfStr[0].equals("list")) {
						// Firstly, update the registry
						updateRegistry(uNames, uIPs, uPorts, serverURL);
						System.out.println("The online users are:");
						for (int i = 0; i < uNames.size(); i++)
							System.out.println(uNames.get(i));
					} 
					else if (arrOfStr[0].equals("unicast")) {
						//System.out.println("You entered unicast command");

						// Firstly, update the registry
						updateRegistry(uNames, uIPs, uPorts, serverURL);
						int i = 8;
						while (command.charAt(i) != ' ')
							i++;
						// System.out.println("i: " + i);
						String user_to_send = command.substring(8, i);
						// System.out.println("user_to_send: " + user_to_send);
						while (command.charAt(i) != '"')
							i++;
						int j = i + 1;
						while (command.charAt(j) != '"')
							j++;
						// System.out.println("j: " + j);
						String msg_to_send = command.substring(i + 1, j);
						//msg_to_send = msg_to_send + "\n"; // to get by readLine() from server side
						msg_to_send = username + ": " + msg_to_send;
						String ip_to_send = "";
						String port_to_send = "";
						boolean isInTheList = false;
						for (i = 0; i < uNames.size(); i++) {
							if (uNames.get(i).equals(user_to_send)) {
								//System.out.println("User is in in the list");

								ip_to_send = uIPs.get(i);
								port_to_send = uPorts.get(i);

								//System.out.println("ip_to_send: " + ip_to_send);
								//System.out.println("port_to_send: " + port_to_send);
								isInTheList = true;
							}
						}
						if (!isInTheList) {
							System.out.println("User is not in the list");
						} else {
							// UDP
							DatagramSocket client_socket = new DatagramSocket();
							byte[] sendData = new byte[1024];
							sendData = msg_to_send.getBytes();
							//System.out.println("port_to_send: " + port_to_send);
							DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
									InetAddress.getByName(ip_to_send), Integer.parseInt(port_to_send));
							client_socket.send(sendPacket);
							System.out.println("message sent to " + user_to_send);

						}
					} 
					else if (arrOfStr[0].equals("broadcast")) {
						// Firstly, update the registry
						updateRegistry(uNames, uIPs, uPorts, serverURL);

						int i = 9;
						while (command.charAt(i) != '"')
							i++;
						int j = i + 1;
						while (command.charAt(j) != '"')
							j++;
						String msg_to_send = command.substring(i + 1, j);
						String ip_to_send = "";
						String port_to_send = "";
						//msg_to_send = msg_to_send + "\n"; // to get by readLine() from server side
						msg_to_send = username + ": " + msg_to_send;

						for (i = 0; i < uNames.size(); i++) {
							if (!uNames.get(i).equals(username)) {
								ip_to_send = uIPs.get(i);
								port_to_send = uPorts.get(i);

								DatagramSocket client_socket = new DatagramSocket();
								byte[] sendData = new byte[1024];
								sendData = msg_to_send.getBytes();
								DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
										InetAddress.getByName(ip_to_send), Integer.parseInt(port_to_send));
								client_socket.send(sendPacket);
								System.out.println("message sent to " + uNames.get(i));
							}
						}

					} 
					else if (arrOfStr[0].equals("multicast")) {
						// Firstly, update the registry
						updateRegistry(uNames, uIPs, uPorts, serverURL);

						ArrayList<String> msg_queue = new ArrayList<String>();
						//System.out.println("You entered multicast command");
						int i = 9;
						while (command.charAt(i) != '[')
							i++;
						i = i + 1;
						int j = i;
						for (; command.charAt(j) != ']'; j++) {
							if (command.charAt(j) == ',') {
								String sub_name = command.substring(i, j);
								if (!sub_name.equals(username))
									msg_queue.add(sub_name);
							}
							while (command.charAt(j) == ' ') {
								j++;
								i = j;
							}
						}
						String last_name = command.substring(i, j);
						while (command.charAt(j) != '"')
							j++;
						j = j + 1;
						i = j;
						while (command.charAt(j) != '"')
							j++;

						String msg_to_send = command.substring(i, j);
						//msg_to_send = msg_to_send + "\n";
						msg_to_send = username + ": " + msg_to_send;
						String ip_to_send = "";
						String port_to_send = "";
						msg_queue.add(last_name);

						for (i = 0; i < uNames.size(); i++) {
							boolean userIsFound = false;
							for (j = 0; j < msg_queue.size(); j++) {
								if (uNames.get(i).equals(msg_queue.get(j))) {
									ip_to_send = uIPs.get(i);
									port_to_send = uPorts.get(i);

									// With UDP
									DatagramSocket client_socket = new DatagramSocket();
									byte[] sendData = new byte[1024];
									sendData = msg_to_send.getBytes();
									//System.out.println("port_to_send: " + port_to_send);
									DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
											InetAddress.getByName(ip_to_send), Integer.parseInt(port_to_send));
									client_socket.send(sendPacket);
									userIsFound = true;
								}
							}
							if (userIsFound)
								System.out.println("message sent to " + uNames.get(i));
							else
								System.out.println("user " + uNames.get(i) + " is not found");
						}
					} 
					else {
						if(arrOfStr[0].equals("exit"))
							System.out.println("exited");
						else
							System.out.println("Please enter a proper command");
					}
				}
				// s.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateRegistry(ArrayList<String> uNames, ArrayList<String> uIPs, ArrayList<String> uPorts,
			String serverURL) throws IOException {

		// Clear the arraylists first to update the local registry
		uNames.clear();
		uIPs.clear();
		uPorts.clear();

		//System.out.println("send");
		Socket s = new Socket(serverURL, 80);

		BufferedReader inToClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
		DataOutputStream outToServer = new DataOutputStream(s.getOutputStream());

		// HTTP GET to get the userlist.txt
		String msg_out = "GET /userlist.txt HTTP/1.1\r\n" + "Host: " + serverURL + "\r\n"
				+ "content-type: text/html\r\n" + "Connection: close\r\n\r\n";

		// Send HTTP POST message to server to get userlist.txt
		outToServer.writeBytes(msg_out);

		// Get first line of server message
		// System.out.println("here");
		String modifiedSentence = inToClient.readLine();
		// System.out.println(modifiedSentence);

		while (!modifiedSentence.equals("")) {
			modifiedSentence = inToClient.readLine();
			// System.out.println(modifiedSentence);
		}
		ArrayList<String> arr_test = new ArrayList<String>();

		modifiedSentence = inToClient.readLine();

		while (modifiedSentence != null) {
			if (!modifiedSentence.equals("")) {
				boolean flag1 = true;
				boolean flag2 = true;
				boolean flag3 = true;
				// uNames
				for (int i = 0, j = 0, k = 0; i < modifiedSentence.length(); i++) {
					if (modifiedSentence.charAt(i) == '@' && flag1) {
						flag1 = false;
						String name = modifiedSentence.substring(0, i);
						uNames.add(name);
						// System.out.println("Name:" + name);
						j = i + 1;
					}
					if (modifiedSentence.charAt(i) == ':' && flag2) {
						flag2 = false;
						String ip = modifiedSentence.substring(j, i);
						uIPs.add(ip);
						// System.out.println("IP:" + ip);
						k = i + 1;
					}
					if (i == (modifiedSentence.length() - 1) && flag3) {
						flag3 = false;
						String port = modifiedSentence.substring(k, i + 1);
						uPorts.add(port);
						// System.out.println("Port:" + port);
					}
				}

			}
			// System.out.println(modifiedSentence);
			modifiedSentence = inToClient.readLine();
		}
		// System.out.println(modifiedSentence);
		// System.out.println("ended");
		// System.out.println("\n");
	}
}