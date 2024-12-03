import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Entity {
    private UUID id;
    private UUID idLeader;
    private HashMap<String,String> data = new HashMap<>();
    private HashMap<String,String> dataPending = new HashMap<>();
    private HashMap entidades = new HashMap();
    private NodeType type;
    private MessageList msgs = new MessageList();
    private int timeoutLimit;
    private int term = 0;
    private int voteCount = 0;
    private boolean votedInTerm = false;
    private long startTime;
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
        mR = new messageReceiver(msgs,this);
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
    }

    public synchronized boolean isElectionInitialized() {
        return type == NodeType.CANDIDATE && votedInTerm;
    }


    public NodeType getType() {
        return type;
    }

    public void vote(Heartbeat requestVote) {
        String candidateId = requestVote.getContent().get("candidateId");
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
            startTime = System.currentTimeMillis();
            ;
            System.out.println("Voted for candidate " + candidateId + " in term " + term);
        } else {
            System.out.println("Already voted in term " + term);
        }
    }

    public synchronized void processMessage(Heartbeat message) throws IOException {
        switch (message.getType()) {
            case SYNC -> processSync(message);
            case COMMIT -> processCommit(message);
            case ACK -> processAck(message);
            case NEWELEMENT -> processAckNew(message);
            case REQUEST_VOTE -> vote(message);
            case VOTE -> processVoteAck(message);
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


    private void processVoteAck(Heartbeat message) throws IOException {
        String voterId = message.getContent().get("voterId");
        int ackTerm = Integer.parseInt(message.getContent().get("term"));

        if (type == NodeType.CANDIDATE && ackTerm == term) {
            voteCount++;
            if (voteCount > (entidades.size() / 2)) { // Assuming totalNodes() gives the total nodes in the system
                becomeLeader();
            }
        }
    }

    private void processLeaderHeartbeat(Heartbeat message) {
        String leaderId = message.getContent().get("leaderId");
        int leaderTerm = Integer.parseInt(message.getContent().get("term"));

        if (leaderTerm >= term) {
            term = leaderTerm;
            idLeader = UUID.fromString(leaderId);
            type = NodeType.FOLLOWER;
            voteCount = 0;
            votedInTerm = false;
            System.out.println("Received LEADER heartbeat. Leader: " + leaderId + " for term: " + term);
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
    }

    public synchronized HashMap<String, String> generateHeartbeatContent() {
        HashMap<String, String> content = new HashMap<>();

        switch (type) {
            case LEADER:
                if (msgs.isEmpty()) {
                    content.put("leaderId", id.toString());
                    content.put("term", String.valueOf(term));
                }else{
                    content = msgs.getClone();
                }
                break;

            case CANDIDATE:
                content.put("candidateId", id.toString());
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
            startTime = System.currentTimeMillis();

            while (type == NodeType.FOLLOWER || type == NodeType.NEW) {
                try {
                    long currentTime = System.currentTimeMillis();
                    long timePassed = currentTime - startTime;

                    if (timePassed >= timeoutLimit) {
                        System.out.println("Timeout occurred. No leader heartbeat received. Starting election...");
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

    void processAckNew(Heartbeat ackMessage) {
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

    void sendNewFollowerBroadcast() throws IOException {
        HashMap<String, String> content = new HashMap<>();
        content.put("nodeID", id.toString());

        Heartbeat newFollowerMessage = new Heartbeat(MessageType.NEWELEMENT, content);
        mS.sendSerializedMessage(newFollowerMessage);
        System.out.println("New follower broadcast sent.");
    }
}
