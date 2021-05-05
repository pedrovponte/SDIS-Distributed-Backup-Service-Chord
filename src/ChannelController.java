import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;

public class ChannelController implements Runnable {
    private int port;
    private Peer peer;
    private static SSLServerSocket serverSocket;


    public ChannelController(int port, Peer peer) {
        try{
            this.port = port;
            this.peer = peer;
        } catch(UnknownHostException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } 
    }

    public void start() {
       
        // SSLSocket sslSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, port);

        // // open streams
        // PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);
        // BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

        // SSLContext context = SSLContext.getInstance("");
        // SSLEngine engine = context.createSSLEngine();

        System.out.println("STARTED");
        SSLServerSocketFactory ssf; 
        
        ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();  
        
        try {  
            serverSocket = (SSLServerSocket) ssf.createServerSocket(this.port);
            System.out.println("CREATED SERVER SOCKET");
        }  
        catch(IOException e) {  
            System.out.println("Server - Failed to create SSLServerSocket");  
            e.getMessage();  
            return;  
        }
      
      
        
    }


    public void sendMessage(byte[] message, InetAddress address) {
        // send request
        System.out.println("INSIDE SEND MESSAGE");
        InetSocketAddress socketAddress = new InetSocketAddress(address, this.port); 

        SSLSocketFactory ssf;
        SSLSocket socket; 

        ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try {
            socket = (SSLSocket) ssf.createSocket();
            socket.connect(socketAddress, 5000);
            OutputStream out = socket.getOutputStream();
            out.write(message);
            socket.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }


    public void run() {

        // maximum size of a chunk is 64KBytes (body)
        // header has at least 32 bytes (fileId) + version + messageType + senderId + chunkNo + replicationDegree
        // so 65KBytes should be sufficient to receive the message
        System.out.println("INSIDE RUN");
        byte[] buf = new byte[65000];

        while(true) {
            try {
                SSLSocket channel  = (SSLSocket) serverSocket.accept();
                InputStream is = channel.getInputStream();
                
                // receive a packet
                int bytesRead = is.read(buf, 0, buf.length);
                byte[] received = Arrays.copyOf(buf, bytesRead);

                //print received message
                System.out.println(new String(received));

                /* ManageReceivedMessages manager = new ManageReceivedMessages(this.peer, received);
                    
                // call a thread to execute the task
                this.peer.getThreadExec().execute(manager); */
                
            } catch (IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}