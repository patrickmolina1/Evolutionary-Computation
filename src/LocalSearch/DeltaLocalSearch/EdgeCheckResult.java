package LocalSearch.DeltaLocalSearch;

public enum EdgeCheckResult {
    NOT_EXIST,           // Case 1: Remove from LM
    DIFFERENT_DIRECTION, // Case 2: Keep in LM, don't apply
    SAME_DIRECTION       // Case 3: Apply and remove from LM
}
