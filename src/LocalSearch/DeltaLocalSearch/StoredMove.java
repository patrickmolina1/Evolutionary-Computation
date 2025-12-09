package LocalSearch.DeltaLocalSearch;

import LocalSearch.IntraRouteMoveType;

public class StoredMove {
    public String moveType;
    public int[] move;
    public int delta;
    public int distanceDelta;
    public IntraRouteMoveType intraType;


    public int edge1Start;
    public int edge1End;
    public int edge2Start;
    public int edge2End;

    public StoredMove(String moveType, int[] move, int delta, IntraRouteMoveType intraType,
                      int edge1Start, int edge1End, int edge2Start, int edge2End) {
        this.moveType = moveType;
        this.move = move;
        this.delta = delta;
        this.intraType = intraType;
        this.edge1Start = edge1Start;
        this.edge1End = edge1End;
        this.edge2Start = edge2Start;
        this.edge2End = edge2End;
        this.distanceDelta = 0;
    }

    public StoredMove(String moveType, int[] move, int delta, int distanceDelta,
                      int e1s, int e1e, int e2s, int e2e) {
        this.moveType = moveType;
        this.move = move;
        this.delta = delta;
        this.distanceDelta = distanceDelta;
        this.edge1Start = e1s;
        this.edge1End = e1e;
        this.edge2Start = e2s;
        this.edge2End = e2e;
    }
}
