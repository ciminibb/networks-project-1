// Project 1
// Ben Cimini


import java.io.* ;
import java.net.* ;
import java.util.* ;


public final class WebServer {
    public static void main(String argv[]) throws Exception {
        int port = 6789;

        // Open <welcomeSocket>, a door that listens for some client.
        ServerSocket welcomeSocket = new ServerSocket(port);

        // Process HTTP requests.
        while (true) {
            // Open a new socket, <connectionSocket>, each time a client is at
            // <welcomeSocket>. A virtual pipe is established.
            Socket connectionSocket = welcomeSocket.accept();

            // Construct a <HttpRequest> object with a reference to
            // <connectionSocket>. It will be used to process an HTTP request.
            HttpRequest request = new HttpRequest(connectionSocket);

            // Create a new thread with a reference to <request> and start it.
            // Execution in main thread begins loop again, then blocks until
            // another HTTP request is received.
            Thread thread = new Thread(request);
            thread.start();
        }
    }
}


final class HttpRequest implements Runnable {
    final static String CRLF = "\r\n"; // Each line of response message must
                                       // terminate with a carriage return and
                                       // line feed.
    Socket socket;                     // Will store a reference to
                                       // <connectionSocket>, which is passed to
                                       // the constructor of this class.

    // Constructor
    public HttpRequest(Socket socket) throws Exception {
        this.socket = socket;
    }

    // Implement <run()> method of the <Runnable> interface, which must return
    // void type to comply.
    public void run() {
        // As another compliance measure, handle exceptions within <run()>. They
        // will be thrown from <processRequest()>.
        try {
            processRequest();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    private void processRequest() throws Exception {
        // Get references to the socket's input and output streams: <is> and
        // <os>. Afterward, wrap filters around <is>.
        InputStream is = this.socket.getInputStream();
        DataOutputStream os =
            new DataOutputStream(this.socket.getOutputStream());
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        // Grab request message from <br> and print to the screen.
        String requestLine = br.readLine();

        System.out.println();
        System.out.println(requestLine);

        // Do the same for header lines with a loop, as the number is unknown.
        // Header lines don't need to be kept, only output.
        String headerLine = null;
        while ((headerLine = br.readLine()).length() != 0) {
            System.out.println(headerLine);
        }

        // Close streams and socket.
        os.close();
        br.close();
        socket.close();
    }
}