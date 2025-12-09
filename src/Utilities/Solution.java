package Utilities;

import java.util.List;

public class Solution {
    public List<Node> selectedNodes;
    public List<Integer> cycle; // order of node IDs
    public int totalCost;
    public int totalDistance;
    public int totalRunningTime;
    public int iterations;

    public Solution(List<Node> selectedNodes, List<Integer> cycle, int totalCost, int totalDistance, int totalRunningTime) {
        this.selectedNodes = selectedNodes;
        this.cycle = cycle;
        this.totalCost = totalCost;
        this.totalDistance = totalDistance;
        this.totalRunningTime = totalRunningTime;
        this.iterations = 0;
    }

}
