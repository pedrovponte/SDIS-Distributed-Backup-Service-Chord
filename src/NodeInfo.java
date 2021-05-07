import java.net.InetSocketAddress;

public class NodeInfo {
    private int nodeId;
    private InetSocketAddress socketAddress;
    private String ip;
    private int port;

    public NodeInfo(int nodeId, InetSocketAddress socketAddress) {
        this.nodeId = nodeId;
        this.socketAddress = socketAddress;
        this.ip = this.socketAddress.getAddress().toString();
        this.port = this.socketAddress.getPort();
    }

    public int getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public InetSocketAddress getSocketAddress() {
        return this.socketAddress;
    }

    public void setSocketAddress(InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "{" +
            " nodeId='" + getNodeId() + "'" +
            ", socketAddress='" + getSocketAddress() + "'" +
            ", ip='" + getIp() + "'" +
            ", port='" + getPort() + "'" +
            "}";
    }
}
