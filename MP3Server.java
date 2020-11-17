import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Project 4
 *
 * A MP3 Server for sending mp3 files over a socket connection.
 *
 * @author Youming Ding, Yuke Han, Section LE2
 *
 * @author Yuke Han, Section LE2
 *
 * @version Apr 9 2019
 */
public class MP3Server {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9478);

        do {
            Socket socket = serverSocket.accept();
            System.out.println("A client is connected"); //////////////////////////////////

            ClientHandler clientHandler = new ClientHandler(socket);
            Thread t = new Thread(clientHandler);
            t.start();

        } while (true);
    }

}


/**
 * Class - ClientHandler
 *
 * This class implements Runnable, and will contain the logic for handling responses and requests to
 * and from a given client. The threads you create in MP3Server will be constructed using instances
 * of this class.
 */
final class ClientHandler implements Runnable {

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public ClientHandler(Socket clientSocket) throws IOException {
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
    }

    /**
     * This method is the start of execution for the thread. See the handout for more details on what
     * to do here.
     */
    public void run() {
        try {
            do {
                SongRequest sr = (SongRequest) inputStream.readObject();

                String str = sr.getArtistName() + " - " + sr.getSongName() + ".mp3";

                if (!sr.isDownloadRequest()) {
                    SongHeaderMessage s = new SongHeaderMessage(false);
                    outputStream.writeObject(s);

                    sendRecordData();
                } else {
                    if (!fileInRecord(str)) {
                        SongHeaderMessage s = new SongHeaderMessage(true,
                                sr.getSongName(), sr.getArtistName(), -1);
                        outputStream.writeObject(s);

                    } else if (fileInRecord(str)) {

                        String name = "songDatabase" + File.separator + str;

                        File f = new File(name);

                        SongHeaderMessage s = new SongHeaderMessage(true,
                                sr.getSongName(), sr.getArtistName(), (int)f.length());
                        outputStream.writeObject(s);

                        byte[] b = readSongData(name);

                        sendByteArray(b);

                    }
                }
                System.out.println("end of server"); /////////////////////////////
            } while (true);
        } catch (Exception e) {
            return;
        }
    }

    /**
     * Searches the record file for the given filename.
     *
     * @param fileName the fileName to search for in the record file
     * @return true if the fileName is present in the record file, false if the fileName is not
     */
    private static boolean fileInRecord(String fileName) {
        try {
            File f = new File("record.txt");
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);

            String s;
            while (true) {
                s = br.readLine();
                if (s != null) {
                    if (s.contains(fileName)) {
                        return true;
                    }
                } else {
                    break;
                }
            }
            br.close();
            fr.close();
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * Read the bytes of a file with the given name into a byte array.
     *
     * @param fileName the name of the file to read
     * @return the byte array containing all bytes of the file, or null if an error occurred
     */
    private static byte[] readSongData(String fileName) {
        try {
            File f = new File(fileName);

            byte[] b = new byte[(int)f.length()];

            FileInputStream fis = new FileInputStream(f);

            fis.read(b);

            fis.close();
            return b;
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Split the given byte array into smaller arrays of size 1000, and send the smaller arrays
     * to the client using SongDataMessages.
     *
     * @param songData the byte array to send to the client
     */
    private void sendByteArray(byte[] songData) throws IOException {
        SongDataMessage sdm = new SongDataMessage(songData);
        int size = sdm.getData().length / 1000;
        int reminder = sdm.getData().length % 1000;
        int count = 0;

        for (int i = 0; i < size; i++) {
            byte[] b = new byte[1000];
            for (int j = 0; j < b.length; j++) {
                b[j] = sdm.getData()[count];
                count++;

            }
            SongDataMessage s = new SongDataMessage(b);
            outputStream.writeObject(s);
        }
        byte[] b = new byte[reminder];
        for (int i = 0; i < reminder; i++) {
            b[i] = sdm.getData()[count];
            count++;
        }
        SongDataMessage s = new SongDataMessage(b);
        outputStream.writeObject(s);
        String str = null;
        outputStream.writeObject(str);
        outputStream.close();
    }

    /**
     * Read ''record.txt'' line by line again, this time formatting each line in a readable
     * format, and sending it to the client. Send a ''null'' value to the client when done, to
     * signal to the client that you've finished sending the record data.
     */
    private void sendRecordData() throws IOException {
        File f = new File("record.txt");
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        String str = null;
        while (true) {
            str = br.readLine();
            if (str == null) {
                break;
            }
            int count1 = 0;
            int count2 = 0;
            for (int i = 0; i < str.length(); i++) {
                if ("-".equals(str.charAt(i) + "")) {
                    count1 = i;
                }
                if (".".equals(str.charAt(i) + "")) {
                    count2 = i;
                }
            }
            String str2 = "\"" + str.substring(count1 + 1, count2).trim() + "\" by: "
                    + str.substring(0, count1).trim();
            outputStream.writeObject(str2);
            outputStream.flush();
        }
        outputStream.writeObject(str);
        outputStream.close();
        br.close();
        fr.close();
    }
}
