import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Entity {
    private UUID id;
    private UUID idLeader;
    private HashMap<String, String> data = new HashMap<>();
    private HashMap<String, String> dataPending = new HashMap<>();
    private HashMap entities = new HashMap();
    private HashMap pedidos = new HashMap();
    private HashMap acks = new HashMap();
    private NodeType type;
    private MessageList msgs = new MessageList();
    private int timeoutLimit;
    private int term = 0;
    private int voteCount = 0;
    private boolean votedInTerm = false;
    private volatile long startTime;
    private messageSender mS;
    private messageReceiver mR;


    public UUID getId() {
        return id;
    }

    public Entity() throws IOException, InterruptedException {
        id = UUID.randomUUID();
        type = NodeType.NEW;
        timeoutLimit = HeartbeatTime.generateRandomTimeout();
        mS = new messageSender(msgs, this);
        mR = new messageReceiver(msgs, this);
        sendNewFollowerBroadcast();
        followerTimeoutCheck();
    }

    public synchronized void initializeElection() throws IOException {
        if (type != NodeType.CANDIDATE) {
            type = NodeType.CANDIDATE;
            term++;
            voteCount = 1; // Vote for self
            votedInTerm = true;
            System.out.println("Node " + id + " became a candidate for term " + term + " and voted for self.");
        }
        Heartbeat voteRequest = new Heartbeat(MessageType.REQUEST_VOTE, generateHeartbeatContent());
        mS.sendSerializedMessage(voteRequest);
        if (entities.isEmpty()) becomeLeader();
    }

    public synchronized boolean isElectionInitialized() {
        return type == NodeType.CANDIDATE && votedInTerm;
    }


    public NodeType getType() {
        return type;
    }

    public void vote(Heartbeat requestVote) throws IOException {
        String candidateId = requestVote.getContent().get("candidateID");
        int candidateTerm = Integer.parseInt(requestVote.getContent().get("term"));

        if (candidateTerm < term) {
            System.out.println("Rejected vote request from " + candidateId + " due to older term.");
            return;
        }

        if (candidateTerm > term) {
            term = candidateTerm;
            votedInTerm = false;
        }

        if (!votedInTerm) {
            votedInTerm = true;
            synchronized (this) { // Synchronize to avoid race conditions
                startTime = System.currentTimeMillis(); // Reset timeout start time
                System.out.println("\nstartTime is " + startTime + "On node " + id.toString());
            }
            HashMap<String, String> content = new HashMap<>();
            content.put("candidateID", candidateId);
            content.put("term", String.valueOf(term));

            Heartbeat vote = new Heartbeat(MessageType.VOTE, content);
            mS.sendSerializedMessage(vote);
            System.out.println("Voted for candidate " + candidateId + " in term " + term);


        } else {
            //System.out.println("Already voted in term " + term);
        }
    }

    public synchronized void processMessage(Heartbeat message) throws IOException {
        switch (message.getType()) {
            case SYNC -> processSync(message);
            case COMMIT -> processCommit(message);
            case ACK -> processAck(message);
            case NEWELEMENT -> processNew(message);
            case REQUEST_VOTE -> vote(message);
            case VOTE -> processVote(message);
            case LEADERHEARTBEAT -> processLeaderHeartbeat(message);
            case LEADER -> processLeader(message);
            case ACKNEW -> processNewAck(message);
            default -> throw new IllegalArgumentException("Unknown message type: " + message.getType());
        }
    }

    public void processSync(Heartbeat syncHeartbeat) throws IOException {
        String leaderId = syncHeartbeat.getContent().get("leaderId");
        HashMap<String, String> dataUpdate = syncHeartbeat.getContent();
        System.out.println("Received SYNC update from leader " + leaderId + ": " + dataUpdate);
        for (Map.Entry<String, String> entry : dataUpdate.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            dataPending.put(key, value);
        }

        HashMap<String, String> content = new HashMap<>();
        content.put("followerId", id.toString());
        content.put("term", String.valueOf(term));
        content.put("ack", "true");

        Heartbeat ack = new Heartbeat(MessageType.ACK, content);
        mS.sendSerializedMessage(ack);
    }


    private void processVote(Heartbeat message) throws IOException {
        String voterId = message.getContent().get("candidateID");
        int ackTerm = Integer.parseInt(message.getContent().get("term"));

        if (type == NodeType.CANDIDATE && ackTerm == term && voterId.equalsIgnoreCase(id.toString())) {
            voteCount++;
            if (voteCount > (entities.size() / 2)) {
                System.out.println("Number of entities: " + entities.size());
                becomeLeader();
            }
        }
    }

    private void processLeader(Heartbeat message) throws IOException {
        String leaderID = message.getContent().get("leaderID");
        int ackTerm = Integer.parseInt(message.getContent().get("term"));

        idLeader = UUID.fromString(leaderID);
    }

    private void processLeaderHeartbeat(Heartbeat message) {
        String leaderId = message.getContent().get("leaderId");
        int leaderTerm = Integer.parseInt(message.getContent().get("term"));

        if (leaderTerm >= term) {
            synchronized (this) {
                startTime = System.currentTimeMillis();
                System.out.println("\nstartTime is " + startTime + "On node " + id.toString());
            }
            term = leaderTerm;
            idLeader = UUID.fromString(leaderId);
            type = NodeType.FOLLOWER;
            voteCount = 0;
            votedInTerm = false;
            System.out.println("Received LEADER heartbeat. Leader: " + leaderId + " for term: " + term + "\n On entity: " + id.toString());
        } else {
            System.out.println("Ignoring outdated LEADER heartbeat from " + leaderId);
        }
    }


    private void processCommit(Heartbeat commitMessage) {
        System.out.println("COMMIT message received. Committing pending messages.");
        for (Map.Entry<String, String> entry : dataPending.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            data.put(key, value);
        }
    }

    private void becomeLeader() throws IOException {
        type = NodeType.LEADER;
        idLeader = id;
        System.out.println("Node " + id + " became leader in term " + term);
        HashMap<String, String> content = new HashMap<>();
        content.put("leaderID", id.toString());
        content.put("term", String.valueOf(term));

        Heartbeat leader = new Heartbeat(MessageType.LEADER, content);
        mS.sendSerializedMessage(leader);
        try {
            Registry registry = LocateRegistry.getRegistry(2000); // Connect to existing registry
            registry.unbind("MessageUpdater"); // Unbind if already registered
            System.out.println("Unbound previous MessageUpdater (if existed).");
        } catch (NotBoundException e) {
            System.out.println("No previous MessageUpdater to unbind, continuing...");
        } catch (Exception e) {
            System.err.println("Error during unbinding: " + e.getMessage());
        }
        Registry r = LocateRegistry.createRegistry(2000);

        MessageListInterface mList = msgs;

        r.rebind("MessageUpdater", mList);
    }

    public synchronized HashMap<String, String> generateHeartbeatContent() {
        HashMap<String, String> content = new HashMap<>();

        switch (type) {
            case LEADER:
                if (msgs.isEmpty()) {
                    content.put("leaderId", id.toString());
                    content.put("term", String.valueOf(term));
                } else {
                    content = msgs.getClone();
                }
                break;

            case CANDIDATE:
                content.put("candidateID", id.toString());
                content.put("term", String.valueOf(term));
                break;

            case FOLLOWER:
                content.put("followerId", id.toString());
                content.put("status", "new");
                break;

            default:
                break;
        }

        return content;
    }

    private void followerTimeoutCheck() {
        new Thread(() -> {
            synchronized (this) {
                startTime = System.currentTimeMillis();
                System.out.println("\nstartTime is " + startTime + "On node " + id.toString());
            }
            long timePassed;

            while (type == NodeType.FOLLOWER || type == NodeType.NEW) {
                try {
                    long currentTime = System.currentTimeMillis();
                    synchronized (this) {
                        timePassed = currentTime - startTime;
                    }

                    if (timePassed >= timeoutLimit) {
                        System.out.println("\nstartTime is " + startTime + "On node " + id.toString());
                        System.out.println("Passed " + timePassed + "On node " + id.toString());
                        System.out.println("Timeout occurred. No leader heartbeat received. Starting election...\n");
                        initializeElection();
                        break;
                    }

                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Timeout check interrupted.");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    void processAck(Heartbeat ackMessage) {
        if (type != NodeType.LEADER) {
            System.out.println("Received ACK, but this node is not a leader.");
            return;
        }

        String followerId = ackMessage.getContent().get("followerId");
        String ackTerm = ackMessage.getContent().get("term");

        if (Integer.parseInt(ackTerm) < term) {
            System.out.println("Ignoring outdated ACK from follower: " + followerId);
            return;
        }

        System.out.println("Received ACK from follower: " + followerId + " for term: " + term);
    }

    void processNew(Heartbeat newEntity) throws IOException {
        String nodeID = newEntity.getContent().get("nodeID");
        if (!nodeID.equalsIgnoreCase(id.toString()) && !nodeID.equalsIgnoreCase(id.toString())) {
            entities.put(nodeID, UUID.fromString(nodeID));
            System.out.println("New Node added on: " + id.toString() + "\tNode list: " + entities.toString());
        }
        sendNewAckBroadcast();
    }

    void processNewAck(Heartbeat newAck) {
        String id = newAck.getContent().get("nodeID");
        if (type == NodeType.NEW && !entities.containsKey(id) && !id.equalsIgnoreCase(id.toString())) {
            entities.put(id, UUID.fromString(id));
            System.out.println("New Node added on: " + id + "\tNode list: " + entities.toString());
        }
        if (newAck.getContent().get("isLeader").equalsIgnoreCase("true")) {
            String[] keyValuePairs = newAck.getContent().get("content").split(", ");
            for (String pair : keyValuePairs) {
                String[] entry = pair.split("=");
                data.put(entry[0], entry[1]);
            }
            type = NodeType.FOLLOWER;
        }
    }

    void sendNewAckBroadcast() throws IOException {
        HashMap<String, String> content = new HashMap<>();
        content.put("nodeID", id.toString());
        if (type == NodeType.LEADER) {
            content.put("isLeader", "true");
            content.put("content", data.toString());
        } else {
            content.put("isLeader", "false");
        }

        Heartbeat newFollowerMessage = new Heartbeat(MessageType.ACKNEW, content);
        mS.sendSerializedMessage(newFollowerMessage);
    }

    void sendNewFollowerBroadcast() throws IOException {
        HashMap<String, String> content = new HashMap<>();
        content.put("nodeID", id.toString());

        Heartbeat newFollowerMessage = new Heartbeat(MessageType.NEWELEMENT, content);
        mS.sendSerializedMessage(newFollowerMessage);
        System.out.println("New follower broadcast sent. from follower: " + id.toString());
    }

}
