package GreedyHeuristics;

import Utilities.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class Solver {

    public Solver(){

    }


    public Solution randomSolution(Instance instance) {

        Random rand = new Random();

        int n = instance.nodes.size();
        int numToSelect = (int) Math.ceil(n / 2.0);

        // 1. Randomly select ⌈n/2⌉ nodes
        List<Node> shuffled = new ArrayList<>(instance.nodes);
        Collections.shuffle(shuffled, rand);
        List<Node> selected = shuffled.subList(0, numToSelect);

        // 2. Create a random order (cycle)
        List<Integer> order = new ArrayList<>();
        for (Node node : selected) order.add(node.id);
        Collections.shuffle(order, rand);

        // 3. Compute total distance (cycle means returning to start)
        int totalDistance = 0;
        for (int i = 0; i < order.size(); i++) {
            int from = order.get(i);
            int to = order.get((i + 1) % order.size()); // wrap around
            totalDistance += instance.distanceMatrix[from][to];
        }

        // 4. Compute total node cost
        int totalNodeCost = selected.stream().mapToInt(nod -> nod.cost).sum();

        int totalCost = totalDistance + totalNodeCost;

        return new Solution(selected, order, totalCost);
    }

}
