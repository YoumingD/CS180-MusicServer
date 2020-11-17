import java.io.*;
import java.net.Socket;
import java.util.Scanner;
/**
 * Project 4
 *
 * An MP3 Client to request .mp3 files from a server and receive them over the socket connection.
 *
 * @author Youming Ding, Section LE2
 *
 * @author Yuke Han, Section LE2
 *
 * @version Apr 9 2019
 */
public class MP3Client {

    public static void main(String[] args) throws IOException {
        String str;

        Scanner scanner = new Scanner(System.in);
        Socket socket;
        ObjectOutputStream oos;

        do {
            socket = new Socket("localhost", 9478);
            oos = new ObjectOutputStream(socket.getOutputStream());

            do {
                System.out.println("Do you want to see the list of songs or download a song? (list/download/exit)");

                str = scanner.nextLine();

                if (str.equals("list")) {
                    SongRequest sr = new SongRequest(false);

                    oos.writeObject(sr);

//                    oos.flush();

                } else if (str.equals("download")) {
                    System.out.println("Enter the song's name:");
                    String songName = scanner.nextLine();
                    System.out.println("Enter the artist's name:");
                    String artistName = scanner.nextLine();

                    SongRequest sr = new SongRequest(true, songName, artistName);

                    oos.writeObject(sr);

//                    oos.flush();
                } else if (str.equals("exit")) {
                    break;
                }
            } while (!str.equals("list") && !str.equals("download"));

            if (!str.equals("exit")) {
                ResponseListener rl = new ResponseListener(socket);
                Thread t = new Thread(rl);
                t.start();
                try {
                    t.join();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println();
                oos.close();
                socket.close();
            }
        } while (!str.equals("exit"));
    }
}


/**
 * This class implements Runnable, and will contain the logic for listening for
 * server responses. The threads you create in MP3Server will be constructed using
 * instances of this class.
 */
final class ResponseListener implements Runnable {

    private ObjectInputStream ois;

    public ResponseListener(Socket clientSocket) throws IOException {
        ois = new ObjectInputStream(clientSocket.getInputStream());
    }

    /**
     * Listens for a response from the server.
     * <p>
     * Continuously tries to read a SongHeaderMessage. Gets the artist name, song name, and file size from that header,
     * and if the file size is not -1, that means the file exists. If the file does exist, the method then subsequently
     * waits for a series of SongDataMessages, takes the byte data from those data messages and writes it into a
     * properly named file.
     */
    public void run() {
        try {
            Object object;
            do {
                object = ois.readObject();
                if (object instanceof SongHeaderMessage) {
                    SongHeaderMessage shm = (SongHeaderMessage) object;
                    if (!shm.isSongHeader()) {
                        while (true) {
                            String str = (String) ois.readObject();
                            if (str == null) {
                                break;
                            }
                            System.out.println(str);

                        }
                    } else {
                        if (shm.getFileSize() == -1) {
                            System.out.println("file doesn't exit");
                        } else {
                            String str = "savedSongs" + File.separator + shm.getArtistName()
                                    + " - " + shm.getSongName() + ".mp3";
                            File f = new File(str);
                            if (!f.exists()) {
                                f.getParentFile().mkdirs();
                            }

                            FileOutputStream fos = new FileOutputStream(f);

                            Object o;
                            while (true) {

                                o = ois.readObject();

                                if (o == null) {
                                    break;
                                }

                                if (o instanceof SongDataMessage) {
                                    SongDataMessage s = (SongDataMessage) o;
                                    fos.write(s.getData());
                                    fos.flush();
                                }
                            }
                            fos.close();
                        }
                    }
                }
                break;
            } while (object == null || !(object instanceof SongHeaderMessage));
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the given array of bytes to a file whose name is given by the fileName argument.
     *
     * @param songBytes the byte array to be written
     * @param fileName  the name of the file to which the bytes will be written
     */
    private void writeByteArrayToFile(byte[] songBytes, String fileName) throws IOException {
        File f = new File(fileName);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(songBytes);

        fos.close();
    }
}
