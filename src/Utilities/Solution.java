package Utilities;

import java.util.List;

public class Solution {
    List<Node> selectedNodes;

    List<Integer> cycle; // order of node IDs
    int totalCost;

    public Solution(List<Node> selectedNodes, List<Integer> cycle, int totalCost) {
        this.selectedNodes = selectedNodes;
        this.cycle = cycle;
        this.totalCost = totalCost;
    }

}
