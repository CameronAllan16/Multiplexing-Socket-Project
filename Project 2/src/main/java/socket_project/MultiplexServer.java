package socket_project;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class MultiplexServer {

    private static final int CLIENT_CODE_LENGTH = 1;
    private static final int MAX_FILE_NAME_LENGTH = 1024;

    public static void main(String[] args) throws IOException{

        //open a selector
        Selector monitor = Selector.open();

        ServerSocketChannel welcomeChannel = ServerSocketChannel.open();
        welcomeChannel.socket().bind(new InetSocketAddress(2000));

        //configure the serverSocketChannel to be non-blocking
        //(selector only works with non-blocking channels.)
        welcomeChannel.configureBlocking(false);

        //register the channel and event to be monitored
        //this causes a "selection key" object to be created for this channel
        welcomeChannel.register(monitor, SelectionKey.OP_ACCEPT);

        while (true) {
            // select() is a blocking call (so there is NO busy waiting here)
            // It returns only after at least one channel is selected,
            // or the current thread is interrupted
            int readyChannels = monitor.select();

            //select() returns the number of keys, possibly zero
            if (readyChannels == 0) {
                continue;
            }

            // elements in this set are the keys that are ready
            // i.e., a registered event has happened in each of those keys
            Set<SelectionKey> readyKeys = monitor.selectedKeys();

            Iterator<SelectionKey> iterator = readyKeys.iterator();

            while (iterator.hasNext()) {

                SelectionKey key = iterator.next();
                iterator.remove();

                if (key.isAcceptable()) {
                    // OS received a new connection request from some new client
                    ServerSocketChannel wChannel = (ServerSocketChannel) key.channel();
                    SocketChannel serveChannel = wChannel.accept();

                    //create the dedicated socket channel to serve the new client
                    serveChannel.configureBlocking(false);

                    //register the dedicated socket channel for reading
                    serveChannel.register(monitor, SelectionKey.OP_READ);
                }

                else if (key.isReadable()) {
                    //OS received one or more packets from one or more clients
                    SocketChannel serveChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(CLIENT_CODE_LENGTH);
                    int bytesToRead = CLIENT_CODE_LENGTH;

                    //make sure we read the entire server reply
                    while((bytesToRead -= serveChannel.read(buffer)) > 0);

                    byte[] a = new byte[CLIENT_CODE_LENGTH];
                    buffer.flip();
                    buffer.get(a);
                    String request = new String(a);
                    System.out.println("Request from a client: " + request);

                    switch(request){
                        case "L":
                            //send reply code to indicate request was accepted
                            sendReplyCode(serveChannel, "S");

                            File[] filesList = new File(".").listFiles();
                            StringBuilder allFiles = new StringBuilder();
                            if (filesList != null){
                                for (File f : filesList){
                                    //ignore directories
                                    if (!f.isDirectory()) {
                                        allFiles.append(f.getName());
                                        allFiles.append(" : ");
                                        allFiles.append(f.length());
                                        allFiles.append("\n");
                                    }
                                }
                            }

                            ByteBuffer data = ByteBuffer.wrap(allFiles.toString().getBytes());
                            serveChannel.write(data);

                            break;

                        case "D":
                            //Delete file
                            buffer = ByteBuffer.allocate(MAX_FILE_NAME_LENGTH);

                            while((serveChannel.read(buffer)) >=0);

                            buffer.flip();
                            //buffer.remaining() tells the number of bytes in the buffer

                            a = new byte[buffer.remaining()];
                            buffer.get(a);
                            String fileName = new String(a);

                            File f = new File(fileName);
                            if(f.delete()){
                                sendReplyCode(serveChannel, "S");
                            } else {
                                sendReplyCode(serveChannel, "F");
                            }
                            serveChannel.close();
                            break;

                        case "G":
                            //Send file to client
                            buffer = ByteBuffer.allocate(MAX_FILE_NAME_LENGTH);

                            while((serveChannel.read(buffer)) >=0);

                            buffer.flip();

                            a = new byte[buffer.remaining()];
                            buffer.get(a);
                            String fName = new String(a);
                            File file = new File(fName);
                            BufferedInputStream br = new BufferedInputStream(new FileInputStream(file));

                            int bytesRead = 0;
                            int b;

                            if (file.exists() && !file.isDirectory()){
                                sendReplyCode(serveChannel, "S");
                                while((b = br.read()) != -1){
                                    serveChannel.write(ByteBuffer.wrap(fileData));
                                }
                            } else {
                                sendReplyCode(serveChannel, "F");
                            }

                            serveChannel.close();
                            break;

                        case "R":
                            //Rename file
                            buffer = ByteBuffer.allocate(MAX_FILE_NAME_LENGTH);

                            while ((serveChannel.read(buffer)) >=0);

                            buffer.flip();
                            a = new byte[buffer.remaining()];
                            String fName1 = new String(a);
                            File file1 = new File(fName1);
                            buffer.get(a);
                            if (file1.exists() && !file1.isDirectory()){
                                sendReplyCode(serveChannel, "S");

                            } else{
                                sendReplyCode(serveChannel, "F");
                            }
                            serveChannel.close();
                            break;

                        default:
                            System.out.println("Unknown command!");
                            //send reply code to indicate request was rejected.
                            sendReplyCode(serveChannel, "F");
                    }
                    //note that calling close() will automatically
                    // deregister the channel with the selector
                    serveChannel.close();
                }
            }
        }
    }

    private static void sendReplyCode (SocketChannel channel, String code) throws IOException{
        ByteBuffer data = ByteBuffer.wrap(code.getBytes());
        channel.write(data);
    }
}


