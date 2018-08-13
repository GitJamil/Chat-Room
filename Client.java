import java.io.*;
import java.net.*;

/*
 * ****************************
 * Filename: Client.java
 * Student Name: Jamil Triaa
 * University ID: 201182053
 * Departmental ID: u6jt
 * ****************************
 */

 /**
  * This class contains a main method that will instantiate a new
  * {@link ClientInstance}.
	*
	* @author Jamil Triaa
  */
	public class Client	{

	 /**
	  * <p>
    * Instantiates a new {@linkplain ClientInstance Client Instance}.
    * </p>
		*
	  * @param args
		*							the supplied command-line arguments as an array of String
		*							objects.
		* @throws Exception
		*							May occur if connection to server is lost suddenly
	  */
		public static void main(String[] args) throws Exception {
			ClientInstance client = new ClientInstance();
			client.start();
		}
	}

/**
 * <p>
 * This class defines a Client Instance that can:
 * <ul>
 * <li>Build connections with a server.</li>
 * <li>Accept user input and handle sending to the server.</li>
 * <li>Handle responses from the server.</li>
 * </ul>
 *
 * @author Jamil Triaa
 *
 */
 class ClientInstance {
 // The initial values should be the same in the server program

 /**
  * Number of the port which the server's socket will connect to
	*/
	private static int portNumber = 2111;

 /**
  * Message prompting client to enter a username
	*/
	private static String welcome = " Please enter your username.";

 /**
  * Message validating the username
	*/
	private static String accepted = " Your username has been accepted.";

 /**
  * The socket used to connect to the server.
  */
	private static Socket socket = null;

 /**
  * The BufferedReader for incoming streams
	*/
	private static BufferedReader in;

 /**
  * The PrintWriter for outgoing createStreams
	*/
	private static PrintWriter out;

 /**
  * A signal indicating that the client has been kicked out of the chat room
	* by Administrator.
  */
  private final static String KICKED_OUT = "[Server] [Kicked Out]";

 /**
  * Whether or not the user is allowed to chat
	*/
	private static boolean isAllowedToChat = false;

 /**
	* Whether the client has finished using the chat.
	*/
	private static volatile boolean finished = false;

 /**
 	* Whether the client is kicked out of the chat room by Administrator.
 	*/
	private static volatile boolean kickedOut = false;

 /**
  * The username
	*/
	private static String clientName;

 /**
	* Calls the methods necessary to establish a connection with the server.
	*/
	public void start() {
		establishConnection();
		handleOutgoingMessages();
		handleIncomingMessages();
	}

 /**
	* Builds a connection with the server and initialises I/O streams.
	*/
	private void establishConnection() {
			String serverAddress = getClientInput(" What is the IP address of the server you wish to connect to?");
			try {
				socket = new Socket(serverAddress, portNumber);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				finished = false;
			}
			catch (IOException e) {
				System.err.println(" Exception in handleConnection(): " + e);
			}
			handleProfileSetUp();
		} // End of handleConnection() in the class ClientInstance

 /**
  * <p>
  * handleProfileSetup handles setting up the client's
	* account on the server.
	* </p>
	* <p>
	* Before the user can chat with others, they need to
	* enter a valid username.
	* </p>
	*/
	private void handleProfileSetUp() {
		String line = null;
		while (! isAllowedToChat) {
			try { line = in.readLine(); }
			catch (IOException e) {
				System.err.println(" Exception in handleProfileSetUp:" + e);
			}
			if (line.startsWith(welcome)) {
				out.println(getClientInput(welcome));
			}
			else if (line.startsWith(accepted) ) {
				isAllowedToChat = true;
				System.out.println(accepted +" Welcome to the chat room! You can send messages here.");
				System.out.println(" If you'd like to see a list of commands, type '-help'.");
				System.out.println(" -----------------------------------------------------------");
			}
			else System.out.println(line);
		}
	}	// End of handleProfileSetUp()	in the class ClientInstance

 /**
  * <p>
	* handleOutgoingMessages handles sending messages to the server.
	* </p>
	*/
	private void handleOutgoingMessages() { // Sender thread
		Thread senderThread = new Thread(new Runnable(){

			@Override
			public void run() {
					while (!finished){
						out.println(getClientInput(null));
					}
			}
		});
		senderThread.start();
	} // End of handleOutgoingMessages() in the class ClientInstance

 /**
  * <p>
	* getClientInput Retrieves the user's input from the keyboard
	* and reads it.
	* </p>
	* @param hint
	* 				User input.
	* @return the message the user has typed out.
	*/
	private String getClientInput (String hint) {
		String message = null;
		try {
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(System.in) );
			if (hint != null) {
				System.out.println(hint);
			}
			message = reader.readLine();
			if (!isAllowedToChat) { clientName = message; }
		}
		catch (IOException e) {
			System.err.println(" Exception in getClientInput(): " + e);
		}
		return message;
	} // End of getClientInput() in the class ClientInstance

 /**
  * <p>
	* handleIncomingMessages handles receiving and printing
	* out incoming messages.
	* </p>
	*/
	private void handleIncomingMessages() { // Listener thread
			Thread listenerThread = new Thread( new Runnable() {

				@Override
				public void run() {
						try {
							while (!finished) {
								String line = in.readLine();
								if (line == null) {
									// Connection lost
									break;
								} else if (line.equals(KICKED_OUT))	{
									// The client has been kicked out of the chat room
									kickedOut = true;
									break;
								} else {
									System.out.println(line);
								}
							}
						}	catch (IOException e) {
								System.err.println(" IOE in handleIncomingMessages()");
							}
						finally	{
							closeConnection();
						}
					}

				});
				listenerThread.start();
			} // end of handleIncomingMessages() in the class ClientInstance

 /**
  * <p>
  * Cuts off the client's connection to the server, closes streams
	* and program exits.
	* </p>
	* {@code closeConnection()} will be called in three cases:
  * <ol>
  * <li>Client wants to disconnect.</li>
	* <li>Server disconnects abruptly.</li>
  * <li>Client is kicked out of the chat room by Administrator.</li>
  * </ol>
  * <p>
  * Exceptions are going to be handled here and reasons for
  * why the Client program is going to exit will be printed out.
  * </p>
	*/
	private static synchronized void closeConnection() {

		System.out.println(" ----------------------------------------------------------");

		// Case 1: Client wants to disconnect
		if (finished)
	    System.out.println(" You have left the chat room.");


		// Case 2: Client disconnects abruptly
		else if (!finished && !kickedOut)
			System.out.println(" Connection to the server has been lost!");


		// Case 3: Client is kicked out of the chat room by Administrator
		if (!finished && kickedOut)
	    System.out.println(" You have been kicked out of the chat room by an administrator.");


		try {
			out.close();
			in.close();
			if (socket!= null) socket.close(); // Finish the client program
		}		catch (IOException e) {
				System.err.println(" Exception when closing the socket");
				System.err.println(e.getMessage());
			} finally {

					// Program exits
					System.exit(0);
				}
	} // End of closeConnection() in the class ClientInstance

} // End of the class ClientInstance
