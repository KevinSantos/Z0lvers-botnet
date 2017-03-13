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

        // The default port
        int port_number = 4600;
        String host = "localhost";

        String hostname = InetAddress.getLocalHost().getHostName(); // find the hostname from computer
        try {
//            URL whatismyip = new URL("http://checkip.amazonaws.com"); // checkt the external ip from the computer.
//            BufferedReader in = new BufferedReader(new InputStreamReader(
//                    whatismyip.openStream()));
//            ip = in.readLine();  
            ip = "noip"; // had to be changed for video!

        } catch (Exception e) {
            ip = "noip";
        }
        whoami = "Admin@"+ip;

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

                new Thread(new Client()).start();

                while (!closed) {
                    // lees een regel text.
                    int a = 'a';
                    String s = "";
                    long begin = System.currentTimeMillis();
                    do {
                        if (System.currentTimeMillis() - begin > 10000) {
                            begin = System.currentTimeMillis();
                            os.println("ping");
                        }
                        if (inputLine.ready()) {
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
            while ((responseLine = is.readLine()) != null) {
                System.out.println(responseLine);

            }

        } catch (IOException e) {
            System.err.println("IOException:  " + e);

        }

        System.err.println("Connection lost with server");
        closed = true;
    
    }
}
