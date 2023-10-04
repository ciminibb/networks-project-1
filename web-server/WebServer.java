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
        System.out.println("REQUEST");
        System.out.println("---------------------------------------------------");
        System.out.println(requestLine);

        // Do the same for header lines with a loop, as the number is unknown.
        // Header lines don't need to be kept, only output.
        String headerLine = null;
        while ((headerLine = br.readLine()).length() != 0) {
            System.out.println(headerLine);
        }

        // Extract <fileName> from <requestLine>. Assume all are GET requests,
        // meaning the method specification can be skipped.
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken();
        String fileName = tokens.nextToken();

        // Ensure file request is within the current directory by prepending "."
        // for local path. The file is now prepared for manipulation.
        fileName = "." + fileName;

        // The first step of sending a file is opening it. A <FileInputStream>
        // object is needed to do so. Note <FileNotFoundException> is caught in
        // order to avoid terminating the thread.
        FileInputStream fis = null;
        boolean fileExists = true;

        try {
            fis = new FileInputStream(fileName);
        }
        catch (FileNotFoundException e) {
            fileExists = false; // There is a special response for this.
        }

        // Construct response message using variables <statusLine>,
        // <contentTypeLine>, and <entityBody>. They represent standard parts of
        // an HTTP response.
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;

        if (fileExists) {
            statusLine = "HTTP/1.0 200 OK" + CRLF; // Don't forget CRLF!
            contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
        }
        else {
            // Respond with "404 Not Found" if the requested file doesn't exist.
            statusLine = "HTTP/1.0 404 Not Found" + CRLF;
            contentTypeLine = "Content-type: text/html" + CRLF; // The response
                                                                // content when
                                                                // file doesn't
                                                                // exist is
                                                                // always HTML
                                                                // text.
            entityBody =
                "<HTML>"
                + "<HEAD><TITLE>Not Found</TITLE></HEAD>"
                + "<BODY>Not Found</BODY></HTML>";
        }

        // Output response message to confirm correctness.
        System.out.println();
        System.out.println("RESPONSE");
        System.out.println("---------------------------------------------------");
        System.out.println(statusLine);
        System.out.println(contentTypeLine);
        
        if (!fileExists) {
            System.out.println("Not Found");
        }

        // Send <statusLine> and <contentTypeLine> to browser by writing to
        // <os>.
        os.writeBytes(statusLine);
        os.writeBytes(contentTypeLine);
        os.writeBytes(CRLF); // Signal end of header lines.

        // Send <entityBody> to browser by the same means. What is sent is,
        // again, contingent on whether the requested file exists.
        if (fileExists) {
            sendBytes(fis, os); // Send the file by <sendBytes> method.
            fis.close();
        }
        else {
            os.writeBytes(entityBody); // Send "404 Not Found."
        }

        // Close streams and socket.
        os.close();
        br.close();
        socket.close();
    }

    // This method writes a file onto the socket's output stream, thereby
    // writing to the requesting client.
    private static void
        sendBytes(FileInputStream fis, OutputStream os) throws Exception {
        byte[] buffer = new byte[1024]; // <buffer> holds bytes on their way.
        int bytes = 0;

        // Copy requested file into the socket's output stream.
        while((bytes = fis.read(buffer)) != -1 ) {
            os.write(buffer, 0, bytes);
        }
    }

    // This method determines a file's MIME type from its name.
    private static String contentType(String fileName) {
        // This server should handle HTML text.
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        }

        // This server should handle JPEG images.
        if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
            return "image/jpeg";
        }

        // This server should handle GIF images.
        if (fileName.endsWith(".gif")) {
            return "image/gif";
        }

        // Many other content types are ignored by this server.
        return "application/octet-stream";
    }
}