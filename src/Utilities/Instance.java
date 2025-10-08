package Utilities;

import java.util.List;

public class Instance {

    public List<Node> nodes;
    public int[][] distanceMatrix;
    int size;
    int[] costs;

    public Instance(String filePath) {
        this.nodes = Utils.readCSV(filePath);
        this.distanceMatrix = Utils.calculateDistanceMatrix(this.nodes);
        this.size = nodes.size();
        this.costs = new int[size];
        for (int i = 0; i < size; i++) {
            costs[i] = nodes.get(i).cost;

        }
    }
}
