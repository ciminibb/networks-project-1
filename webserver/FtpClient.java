// Project 1
// Ben Cimini


package webserver;

import java.io.*;
import java.net.*;
import java.util.regex.*;


/**
 *
 * This is the main FTP client. Study the code and figure out what 
 * each function does before adding to it. You only need to add code 
 * wherever you see a '?'
 *
 * @author Giovani
 * 
 */
public class FtpClient {

    final static String CRLF = "\r\n";
    private boolean DEBUG = false;		// Debug Flag
    private Socket controlSocket = null;
    private BufferedReader controlReader = null;
    private DataOutputStream controlWriter = null;
    private String currentResponse;

    // Constructor is empty!
    public FtpClient() {}

    /*
     * Connect to the FTP server
     * @param username: the username you use to login to your FTP session
     * @param password: the password associated with the username
     */
    public void connect(String username, String password) {
        try {
            // Establish the <controlSocket> using similar syntax as with
            // <welcomeSocket> in the web server. Remember, FTP is out-of-band,
            // so files should be sent over a DIFFERENT port. I'm choosing 21
            // because that's the port of my FTP server! It's also important to
            // note that our socket was initialized above as a data member.
            controlSocket = new Socket("localhost", 21);

            // Get references to input and output streams using the
            // <controlSocket>. Follow client syntax from the book!
            controlReader = new BufferedReader(
                new InputStreamReader(controlSocket.getInputStream()));
            
            controlWriter = new DataOutputStream(
                controlSocket.getOutputStream());

            // Check if the initial connection response code is OK by passing
            // the expected code (220) to <checkResponse()>. 220 says that the
            // service is ready for a new user.
            if (checkResponse(220)) {
                System.out.println("Succesfully connected to FTP server");
            }

            // Send <username> and <password> to server with FTP commands. Code
            // 331 says that the user name is okay but a password is needed;
            // code 230 confirms a successful login.
            sendCommand("USER " + username + CRLF, 331);
            sendCommand("PASS " + password + CRLF, 230);

        } catch (UnknownHostException ex) {
            System.out.println("UnknownHostException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    /*
     * Retrieve the file from FTP server after connection is established
     * @param file_name: the name of the file to retrieve
     */
    public void getFile(String file_name) {
	int data_port = 0; // initialize the data port        
	try {
            // Change to root directory at the server, which is denoted with a
            // backslash. This should succeed, indicated by code 250.
            sendCommand("CWD /" + CRLF, 250);

            // Set to passive mode using FTP command <PASV> which should return
            // code 227 which signals the mode entry. Then use method
            // <extractDataPort> on the response.
            currentResponse = sendCommand("PASV" + CRLF, 227);
            data_port = extractDataPort(currentResponse);

            // Connect to <data_port> by creating a socket and grabbing its
            // input stream. The socket should hit <data_port>, which we just
            // retrieved.
            Socket data_socket = new Socket("localhost", data_port);
            DataInputStream data_reader = new DataInputStream(
                data_socket.getInputStream());

            // Download file from server. A client sends a <RETR> command when
            // it wishes to download a file, it also provides the filename.
            sendCommand("RETR " + file_name + CRLF, 150);

            // Check if the transfer was succesful. Not really sure the use of
            // this step, since we check the response as part of the previous
            // command. Maybe the server sends another message upon successful
            // transfer? Let's check for that.
            checkResponse(226);

            // Write data on a local file
            createLocalFile(data_reader, file_name);

        } catch (UnknownHostException ex) {
            System.out.println("UnknownHostException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    /*
     * Close the FTP connection
     */
    public void disconnect() {
        try {
            controlReader.close();
            controlWriter.close();
            controlSocket.close();
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    /*
     * Send ftp command 
     * @param command: the full command line to send to the ftp server
     * @param expected_code: the expected response code from the ftp server
     * @return the response line from the ftp server after sending the command
     */
    private String sendCommand(String command, int expected_response_code) {
        String response = "";
        try {
            // send command to the ftp server
            controlWriter.writeBytes(command);

            // get response from ftp server
            response = controlReader.readLine();
            if (DEBUG) {
                System.out.println("Current FTP response: " + response);
            }

            // check validity of response  
            if (!response.startsWith(String.valueOf(expected_response_code))) {
                throw new IOException(
                        "Bad response: " + response);
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        return response;
    }

    /*
     * Check the validity of the ftp response, the response code should
     * correspond to the expected response code
     * @param expected_code: the expected ftp response code
     * @return response status: true if successful code
     */
    private boolean checkResponse(int expected_code) {
        boolean response_status = true;
        try {
            currentResponse = controlReader.readLine();
            if (DEBUG) {
                System.out.println("Current FTP response: " + currentResponse);
            }
            if (!currentResponse.startsWith(String.valueOf(expected_code))) {
                response_status = false;
                throw new IOException(
                        "Bad response: " + currentResponse);
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        return response_status;
    }

    /*
     * Given the complete ftp response line of setting data transmission mode
     * to passive, extract the port to be used for data transfer
     * @param response_line: the ftp response line
     * @return the data port number 
     */
    private int extractDataPort(String response_line) {
        int data_port = 0;
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        Matcher matcher = pattern.matcher(response_line);
        String[] str = new String[6];
        if (matcher.find()) {
            str = matcher.group(1).split(",");
        }
        if (DEBUG) {
            System.out.println("Port integers: " + str[4] + "," + str[5]);
        }
        data_port = Integer.valueOf(str[4]) * 256 + Integer.valueOf(str[5]);
        if (DEBUG) {
            System.out.println("Data Port: " + data_port);
        }
        return data_port;
    }

    /*
     * Create the file locally after retreiving data over the FTP data stream.
     * @param dis: the data input stream 
     * @param file_name: the name of the file to create 
     */
    private void createLocalFile(DataInputStream dis, String file_name) {
        byte[] buffer = new byte[1024];
        int bytes = 0;
        try {
            FileOutputStream fos = new FileOutputStream(new File(file_name));
            while ((bytes = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytes);
            }
            dis.close();
            fos.close();
        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException" + ex);
        } catch (IOException ex){
            System.out.println("IOException: " + ex);
        } 
    }
}
