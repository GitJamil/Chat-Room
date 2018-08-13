import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 * ****************************
 * Filename: Server.java
 * Student Name: Jamil Triaa
 * University ID: 201182053
 * Departmental ID: u6jt
 * ****************************
 */

 /**
  * Additional functionality:
	*
	* 1. Allows a client to block/unblock messages from another client.
	*
	* 2. Allows a client to send a private message to another client. Otherwise
	* messages are invisible to other clients.
	*
	* 3. Allows a client to clear their screen by using command '-cls'.
	*
	* 4. Allows a client to become an Administrator by entering a password.
	*
	* 5. Allows an Administrator to kick a client out of the chat room.
	*
	* 6. Allows an Administrator to shut down the server.
	*/

 /**
  * This class defines a Server that can:
  * <ul>
  * <li>Listen for new connections from clients.</li>
  * <li>Handle connections from multiple clients.</li>
  * <li>Respond to client requests.</li>
  * <li>Broadcast chat message to all clients.</li>
  * </ul>
	*
	* @author Jamil Triaa
	*
  */
	public class Server {
		/* The initial values should be the same in the server program. */

 	 /**
  	* Number of the port which the server's socket will connect to
  	*/
		private int portNumber = 2111;

 	 /**
  	* Message prompting client to enter a username
  	*/
		private String welcome = " Please enter your username.";

 	 /**
    * Message validating the username
 	  */
		private String accepted = " Your username has been accepted.";

 	 /**
  	* The IP Address of the server.
  	*/
		private static String serverIP;

 	 /**
  	* The time (in milliseconds) when the server begins running.
		*/
		private static long serverStartTime = System.currentTimeMillis();

 	 /**
  	* socket: The server socket
		*/
		private ServerSocket ss; // For the method "shutDown"

	 /**
    * <p>
    * The Map storing each user's name(key) and its corresponding
    * PrintWriter(value).
    * </p>
    * In the multi-threading environment, A CurrentHashMap can perform more
    * safely and efficiently than a HashMap.
    */
    private static ConcurrentHashMap<String, PrintWriter> clients;

	 /**
    * The block list in which each user (name) is a Key while each Value is the
    * set of users (name) that have been blocked by this user.
    */
    private static ConcurrentHashMap<String, HashSet<String>> blockList;

	 /**
    * <P>
    * The Administrator Password.
    * </P>
    * For the sake of security, every time the server restarts, it will
    * generate a new random 4-digit password on its screen.
    */
    private static int adminPassword;

   /**
    * The set of names of Administrators.
    */
    private static HashSet<String> admins;

	 /**
 		* Gets the current time in a specific format.
 		*
 		* @return the current time in the format of [HH:mm:ss]
 		*/
		public static String getCurrentTime() {
			// Convert the current time into the format of [HH:mm:ss]
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			String currentTime = sdf.format(System.currentTimeMillis());
			return "[" + currentTime + "] ";
		}


   /**
  	* Main Method: start the server.
		* @param args
		*						command line arguments
		* @throws IOException
		*						may occur if local host name cannot be resolved
		*						into an address.
		*/
		public static void main (String[] args) throws IOException {

	 	 /*
	   	* Instantiates a new instance of the server class
		 	*/
			Server server = new Server();

	 	 /*
	  	* Finds out the IP Address of the server. Assumes that it will not
	  	* change during the server is running.
	  	*/
			serverIP = InetAddress.getLocalHost().getHostAddress();

		 /*
	 		* Creates a new ConcurrentHashMap to store user names and their
	 		* PrintWriters.
	 		*/
			clients = new ConcurrentHashMap<String, PrintWriter>();

		 /*
	 		* Creates a new ConcurrentHashMap to store user names and their block
	 		* lists.
	 		*/
			blockList = new ConcurrentHashMap<String, HashSet<String>>();

		 /*
			*Creates a new HashSet to store the names of Administrators.
			*/
			admins = new HashSet<String>();

		 /*
	 		* For the sake of security, every time the server restarts, it will
	 		* generate a new 4-digit Administrator Password randomly and display it
	 		* on its screen.
	 		*/
			adminPassword = (int) ((Math.random() * 9000) + 1000);

	 	 /*
	  	* Calls start() method
	  	*/
			server.start();
		}

 	 	 /**
  		* Starts the server.
			*
			* @throws IOException
			*						may occur if local host name cannot be resolved
			*						into an address.
			*/
			private void start() throws IOException {
				ss = new ServerSocket(portNumber);

				// Shows server information
				System.out.println(" Server running! Hosted at "
					+ serverIP + " and waiting for connections...");
				System.out.println(" Administrator password: " + adminPassword);

					Socket socket;
					Thread thread;
					try {
						while (true) {
							socket = ss.accept();
							thread = new Thread(new HandleSession(socket));
							thread.start();
						}
					}	catch (Exception e)  {
					 /*
	     			* The socket may be closed by shutDown() method so we do not need
	     			* to handle the exception caused by the close of socket, but we do
	     			* need to handle other I/O exceptions.
	     			*/
						if (!ss.isClosed())	{
							System.out.println(e.getMessage());
							System.exit(1);
						}
					}
				}

 /**
  * <p>
	* HandleSession receives messages or commands from a client and make
  * responses.
	* </p>
	* <p>
  * Before a client can enter the chat room, he/she will be required to enter
  * a user name. After HandleSession validates the name, a feedback will
  * be given to the client and he/she will be allowed to chat with others.
	* </p>
	*/
	public class HandleSession implements Runnable {

	 /**
	  * The client socket.
	  */
		private Socket socket; // For one client

	 /**
	  * The name of the client.
	  */
		private String name; // For the current client

	 /**
	  * Reads text from InputStream.
	  */
		private BufferedReader in = null;

	 /**
	  * Prints text to OutputStream.
	  */
		private PrintWriter out;

	 /**
	  * The time (in milliseconds) at which the client enters the chat room.
	  */
		private long clientStartTime = System.currentTimeMillis();

	 /**
	 	* Has the client decided to finish the chat?
	 	*/
		private boolean finished = false;


	 /**
	  * Creates a new instance of HandleSession.
	  *
	  * @param socket
	  *            the client socket
	  */
		private HandleSession (Socket socket) {
			this.socket = socket;
		}

	 /**
	  * Asks the client to enter a valid name. <br>
	  * Broadcasts chat messages to all clients. <br>
	  * Responds to client requests.<br>
	  *
	  * @see java.lang.Runnable#run()
	  */
	  @Override
		public void run() {
			try {
				createStreams();
				getClientUserName();
				while (!finished)	{
					listenForClientMessages();
				}
			}
			catch (IOException e) {
				System.out.println(e);
			}
			finally {
				closeConnection();
			}
		} // End of run() in the class HandleSession

	 /**
	  * Detects a user connecting to the server.
		*/
		private void createStreams() {
			try {
				in = new BufferedReader(new
					InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(new
					OutputStreamWriter(socket.getOutputStream()));
				System.out.println(" Connection detected!");
			}
			catch (IOException e) {
				System.err.println(" Exception in createStreams(): " + e);
			}
		} // End of createStreams() in the class HandleSession

	 /**
	  * Asks for and retrieves a valid username from the client.
		*/
		private void getClientUserName() {
			while (true) {
				out.println(welcome); out.flush();
				try { name = in.readLine(); }
				catch (IOException e) {
					System.err.println(" Exception in getClientUserName: " + e);
				}
				if (name == null) return;
				if (name.length() > 0 && !clients.containsKey(name))  {

					/*
					* Puts the client's PrintWriter into the HashMap,
					* using his/her name as the key
					*/
					clients.putIfAbsent(name, out);

					/*
				 	* Creates an empty block list(HashSet) for the new
				 	* client
				 	*/
					blockList.putIfAbsent(name, new HashSet<String>());

					break;
				}
				out.println(" Sorry, this username is already being used.");
				out.flush();
			}
			out.println(accepted + " Please type messages.");
			out.flush(); // Otherwise the client may not see the message

			// Sends notification to the other clients
			broadcast(name + " has entered the chat room!", "Server");
		}	// End of getClientUserName() in the class HandleSession

	 /**
	  * Sends messages to the server until the client
		* leaves the server by entering '-quit' or disconnecting
		* abruptly.
		*
	  * @throws IOException
		*
		*/
		private void listenForClientMessages() throws IOException {
			String line; // Input from a remote client
			while (!finished) {
				line = in.readLine();
				if (line == null)	{
					finished = true;
				}
				else if (line.length() == 0)	{
					out.println(" You are not allowed to send an empty message.");
					out.flush();
				}
				else if (line.startsWith("-")) {
					processClientRequest(line);
				}
				else broadcast(line, name);
			}
		} // End of listenForClientMessages() in the class HandleSession

	 /**
	  * <p>
		* Broadcasts a message to all the other clients.
		* </p>
		* There are two kinds of broadcasts, one is the server's broadcast and
	  * another is the client's broadcast. <br>
		*
		* @param content
		*						 the content of the broadcast.
		* @param sender
								 "Server" or client.
		*/
		private void broadcast (String content, String sender)	{
			String message = "";

			// If the sender is the server
		 	if (sender.equals("Server")) {
	 			message = "[Server] " + content;
	 			for (PrintWriter writer : clients.values()) {
					if (!writer.equals(out)) {
		 			// Sends messages to all other clients
		 			writer.println(message);
		 			writer.flush();
			 		}
	 			}
	 			// Prints out events on server's screen
	 			System.out.println(getCurrentTime() + content);
		 	}	else	{
				// If the sender is a client
				for (Map.Entry<String, PrintWriter> entry : clients.entrySet()){
		    	String receiver = entry.getKey();
		    	// Check whether the sender is blocked by the receiver
		    	if (!blockList.get(receiver).contains(sender)){
						if (!receiver.equals(sender)){
			    	// Message sent to others
			    	message = " " + getCurrentTime() + sender + ": " + content;
					} else {
			    	// Feedback given to the sender
			    	message = " " + getCurrentTime() + sender + "(You): " + content;
					}
					PrintWriter writer = entry.getValue();
					writer.println(message);
					writer.flush();
		    }
		    // If the sender is blocked by this receiver, do not send to him/her
			}
		}
	}

	 /**
	 * Processes the request commands and calls the corresponding methods to
	 * deal with them.
	 *
	 * @param command
	 *            the whole command, starting with "- "
	 */
		private void processClientRequest (String command) {

			// Complex commands that require parameters
			if (command.startsWith("-private "))	{
				// The client wants to private message someone
				privateMsg(command);

			} else if (command.startsWith("-block "))	{
					// The client wants to block someone
					block(command);

			} else if (command.startsWith("-unblock "))	{
					// The client wants to unblock someone
					unBlock(command);

			} else if (command.startsWith("-admin ")) {
					// The client wants Admin rights
					verifyAdmin(command);

	    } else if (command.startsWith("-kick ")) {
					// The client wants to kick someone out of the chat room
					kick(command);

			}	else	{

				// Simple commands that need no parameters
				switch (command)	{

					// The client asks for the list of commands
		    	case "-help":	{
						showHelp();
						break;
					}

					// The client asks how long the server has been running for
					case "-serverTime":	{
						showSRun();
						break;
					}

					// The client asks how long they've been in the chat room
					case "-clientTime":	{
						showCRun();
						break;
					}

					// The client asks for the server's IP address
					case "-address":	{
						showIP();
						break;
					}

					// The client asks how many users are connected to the chat room
					case "-clientNo":	{
						showClientNo();
						break;
					}

					// The client wants to clear their screen
					case "-cls":	{
						clearScreen();
						break;
					}

					// The client wants to leave the chat room
					case "-quit":	{
						finished = true;
						break;
					}

					// The client wants to shut down the server
					case "-shutdown": {
		    		shutDown();
		    		break;
					}

					// Does not match any command
					default: {
						out.println(" " + command + " is not a recognised command.");
						out.println(" To see a list of the available commands, type '-help'.");
						out.flush();
					}
				}
			}
			return;
		}

	 /**
	  * Shows all the request commands a client can send to the server.
		* <br>
		* If the client is an Administrator, they'll have two extra commands.
		*/
		public void showHelp() {
			// Formats of each command is on the left hand side.
			out.println(" -------------------------------------------------------------------------");
			out.println(" List of commands:");
			out.println(" -help | Displays a list of commands");
			out.println(" -serverTime | Displays how long the server has been running for");
			out.println(" -clientTime | Displays how long you have been in the chat room");
			out.println(" -address | Displays the server's IP address");
			out.println(" -clientNo | Displays the number of people in the chat room");
			out.println(" -private username: message | Send a private message to another user");
			out.println(" -block username | Block all messages from other user");
			out.println(" -unblock username | Unblock messages from other user");
			out.println(" -cls | Clears the screen");
			out.println(" -quit | Leave the chat room");
			// If the client is an administrator, he/she has two extra commands
		 if (admins.contains(name)) {
	 	 		out.println(" -kick username | Kick a user out of the chat room");
	 			out.println(" -shutdown |shut down the server.");
		 } else {
	 	 		// If not, he can become an administrator with the command "-admin password"
	 			out.println(" -admin password | Enter the password to become an Administrator");
		 }
		 		out.println(" -------------------------------------------------------------------------");
	    	out.flush();
		}

	 /**
	  * Calculates how long the server has been running.
		*
		* @return serverRunTime
		*									The amount of time the server has been
		*									running for.
		*/
		public long calculateSRun()	{
			long currentTime = System.currentTimeMillis(); // Current time in milliseconds
			long serverRunTime = (((currentTime - serverStartTime) / 1000) / 60); // Server run time in minutes
			return serverRunTime;
		}

	 /**
	  * Shows the amount of time the server has been running for.
		*/
		public void showSRun()	{
			out.println(" The server has been running for " + calculateSRun() + " minute(s).");
			out.flush();
		}

	 /**
	  * Calculates how long the client has been in the chat room.
		*
		* @return clientRunTime
		*									The amount of time that the client has been
		*									in the chat.
		*/
		public long calculateCRun()	{
			long currentTime = System.currentTimeMillis(); // Current time in milliseconds
			long clientRunTime = (((currentTime - clientStartTime) / 1000 ) / 60); // Client session time in minutes
			return clientRunTime;
		}

	 /**
	  * Shows the amount of time the client has been in the chat room for.
		*/
		public void showCRun()	{
			out.println(" You have been in the chat room for " + calculateCRun() + " minute(s).");
			out.flush();
		}

	 /**
	  * Shows the server's IP address
		*/
		public void showIP() {
	    out.println(" The server's IP Address is " + serverIP);
			out.flush();
		}

	 /**
		* Shoes the number of people who are currently connected to
		* the chat room.
		*/
		public void showClientNo() {
			if (clients.size() == 1)	{
				out.println(" There is 1 person currently in the chat room.");
			}
			else	{
	    	out.println(" There are " + clients.size() + " people currently in the chat room.");
			}
			out.flush();
		}

 /**
	* <p>
	* Sends a private message to another client.
	* </p>
	* <p>
	* The message is only visible to the receiver and the sender.
	* </p>
	*
	* @param command
  *            the whole command, "-private name: message"
	*/
	public void privateMsg(String command) {
	    try {
				// Gets the name of the target receiver
				String receiver = command.substring(9,command.indexOf(":"));
				// Gets the message to be sent
				String message = command.substring(command.indexOf(":")+1).trim();
				if (!blockList.containsKey(receiver)) {
		    	// The target client does not exist
		    	out.println(" Failed. Cannot find a user named " + receiver + ".");
					out.flush();
				} else if (receiver.equals(name)) {
		    	// Cannot send a private message to oneself
		    	out.println(" You cannot send a private message to yourself!");
					out.flush();
				} else {
		    	if (blockList.get(receiver).contains(name)) {
						// If the client has been blocked by the target receiver
						out.println(" Failed. You have been blocked by " + receiver + ".");
						out.flush();
		    	} else {
						// Creates a private message with a fixed format
						String finalMsg = " " + getCurrentTime() + name + ": " + message + " [Private Message]";
						// Sends it to the target receiver
						clients.get(receiver).println(finalMsg);
						clients.get(receiver).flush();
						// Gives a feedback to the client(sender)
						out.println(" You've sent a private message to " + receiver + ".");
						out.flush();
		    	}
				}
	    	} catch (Exception e) {
					// If the command is not in the correct format
					out.println(" Failed. Invalid format.");
					out.println(" Valid Format: '-private name: message'.");
					out.flush();
	    }
	}

	 /**
	 	* <p>
	 	* Blocks all the messages from another client with his/her name.
	 	* </p>
	 	* The client can also {@linkplain #unBlock(String) unblock} another
	 	* client.
	 	*
	 	* @param command
	 	*            the whole command, '-block name'
	 	*/
		public void block(String command) {

	    // Gets the name which the client wants to block
	    String blockName = command.substring(7);

	    if (blockName.equals(name)) {
				// Cannot not block oneself
				out.println(" You cannot block yourself.");
				out.flush();
				return;
	    }

	    if (clients.containsKey(blockName)) {
				// Adds the name to the current client's block list
				blockList.get(name).add(blockName);
				out.println(" You will no longer receive messages from " + blockName + ".");
				out.flush();
	    } else {
					// If the target client does not exist
					out.println(" Failed. Cannot find a user named " + blockName + ".");
					out.flush();
	    }
		}

		/**
 		 * Unblocks another client (receive messages from him/her again).
 	 	 *
 	 	 * @param command
 	 	 *            the whole command, '-unblock name'
 	 	 */
		 public void unBlock(String command) {

			 // Gets the name which the client wants to unblock
			 String unBlockName = command.substring(9);

			 if (clients.containsKey(unBlockName)) {
				 // Removes the name from the current client's block list
				 blockList.get(name).remove(unBlockName);
				 out.println(" You will now receive messages from " + unBlockName + ".");
				 out.flush();
			 } else {
				 // If the target client does not exist
				 out.println(" Failed. Cannot find a user named " + unBlockName + ".");
				 out.flush();
			 }
			}

		 /**
	  	* Clears the client's screen by printing out 50 empty lines.
	  	*/
			public void clearScreen() {
	    	for (int i = 0; i < 50; i++) out.println();
	    	out.flush();
			}

		 /**
	 		* Verifies the password entered by the client.
	 		*
	 		* @param command
	 		*            the whole command, '-admin password'
	 		*/
			public void verifyAdmin(String command) {
	    	if (command.equals("-admin " + adminPassword)) {
					// Password matches
					admins.add(name);
					out.println(" ---------------------------------------------");
					out.println(" You are now an Administrator. Congratulations!");
					out.println(" Enter '-help' to see your extra commands.");
					out.println(" ---------------------------------------------");
					out.flush();
					// Sends notifications to other clients
					broadcast(name + " has become an Administrator!", "Server");
	    	} else {
					// The given password is incorrect
					out.println(" Oops! Incorrect password. Try again.");
					out.flush();
	    	}
			}

			/**
	 		* <p>
	 		* Kicks a user out of the chat room, which is one of the privileges of
	 		* Administrator.
	 		* </p>
	 		* <p>
	 		* To use it, the client needs to first {@linkplain #verifyAdmin(String)
	 		* become an Administrator}.
	 		* </p>
	 		*
	 		* @param command
	 		*            the whole command, '-kick name'
	 		*/
			public void kick(String command) {

	    	// The name to be kicked is after "-kick "
	    	String kickedUser = command.substring(6);

	    	// Checks if the client is an Administrator
	    	if (admins.contains(name)) {

					// Only the Administrator has the right to kick others
					PrintWriter target = clients.get(kickedUser);
					if (target == null) {

		    	// Targetted client does not exist
		    	out.println(" Failed. Cannot find a user named " + kickedUser + ".");
					out.flush();
				} else {

		    		// Cannot kick another administrator
		    		if (admins.contains(kickedUser)) {
							out.println(" Failed. You cannot kick out another Administrator.");
							out.flush();
		    		} else {

								/*
			 					 * Sends a signal to the targetted client then
			 				 	 */
								 target.println("[Server] [Kicked Out]");
								 target.flush();
								 broadcast(kickedUser + " has been kicked out of the chat room by " + name, "Server");
		    	 	}
				 	}
	    	} else {
						// The client is not an Administrator
						out.println(" Sorry, you can't use this command as you're not an Administrator.");
						out.flush();
	    	}
			}

	 	 /**
	   	* Finishes the client's connection to the server.
		 	*/
			public void closeConnection() {
				if (name != null) {
					// Removes the client from the chat
					clients.remove(name, out);

					// Removes his/her block list
		    	blockList.remove(name);

					// If user is an admin, removes them from the Administrator list
					admins.remove(name);

					// Sends notifications to other clients
					broadcast(name + " has left the chat room.", "Server");
				}
				try	{
					in.close();
					if (out != null) {
						out.flush();
						out.close();
					}
					if (socket != null)	{
						socket.close();
					}
				}	catch (IOException e) {
						System.err.println(" Closing: " + e.getMessage());
					}
				} // End of closeConnection() in the class HandleSession

				/**
				 * Shuts down the server, which is a privilege of the Administrator.<br>
				 * <p>
				 * To use it, the client needs to first {@linkplain #verifyAdmin(String)
				 * become an Administrator}.
				 * </p>
				 */
				 public synchronized void shutDown() {

					// Checks if the client is an Administrator
					if (admins.contains(name))	{
						try {

							// Sends notification to the other clients in the chat room
							broadcast(" Uh-oh! The server has been shut down by " + name, "Server");

							// Sends a feedback message to the client who made the request
							out.println(" You have shut down the server."); out.flush();
							ss.close();

						}	catch (Exception e) {
								System.err.println(" There is a problem shutting down the server: " + e.getMessage());
						} finally {
								System.exit(0);
						}
					} else	{

							// If not an admin, abort the action
							out.println(" Sorry, you cannot use this command since you are not an admin.");
							out.flush();
					}
				}


			} // End of the class HandleSession

		} // End of the class Server
