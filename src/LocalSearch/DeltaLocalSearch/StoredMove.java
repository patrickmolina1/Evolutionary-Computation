package LocalSearch.DeltaLocalSearch;

import LocalSearch.IntraRouteMoveType;

public class StoredMove {
    public String moveType;
    public int[] move;
    public int delta;
    public int distanceDelta;
    public IntraRouteMoveType intraType;


    public int prevNode;
    public int nextNode;
    public boolean prevEdgeReversed;
    public boolean nextEdgeReversed;

    // Constructor for intra-route moves
    public StoredMove(String moveType, int[] move, int delta, IntraRouteMoveType intraType) {
        this.moveType = moveType;
        this.move = move;
        this.delta = delta;
        this.intraType = intraType;
        this.distanceDelta = 0;
    }

    // Constructor for inter-route moves
    public StoredMove(String moveType, int[] move, int delta, int distanceDelta,
                      int prevNode, int nextNode, boolean prevEdgeReversed, boolean nextEdgeReversed) {
        this.moveType = moveType;
        this.move = move;
        this.delta = delta;
        this.distanceDelta = distanceDelta;
        this.prevNode = prevNode;
        this.nextNode = nextNode;
        this.prevEdgeReversed = prevEdgeReversed;
        this.nextEdgeReversed = nextEdgeReversed;
    }
}

