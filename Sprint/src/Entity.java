import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

public class Entity {
    private UUID id;
    private UUID idLider;
    private HashMap data;
    private HashMap dataPending;
    private HashMap entidades;
    private NodeType type;
    private MessageList msgs;
    private int timeoutLimit;
    private int term = 0;
    private int voteCount = 0;
    private boolean votedInTerm = false;
    private long startTime;

    public Entity() throws IOException, InterruptedException {
        id = UUID.randomUUID();
        type = NodeType.Follower;
        timeoutLimit = HeartbeatTime.generateRandomTimeout();
        //Codigo para verificar quem é o líder se não existir passar para candidato
        new messageSender(msgs,this);
        new messageReceiver(id,idLider,this);
        followerTimeoutCheck();
    }

    public synchronized void election() {
        type = NodeType.CANDIDATE;
        term++;
        voteCount = 1;
        votedInTerm = true;

        /*HashMap<String, String> content = new HashMap<>();
        content.put("candidateId", id.toString());
        content.put("term", String.valueOf(term));

        Heartbeat requestVote = new Heartbeat(MessageType.VOTE, content);

        msgs.addElement(UUID.randomUUID().toString(), serialize(requestVote));*/


        System.out.println("Started election for term " + term + ". Sent RequestVote messages.");
    }


    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
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
            startTime = System.currentTimeMillis();;
            System.out.println("Voted for candidate " + candidateId + " in term " + term);
        } else {
            System.out.println("Already voted in term " + term);
        }
    }

    public synchronized void processMessage(Heartbeat message) {
        switch (message.getType()) {
            case VOTE:
                processVoteRequest(message);
                break;
            case ACK:
                //FIX
                processVoteAck(message);
                break;
            case LEADER:
                //FIX
                processLeaderHeartbeat(message);
                break;
            default:
                break;
        }
    }

    private void processVoteRequest(Heartbeat message) {
        String candidateId = message.getContent().get("candidateId");
        int candidateTerm = Integer.parseInt(message.getContent().get("term"));

        if (candidateTerm > term) {
            term = candidateTerm;
            type = NodeType.Follower;
            votedInTerm = false;
        }

        if (!votedInTerm && candidateTerm >= term) {
            votedInTerm = true;
            timeoutLimit = HeartbeatTime.generateRandomTimeout();

            // Send VoteAck
            HashMap<String, String> content = new HashMap<>();
            content.put("voterId", id.toString());
            content.put("term", String.valueOf(term));
            Heartbeat voteAck = new Heartbeat(MessageType.ACK, content);
            msgs.addElement(UUID.randomUUID().toString(), serialize(voteAck));
            System.out.println("Voted for candidate " + candidateId);
        }
    }

    public void processSync(Heartbeat syncHeartbeat) {
        // Extract and process update data
        String leaderId = syncHeartbeat.getContent().get("leaderId");
        String dataUpdate = syncHeartbeat.getContent().get("data");
        System.out.println("Received SYNC update from leader " + leaderId + ": " + dataUpdate);

        // Respond with ACK to the leader
        HashMap<String, String> content = new HashMap<>();
        content.put("followerId", id.toString());
        content.put("term", String.valueOf(term));
        content.put("ack", "true");

        Heartbeat ack = new Heartbeat(MessageType.ACK, content);
        msgs.addElement(UUID.randomUUID().toString(), serialize(ack));
    }


    private void processVoteAck(Heartbeat message) {
        String voterId = message.getContent().get("voterId");
        int ackTerm = Integer.parseInt(message.getContent().get("term"));

        if (type == NodeType.Candidate && ackTerm == term) {
            voteCount++;
            if (voteCount > (totalNodes() / 2)) { // Assuming totalNodes() gives the total nodes in the system
                becomeLeader();
            }
        }
    }

    private void processLeaderHeartbeat(Heartbeat message) {
        String newLeaderId = message.getContent().get("leaderId");
        int leaderTerm = Integer.parseInt(message.getContent().get("term"));

        if (leaderTerm >= term) {
            term = leaderTerm;
            idLeader = UUID.fromString(newLeaderId);
            type = NodeType.Follower;
            votedInTerm = false;
            electionInProgress = false;
        }
    }

    private void becomeLeader() {
        type = NodeType.Leader;
        idLeader = id;
        electionInProgress = false;
        System.out.println("Node " + id + " became leader in term " + term);
        sendLeaderHeartbeats();
    }

    private void sendLeaderHeartbeats() {
        new Thread(() -> {
            while (type == NodeType.Leader) {
                HashMap<String, String> content = new HashMap<>();
                content.put("leaderId", id.toString());
                content.put("term", String.valueOf(term));
                Heartbeat heartbeat = new Heartbeat(MessageType.LEADER, content);
                msgs.addElement(UUID.randomUUID().toString(), serialize(heartbeat));

                try {
                    Thread.sleep(HeartbeatTime.TIME.getValue());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public MessageType getHeartbeatType() {
        switch (type) {
            case Leader:
                return MessageType.LEADER;
            case Candidate:
                return MessageType.VOTE; // Send vote requests
            default:
                return MessageType.SYNC; // Followers send sync requests, if needed
        }
    }

    public synchronized HashMap<String, String> generateHeartbeatContent() {
        HashMap<String, String> content = new HashMap<>();

        switch (type) {
            case Leader:
                content.put("leaderId", id.toString());
                content.put("term", String.valueOf(term));
                content.put("data", getPendingUpdates()); // Example: sync updates
                break;

            case Candidate:
                content.put("candidateId", id.toString());
                content.put("term", String.valueOf(term));
                break;

            case Follower:
                // Followers generally don't send heartbeats unless for sync
                break;

            default:
                break;
        }

        return content;
    }

    private void followerTimeoutCheck() {
        new Thread(() -> {
            startTime = System.currentTimeMillis();

            while (type == NodeType.Follower) {
                try {
                    long currentTime = System.currentTimeMillis();
                    long timePassed = currentTime - startTime;

                    if (timePassed >= timeoutLimit) {
                        System.out.println("Timeout occurred. No leader heartbeat received. Starting election...");
                        election();
                        break;
                    }

                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Timeout check interrupted.");
                }
            }
        }).start();
    }


}
