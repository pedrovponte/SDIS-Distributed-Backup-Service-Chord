public class StoredMessageThread implements Runnable {
    private Message message;
    private int senderId;
    private String address;
    private int port;
    private String fileId;
    private int chunkNo;
    private String protocolVersion;

    // Version STORED <SenderId> <Address> <Port> <FileId> <ChunkNo>
    public StoredMessageThread(Message message) {
        this.message = message;
        String[] header = this.message.getHeader();
        this.senderId = Integer.parseInt(header[2]);
        this.address = header[3];
        this.port = Integer.parseInt(header[4]);
        this.fileId = header[5];
        this.chunkNo = Integer.parseInt(header[6]);
        this.protocolVersion = header[0];
    }
    
    
    @Override
	public void run() {
		System.out.println("RECEIVED: " + this.protocolVersion + " STORED " + this.senderId + " " + this.address + " " + this.port + " " + this.fileId + " " + this.chunkNo);

        // add the regist to the chunksDistribution table. By doing that, all peers know the peers that each one has stored
        //Peer.getStorage().addChunksDistribution(senderId, fileId, chunkNo);

        //if(this.peer.getPeerId() != senderId) {
            //System.out.println("Different peer and sender");
        if(Peer.getStorage().hasRegisterStore(fileId, chunkNo)) {
            //System.out.println("Has regist");
            Peer.getStorage().incrementStoredMessagesReceived(senderId, fileId, chunkNo);
            //System.out.println("INCREMENTED");
            Peer.getStorage().addBackupFileDistribution(this.address, this.port, this.fileId, this.chunkNo);
            //System.out.println("ADDED BACKUP FILE");
        }
        //}
		
	}
    
}