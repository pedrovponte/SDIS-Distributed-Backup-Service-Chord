import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class Peer implements RemoteInterface {
    private ChannelController TCPChannel;
    //private broadcast.ChannelController MDB;
    //private broadcast.ChannelController MDR;
    //private TCPChannel tcpChannel;
    //private int TCPport;
    private String protocolVersion;
    private static int peerId;
    private static ScheduledThreadPoolExecutor threadExec;
    private static FileStorage storage;
    private ConcurrentHashMap<String, Integer> receivedChunkMessages;
    private ServerSocket serverSocket;
    private InetAddress address;
    private static ChordNode chordNode;
    private int tcpPort;
    private static Peer peer;

    public Peer(String[] args) {
        //this.receivedChunkMessages = new ConcurrentHashMap<String, Integer>();
        this.protocolVersion = args[0];
        peerId = Integer.parseInt(args[1]);
        System.out.println("ID: " + peerId);
        threadExec = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(10000);
        System.out.println("--- Created Threads ---");
        this.tcpPort = Integer.parseInt(args[3]);

        try {
            this.address = InetAddress.getLocalHost();
            System.out.println("ADDRESS: " + this.address + "; PORT: " + this.tcpPort);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        createChannels(this.address.getHostAddress(), this.tcpPort);
        System.out.println("--- TCP Channel Created ---");

        execChannels();
        System.out.println("--- Running TCP Channel --- \n\n");

        if(new File("peer_" + peerId + "/storage.ser").exists()) {
            deserialization();
        }
        else {
            storage = new FileStorage();
        }


        ConcurrentHashMap<String, Integer> stored = getStorage().getStoredMessagesReceived();
        System.out.println("-------STORED-------");
        for(String key : stored.keySet()) {
            System.out.println("Key: " + key);
            System.out.println("Value: " + stored.get(key));
        }
        System.out.println("Total: " + stored.size());

        ArrayList<FileManager> files = getStorage().getFilesStored();
        System.out.println("-------FILES-------");
        for(int i = 0; i < files.size(); i++) {
            System.out.println("Key: " + i);
            System.out.println("Value: " + files.get(i).getPath());
        }
        System.out.println("Total: " + files.size());

        ConcurrentHashMap<String, Chunk> chunks = getStorage().getChunksStored();
        System.out.println("-------CHUNKS-------");
        for(String key : chunks.keySet()) {
            System.out.println("Key: " + key);
            System.out.println("Value: " + chunks.get(key));
        }
        System.out.println("Total: " + chunks.size());


        chordNode = new ChordNode(this.address.getHostAddress(), this.tcpPort);


        try {
            Thread.sleep(1000);
        } catch(Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        if(args.length == 4) {
            chordNode.create();
        }
        else if(args.length == 6) {
            chordNode.join(args[4], Integer.parseInt(args[5]));
        }

        // chamar as atualizaçoes aqui ou no create?
    }

    public static void main(String[] args) {

        // add two arguments (ip address and port to connect to chord ring in case it exists. Otherwise, create a new one)
        if (args.length != 4 && args.length != 6) {
            System.out.println(
                    "Usage: java Peer <protocol_version> <peer_id> <service_access_point> <TCP_port> <ip_address_of_other> <port_of_other>");
            return;
        }

        String protocolVersion = args[0];

        if(!(protocolVersion.equals("1.0"))) {
            System.out.println("Invalid protocol version: " + protocolVersion + ". Available versions: '1.0' or '2.0'.");
            return;
        }

        int peerId = Integer.parseInt(args[1]);
        String serviceAccessPoint = args[2];
        int tcpPort = Integer.parseInt(args[3]);

        System.out.println("Protocol version: " + protocolVersion);
        System.out.println("Peer Id: " + peerId);
        System.out.println("Service Access Point: " + serviceAccessPoint);
        System.out.println("TCP Port: " + tcpPort);

        peer = new Peer(args);

        System.out.println("AFTER PEER CREATION");
        
        try {
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(peer, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(serviceAccessPoint, stub);
            System.out.println("--- Running RMI Registry ---");
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }


        // sends WORKING message in order to make all the other peers know that this peer is available and get the files deleted information in order to delete them and update chunksDistribution info
        /*if(protocolVersion.equals("2.0")) {
            // <Version> WORKING <PeerId> <CRLF><CRLF>
            String toSend = protocolVersion + " WORKING " + peerId + " \r\n\r\n";
            peer.getThreadExec().execute(new broadcast.ThreadSendMessages(peer.getMC(), toSend.getBytes()));
            System.out.println("SENT: " + toSend);
        }*/

        // serialize data before close peer
        Runtime.getRuntime().addShutdownHook(new Thread(Peer::serialization));
    }


    public ChannelController getTCPChannel() {
        return this.TCPChannel;
    }


    /*public broadcast.ChannelController getMDB() {
        return this.MDB;
    }

    
    public broadcast.ChannelController getMDR() {
        return this.MDR;
    }*/

    
    /*public TCPChannel getTcpChannel() {
        return this.tcpChannel;
    }*/

    
    public String getProtocolVersion() {
        return this.protocolVersion;
    }

    
    public static int getPeerId() {
        return peerId;
    }

    public static Peer getPeer() {
        return peer;
    }

    
    public static ScheduledThreadPoolExecutor getThreadExec() {
        return threadExec;
    }

    
    public static FileStorage getStorage() {
        return storage;
    }

    
    public ConcurrentHashMap<String,Integer> getReceivedChunkMessages() {
        return this.receivedChunkMessages;
    }

    public static ChordNode getChordNode() {
        return chordNode;
    }


    public InetAddress getAddress() {
        return this.address;
    }

    public int getTcpPort() {
        return this.tcpPort;
    }



    // increment the number of CHUNK messages received for a given chunkId, in order to know if some peer already has sent the chunk chunkId to the initiator peer
    public void incrementReceivedChunkMessagesNumber(String chunkId) {
        Integer number = this.receivedChunkMessages.get(chunkId);
        if(number == null) {
            this.receivedChunkMessages.put(chunkId, 1);
        }
        else {
            this.receivedChunkMessages.replace(chunkId, number + 1);
        }
    }


    public void createChannels(String ipAddress, int tcpPort) {
        this.TCPChannel = new ChannelController(ipAddress, tcpPort);
    }


    public void execChannels() {
        threadExec.execute(this.TCPChannel);
    }


    @Override
    public void backup(String path, int replication) {
        File backupFile = new File(path);

        if(!backupFile.exists()) {
            System.out.println("The file - " + path + " - doesn't exist.");
            return;
        }

        FileManager fileManager = new FileManager(path, replication, peerId);

        String fileIDNew = fileManager.getFileID();
        
        for(int i = 0; i < storage.getFilesStored().size(); i++) {
            if(storage.getFilesStored().get(i).getFileID().equals(fileIDNew)) {
                System.out.println("File already backed up by this peer.");
                return;
            }
        }

        storage.addFile(fileManager);

        // relacionado com enhancement do delete proj1
        /*if(storage.hasDeletedFile(fileManager.getFileID())) {
            storage.removeDeletedFile(fileManager.getFileID());
        }*/

        ArrayList<Chunk> fileChunks = fileManager.getFileChunks();

        for(int i = 0; i < fileChunks.size(); i++) {
            // <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
            try {
                Chunk chunk = fileChunks.get(i);

                MessageBuilder messageBuilder = new MessageBuilder();
                byte[] message = messageBuilder.constructPutChunkMessage(this, fileIDNew, chunk);

                if(!(storage.hasRegisterStore(fileManager.getFileID(), chunk.getChunkNo()))) {
                    storage.createRegisterToStore(fileManager.getFileID(), chunk.getChunkNo());
                }

                for(int j = 0; j < replication; j++) {
                    NodeInfo receiver;
                    if(j < chordNode.getFingerTableLength()) receiver = chordNode.getFingerTable().get(j);
                    else receiver = chordNode.getSuccessor();
                    
                    // send threads
                    threadExec.execute(new ThreadSendMessages(receiver.getIp(), receiver.getPort(), message));
                    threadExec.schedule(new ThreadCountStored(replication, fileManager.getFileID(), i, message, receiver), 1, TimeUnit.SECONDS);
                }
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    //TODO
    @Override
    public void restore(String path) {
        //<Version> GETCHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        if(!new File(path).exists()) {
            System.out.println("The file - " + path + " - doesn't exist.");
            return;
        }

        ArrayList<FileManager> files = getStorage().getFilesStored();
        ArrayList<String> filesNames = new ArrayList<>();

        for(FileManager fileManager : files) {
            filesNames.add(fileManager.getPath());
        }

        if(!filesNames.contains(path)){
            System.out.println("File " + path + " never backed up in this peer");
            return;
        }

        String fileID = null;
        FileManager file = null;
        for(FileManager fileManager : files) {
            if(fileManager.getPath().equals(path)) {
                file = fileManager;
                fileID = fileManager.getFileID();
                break;
            }
        }

        ArrayList<Chunk> chunks = file.getFileChunks();
        for(Chunk chunk : chunks) {
            try {
                MessageBuilder messageBuilder = new MessageBuilder();
                byte[] message = messageBuilder.constructGetChunkMessage(peer.getAddress().getHostAddress(), peer.getTcpPort(), fileID, chunk.getChunkNo());

                //send delete message
                for(ConcurrentHashMap.Entry<String, ArrayList<InetSocketAddress>> set : getStorage().getBackupChunksDistribution().entrySet()) {
                    if(set.getKey().equals(fileID + "_" + chunk.getChunkNo())) {
                        for(int j = 0; j<set.getValue().size(); j++)
                            threadExec.execute(new ThreadSendMessages(set.getValue().get(j).getAddress().getHostAddress(), set.getValue().get(j).getPort(), message));
                    }
                }
            } catch(Exception e) {
                System.err.println("Caught exception while restoring");
                e.printStackTrace();
            }
        }

        threadExec.execute(new ManageRestoreThread(this, file));
    }

    @Override
    public void delete(String path) {
        // <Version> DELETE <SenderId> <FileId> <CRLF><CRLF>
        if(!new File(path).exists()) {
            System.out.println("The file '" + path + "' doesn't exist.");
            return;
        }

        FileManager fileManager = new FileManager(path, peerId);
        String fileID = fileManager.getFileID();
        ArrayList<Chunk> fileChunks = fileManager.getFileChunks();
        ArrayList<FileManager> files = getStorage().getFilesStored();
        ArrayList<String> filesNames = new ArrayList<>();

        // This message does not elicit any response message. An implementation may send this message as many times as it is deemed necessary
        ArrayList<Chunk> chunks = fileManager.getFileChunks();
        for (Chunk chunk : chunks) {
            for(int i = 0; i < 5; i++) {
                MessageBuilder messageBuilder = new MessageBuilder();
                byte[] message = messageBuilder.constructDeleteMessage(peer.getAddress().getHostAddress(), peer.getTcpPort(), fileID, chunk.getChunkNo());

                try {
                    //send delete message
                    for(ConcurrentHashMap.Entry<String, ArrayList<InetSocketAddress>> set : getStorage().getBackupChunksDistribution().entrySet()) {
                        if(set.getKey().equals(fileID + "_" + chunk.getChunkNo())) {
                            for(int j=0; j < set.getValue().size(); j++)
                                threadExec.execute(new ThreadSendMessages(set.getValue().get(j).getAddress().getHostAddress(), set.getValue().get(j).getPort(), message));
                        }
                    }
                } catch(Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        if(!filesNames.contains(path)) return;
        getStorage().deleteFile(fileManager); //apagar ficheiro local
    }


    @Override
    public void reclaim(int maximum_disk_space) {
        // <Version> REMOVED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        int max_space = maximum_disk_space * 1000;

        storage.setCapacity(max_space);

        int occupiedSpace = storage.getPeerOccupiedSpace();

        int spaceToFree = occupiedSpace - max_space;

        if(spaceToFree > 0) {
            ConcurrentHashMap<String, Chunk> chunksStored = getStorage().getChunksStored();
            ArrayList<Chunk> chunks = new ArrayList<>();

            for(String key : chunksStored.keySet()) {
                chunks.add(chunksStored.get(key));
            }

            // descendant ordered list to start delete biggest chunks first
            Collections.sort(chunks, Comparator.comparing(Chunk::getSize));
            Collections.reverse(chunks);

            for(int i = 0; i < chunks.size(); i++) {
                String chunkId = chunks.get(i).getFileId() + "_" + chunks.get(i).getChunkNo();
                storage.deleteChunk(chunkId);
                
                MessageBuilder messageBuilder = new MessageBuilder();
                byte[] message = messageBuilder.constructRemovedMessage(this, chunks.get(i).getFileId(), chunks.get(i).getChunkNo());
                
                try {
                    threadExec.execute(new ThreadSendMessages(chunks.get(i).getIp(), chunks.get(i).getPort(), message));
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }

                spaceToFree -= chunks.get(i).getSize();

                String name = "peer_" + peerId + "/backup/" + chunkId;

                File filename = new File(name);

                filename.delete();

                if(spaceToFree <= 0) {
                    return;
                }
            }
        }        



        /*int max_space = maximum_disk_space * 1000;

        storage.setCapacity(max_space);

        int occupiedSpace = storage.getPeerOccupiedSpace();

        int spaceToFree = occupiedSpace - max_space;

        if(spaceToFree > 0) {
            ConcurrentHashMap<String, storage.Chunk> chunksStored = this.getStorage().getChunksStored();
            ArrayList<storage.Chunk> chunks = new ArrayList<>();

            for(String key : chunksStored.keySet()) {
                chunks.add(chunksStored.get(key));
            }

            // descendant ordered list to start delete biggest chunks first
            Collections.sort(chunks, Comparator.comparing(storage.Chunk::getSize));
            Collections.reverse(chunks);

            for(int i = 0; i < chunks.size(); i++) {
                String chunkId = chunks.get(i).getFileId() + "_" + chunks.get(i).getChunkNo();
                storage.deleteChunk(chunkId);

                String message = this.protocolVersion + " REMOVED " + peerId + " " + chunks.get(i).getFileId() + " " + chunks.get(i).getChunkNo() + " \r\n\r\n";
                try {
                    this.threadExec.execute(new broadcast.ThreadSendMessages(this.MC, message.getBytes(StandardCharsets.US_ASCII)));

                    System.out.println("SENT: " + message);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }

                spaceToFree -= chunks.get(i).getSize();

                String name = "peer_" + peerId + "/backup/" + chunkId;

                File filename = new File(name);

                filename.delete();

                if(spaceToFree <= 0) {
                    return;
                }
            }
        }*/
    }


    @Override
    public void state() {
        /*System.out.println();
        System.out.println("---------FILES BACKED UP---------");
        if(storage.getFilesStored() != null && !storage.getFilesStored().isEmpty()) {
            for(int i = 0; i < storage.getFilesStored().size(); i++) {
                String path = storage.getFilesStored().get(i).getPath();
                String fileId = storage.getFilesStored().get(i).getFileID();
                int desiredReplication = storage.getFilesStored().get(i).getReplication();
                System.out.println("PATHNAME: " + path);
                System.out.println("FILE ID: " + fileId);
                System.out.println("DESIRED REPLICATION: " + desiredReplication);
    
                System.out.println("------FILE CHUNKS------");
                for(int j = 0; j < storage.getFilesStored().get(i).getFileChunks().size(); j++) {
                    int chunkNo = storage.getFilesStored().get(i).getFileChunks().get(j).getChunkNo();
                    String chunkId = fileId + "_" + chunkNo;
                    System.out.println("CHUNK NO: " + chunkNo);
                    int perceivedReplication = storage.getPerceivedReplication(chunkId);
                    System.out.println("PERCEIVED REPLICATION DEGREE: " + perceivedReplication);
                    System.out.println();
                }
                System.out.println("---------------------------");
            }
        }
        
        System.out.println();
        System.out.println("---------CHUNKS STORED---------");
        for(String key : storage.getChunksStored().keySet()) {
            int chunkNo = storage.getChunksStored().get(key).getChunkNo();
            int size = storage.getChunksStored().get(key).getSize();
            int desiredReplication = storage.getChunksStored().get(key).getReplication();
            String fileId = storage.getChunksStored().get(key).getFileId();
            String chunkId = fileId + "_" + chunkNo;
            int perceivedReplication = storage.getPerceivedReplication(chunkId);
            System.out.println("CHUNK NO: " + chunkNo);
            System.out.println("SIZE: " + size / 1000 + " KBytes");
            System.out.println("DESIRED REPLICATION: " + desiredReplication);
            System.out.println("PERCEIVED REPLICATION: " + perceivedReplication);
            System.out.println();
        }
        
        System.out.println();
        int totalCapacity = storage.getCapacity();
        int occupiedCapacity = storage.getPeerOccupiedSpace();
        System.out.println("STORAGE CAPACITY: " + totalCapacity / 1000 + " KBytes");
        System.out.println("USED CAPACITY: " + occupiedCapacity / 1000 + " KBytes");*/
    }


    // https://www.tutorialspoint.com/java/java_serialization.htm
    public void deserialization() {
        System.out.println("Deserializing data...");
        try {
            String fileName = "peer_" + peerId + "/storage.ser";
            

            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            storage = (FileStorage) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            System.err.println(i.getMessage());
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Storage class not found");
            c.printStackTrace();
            return;
        }
    }


    // https://www.tutorialspoint.com/java/java_serialization.htm
    private static void serialization() {
        System.out.println("Serializing data...");
        try {
            String fileName = "peer_" + getPeerId() + "/storage.ser";
            File directory = new File("peer_" + getPeerId());

            if (!directory.exists()){
                // System.out.println("Not exists dir");
                directory.mkdir();
                (new File(fileName)).createNewFile();
            } 
            else if(directory.exists()) {
                if(!(new File(fileName).exists())) {
                    (new File(fileName)).createNewFile();
                }
            }

            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(storage);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            System.err.println(i.getMessage());
            i.printStackTrace();
        }
    }

    public byte[] hashChunkIdentifier(String fileId, int chunkNo, int replication) {
        String id = fileId + chunkNo + replication;
        try{
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] encodedHash = digest.digest(id.getBytes(StandardCharsets.UTF_8));
            
            return encodedHash;
        } catch(Exception e) {
            throw new RuntimeException(e);
        } 
    }

}
