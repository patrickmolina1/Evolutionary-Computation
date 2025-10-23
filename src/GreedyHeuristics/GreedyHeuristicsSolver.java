package GreedyHeuristics;

import Utilities.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class GreedyHeuristicsSolver extends Solver {

    public GreedyHeuristicsSolver(){

    }

    public Solution randomSolution(Instance instance) {
        int startTime = (int) System.currentTimeMillis();
        Random rand = new Random();
        int n = instance.nodes.size();
        int numToSelect = (int) Math.ceil(n / 2.0);

        List<Node> shuffled = new ArrayList<>(instance.nodes);
        Collections.shuffle(shuffled, rand);
        List<Node> selected = shuffled.subList(0, numToSelect);

        List<Integer> order = new ArrayList<>();
        for (Node node : selected) order.add(node.id);
        Collections.shuffle(order, rand);

        int totalDistance = 0;
        for (int i = 0; i < order.size(); i++) {
            int from = order.get(i);
            int to = order.get((i + 1) % order.size());
            totalDistance += instance.distanceMatrix[from][to];
        }

        int totalNodeCost = selected.stream().mapToInt(nod -> nod.cost).sum();
        int totalCost = totalDistance + totalNodeCost;

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selected, order, totalCost, totalDistance, endTime - startTime);
    }

    public Solution nearestNeighborEndOnly(Instance instance) {

        int startTime = (int) System.currentTimeMillis();
        Random rand = new Random();

        List<Node> allNodes = new ArrayList<>(instance.nodes);
        Node startNode = allNodes.get(rand.nextInt(allNodes.size()));

        List<Node> selected = new ArrayList<>();
        List<Integer> order = new ArrayList<>();
        selected.add(startNode);
        order.add(startNode.id);

        List<Node> remaining = new ArrayList<>(allNodes);
        remaining.remove(startNode);

        int n = instance.nodes.size();
        int numToSelect = (int) Math.ceil(n / 2.0);

        while (selected.size() < numToSelect && !remaining.isEmpty()) {
            Node lastNode = selected.get(selected.size() - 1);
            Node firstNode = selected.get(0);

            Node bestCandidate = null;
            int minIncrease = Integer.MAX_VALUE;

            for (Node candidate : remaining) {
                int distToCandidate = instance.distanceMatrix[lastNode.id][candidate.id];
                int distCandidateToFirst = instance.distanceMatrix[candidate.id][firstNode.id];
                int distLastToFirst = instance.distanceMatrix[lastNode.id][firstNode.id];

                int distanceIncrease = distToCandidate + distCandidateToFirst - distLastToFirst;
                int objectiveIncrease = distanceIncrease + candidate.cost;

                if (objectiveIncrease < minIncrease) {
                    minIncrease = objectiveIncrease;
                    bestCandidate = candidate;
                }
            }

            if (bestCandidate != null) {
                selected.add(bestCandidate);
                order.add(bestCandidate.id);
                remaining.remove(bestCandidate);
            }
        }

        // Calculate total distance
        int totalDistance = 0;
        for (int i = 0; i < order.size(); i++) {
            int from = order.get(i);
            int to = order.get((i + 1) % order.size());
            totalDistance += instance.distanceMatrix[from][to];
        }

        int totalNodeCost = selected.stream().mapToInt(node -> node.cost).sum();
        int totalCost = totalDistance + totalNodeCost;

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selected, order, totalCost, totalDistance, endTime - startTime);
    }


    public Solution nearestNeighborAllPositions(Instance instance) {
        int startTime = (int) System.currentTimeMillis();
        Random rand = new Random();
        List<Node> allNodes = new ArrayList<>(instance.nodes);
        Node startNode = allNodes.get(rand.nextInt(allNodes.size()));

        List<Node> selected = new ArrayList<>();
        List<Integer> order = new ArrayList<>();
        selected.add(startNode);
        order.add(startNode.id);

        List<Node> remaining = new ArrayList<>(allNodes);
        remaining.remove(startNode);

        int n = instance.nodes.size();
        int numToSelect = (int) Math.ceil(n / 2.0);

        while (selected.size() < numToSelect && !remaining.isEmpty()) {
            Node bestCandidate = null;
            int bestPosition = -1;
            int minIncrease = Integer.MAX_VALUE;

            for (Node candidate : remaining) {
                for (int i = 0; i < order.size(); i++) {
                    int prevNodeId = order.get(i);
                    int nextNodeId = order.get((i + 1) % order.size());

                    int distPrevToNext = instance.distanceMatrix[prevNodeId][nextNodeId];
                    int distPrevToCandidate = instance.distanceMatrix[prevNodeId][candidate.id];
                    int distCandidateToNext = instance.distanceMatrix[candidate.id][nextNodeId];

                    int distanceIncrease = distPrevToCandidate + distCandidateToNext - distPrevToNext;
                    int objectiveIncrease = distanceIncrease + candidate.cost;

                    if (objectiveIncrease < minIncrease) {
                        minIncrease = objectiveIncrease;
                        bestCandidate = candidate;
                        bestPosition = i + 1;
                    }
                }
            }

            if (bestCandidate != null) {
                selected.add(bestPosition, bestCandidate);
                order.add(bestPosition, bestCandidate.id);
                remaining.remove(bestCandidate);
            }
        }

        int totalDistance = 0;
        // Compute the total distance of the cycle, including the edge from the last node back to the first (using modulo for wrap-around)
        for (int i = 0; i < order.size(); i++) {
            int from = order.get(i);
            int to = order.get((i + 1) % order.size());
            totalDistance += instance.distanceMatrix[from][to];
        }

        int totalNodeCost = selected.stream().mapToInt(node -> node.cost).sum();
        int totalCost = totalDistance + totalNodeCost;

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selected, order, totalCost, totalDistance, endTime - startTime);
    }



    public Solution greedyCycle(Instance instance, Node startNode) {
        int startTime = (int) System.currentTimeMillis();
        List<Node> selected = new ArrayList<>();
        List<Integer> order = new ArrayList<>();
        selected.add(startNode);
        order.add(startNode.id);

        List<Node> remaining = new ArrayList<>(instance.nodes);
        remaining.remove(startNode);

        int n = instance.nodes.size();
        int numToSelect = (int) Math.ceil(n / 2.0);

        while (selected.size() < numToSelect && !remaining.isEmpty()) {
            Node bestCandidate = null;
            int bestPosition = -1;
            int minIncrease = Integer.MAX_VALUE;

            for (Node candidate : remaining) {
                for (int pos = 0; pos < order.size(); pos++) {
                    int prevNodeId = order.get(pos);
                    int nextNodeId = order.get((pos + 1) % order.size());

                    int distPrevToNext = instance.distanceMatrix[prevNodeId][nextNodeId];
                    int distPrevToCandidate = instance.distanceMatrix[prevNodeId][candidate.id];
                    int distCandidateToNext = instance.distanceMatrix[candidate.id][nextNodeId];

                    int distanceIncrease = distPrevToCandidate + distCandidateToNext - distPrevToNext;
                    int objectiveIncrease = distanceIncrease + candidate.cost;

                    if (objectiveIncrease < minIncrease) {
                        minIncrease = objectiveIncrease;
                        bestCandidate = candidate;
                        bestPosition = pos + 1;
                    }
                }
            }

            if (bestCandidate != null) {
                selected.add(bestPosition, bestCandidate);
                order.add(bestPosition, bestCandidate.id);
                remaining.remove(bestCandidate);
            }
        }

        // Calculate total distance
        int totalDistance = 0;
        for (int i = 0; i < order.size(); i++) {
            int from = order.get(i);
            int to = order.get((i + 1) % order.size());
            totalDistance += instance.distanceMatrix[from][to];
        }

        int totalNodeCost = selected.stream().mapToInt(node -> node.cost).sum();
        int totalCost = totalDistance + totalNodeCost;

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selected, order, totalCost, totalDistance, endTime - startTime);
    }

    public List<Solution> generateSolutions(Instance instance) {
        List<Solution> solutions = new ArrayList<>();

        // Generate 200 random solutions
        for (int i = 0; i < 200; i++) {
            solutions.add(randomSolution(instance));
        }

        // Generate 200 solutions for each greedy method starting from each node
        for (Node startNode : instance.nodes) {
            solutions.add(nearestNeighborEndOnly(instance)); // uses random start internally
            solutions.add(nearestNeighborAllPositions(instance)); // uses random start internally
            solutions.add(greedyCycle(instance, startNode));
        }

        return solutions;
    }



}
