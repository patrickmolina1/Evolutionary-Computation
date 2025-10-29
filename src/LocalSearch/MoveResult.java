package LocalSearch;

public class MoveResult {
    public boolean improved;
    public int totalDelta;
    public int distanceDelta;

    public MoveResult(boolean improved, int totalDelta, int distanceDelta) {
        this.improved = improved;
        this.totalDelta = totalDelta;
        this.distanceDelta = distanceDelta;
    }
}