import webserver.FtpClient;

public class FtpTest {
    public static void main(String argv[]) {
        // Create instance of <FtpClient>.
        FtpClient myClient = new FtpClient();

        // Connect to the server.
        myClient.connect("ciminibb", "cs4065$$GIO");

        // Retrieve file from the server.
        myClient.getFile("ftp_test.txt");

        // Disconnect from the server.
        myClient.disconnect();
    }
}
