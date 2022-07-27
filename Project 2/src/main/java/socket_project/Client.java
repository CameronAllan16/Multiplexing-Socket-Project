package socket_project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
//import java.util.Random;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {

    //size of the reply codes from the server
    //(reply code indicates whether the client request was accepted or rejected by server)
    private final static int SERVER_CODE_LENGTH = 1;
    private final static int FILE_SIZE = 2000;

    public static void main(String[] args) throws IOException{

        if (args.length != 2) {
            System.err.println("Usage: java socket_project.Client <server_IP> <server_port>");
            System.exit(0);
        }

        int serverPort = Integer.parseUnsignedInt(args[1]);
        String serverAddr = args[0];

        String command;
        do{
            Scanner keyboard = new Scanner(System.in);
            System.out.println("enter a command (D, G, L, R, or Q):");
            //Commands are NOT case-sensitive.
            command = keyboard.next().toUpperCase();
            //This is to read/clear the new-line character:
            keyboard.nextLine();


            switch (command){
                case "L":
                    //List all files (ignoring directories) in the server directory
                    //(file name : file size)
                    ByteBuffer buffer = ByteBuffer.wrap("L".getBytes());
                    SocketChannel channel = SocketChannel.open();
                    channel.connect(new InetSocketAddress(serverAddr, serverPort));
                    //System.out.println("TCP connection established.");

                    //The random sleep is for testing purpose only!
                    // try {
                    //    Thread.sleep(new Random().nextInt(20000));
                    // }catch(InterruptedException e){;}

                    //read from the buffer into the channel
                    channel.write(buffer);

                    //before writing to buffer, clear buffer
                    //("position" set to zero, "limit" set to "capacity")
                    //buffer.clear();

                    int bytesRead;
                    if (serverCode(channel).equals("F")){
                        System.out.println("Server rejected the request.");
                    }else {
                        ByteBuffer data = ByteBuffer.allocate(1024);
                        //read() will return -1 if the server has closed the TCP connection
                        // (when server has done sending)
                        while( (bytesRead = channel.read(data)) != -1) {
                            //before reading from buffer, flip buffer
                            //("limit" set to current position, "position" set to zero)
                            data.flip();
                            byte[] a = new byte[bytesRead];
                            //copy bytes from buffer to array
                            //(all bytes between "position" and "limit" are copied)
                            data.get(a);
                            String serverMessage = new String(a);
                            System.out.println(serverMessage);
                        }
                    }
                   // channel.close();
                    break;

                case "D":
                    //Delete a file
                    //Ask the user for the file name
                    //Notify the user whether the operation is successful
                    System.out.println("Enter File name that you wish to delete:");
                    String fileName = keyboard.nextLine();

                    channel = SocketChannel.open();
                    channel.connect(new InetSocketAddress(serverAddr, serverPort));

                    buffer = ByteBuffer.wrap(("D"+fileName).getBytes());

                    //Send the bytes to the server
                    channel.write(buffer);

                    //shutdown the channel for writing
                    channel.shutdownOutput();

                    //receive server reply code
                    if (serverCode(channel).equals("S")){


                        System.out.println("The request was accepted by the server.");
                    } else{
                        System.out.println("The request was rejected");
                    }
                    channel.close();


                    break;

                case "G":
                    //Get a file from the server
                    //Ask the user for the file name
                    //Notify the user whether the operation is successful
                    System.out.println("Enter the File Name you wish to download:");
                    String fName = keyboard.nextLine();

                    channel = SocketChannel.open();
                    channel.connect(new InetSocketAddress(serverAddr, serverPort));

                    buffer = ByteBuffer.wrap(("G"+fName).getBytes());

                    //send bytes to the server
                    channel.write(buffer);
                    //shutdown the channel for writing
                    channel.shutdownOutput();
                    //receive server reply code
                    if (serverCode(channel).equals("S")){
                        System.out.println("The request was accepted by the server");
                        byte [] mybytearray = new byte[FILE_SIZE];
                        InputStream is = channel.get
                    } else{
                        System.out.println("The request was rejected");
                    }
                    channel.close();

                    break;

                case "R":
                    //Rename a file
                    //Ask the user for the original file name
                    //and the new file name.
                    //Notify the user whether the operation is successful.
                    System.out.println("Enter the original name of the file you would like to rename:");
                    String fNameBefore = keyboard.nextLine();

                    System.out.println("Enter the name you would like to rename the file to:");
                    String fNameAfter = keyboard.nextLine();

                    channel= SocketChannel.open();
                    channel.connect(new InetSocketAddress(serverAddr, serverPort));

                    buffer = ByteBuffer.wrap(("R"+fNameBefore+fNameAfter).getBytes());

                    channel.write(buffer);

                    channel.shutdownOutput();

                    if (serverCode(channel).equals("S")){
                        System.out.println("The request was accepted by the server");
                    } else {
                        System.out.println("The request was rejected");
                    }
                    channel.close();

                    break;

                default:
                    if (!command.equals("Q")){
                        System.out.println("Unknown command!");
                    }
            }
        }while(!command.equals("Q"));
    }

    private static String serverCode(SocketChannel channel) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(SERVER_CODE_LENGTH);
        int bytesToRead = SERVER_CODE_LENGTH;

        //make sure we read the entire server reply
        while((bytesToRead -= channel.read(buffer)) > 0);

        //before reading from buffer, flip buffer
        buffer.flip();
        byte[] a = new byte[SERVER_CODE_LENGTH];
        //copy bytes from buffer to array
        buffer.get(a);
        String serverReplyCode = new String(a);

        //System.out.println(serverReplyCode);

        return serverReplyCode;
    }
}
