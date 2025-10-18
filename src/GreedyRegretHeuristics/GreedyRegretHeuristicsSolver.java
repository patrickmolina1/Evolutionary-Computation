package GreedyRegretHeuristics;

import Utilities.Instance;
import Utilities.Node;
import Utilities.Solution;
import Utilities.Solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class GreedyRegretHeuristicsSolver extends Solver {

    public GreedyRegretHeuristicsSolver(){

    }

    public Solution greedy2RegretNearest(Instance instance, Node startNode) {
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
            int maxRegret = Integer.MIN_VALUE;

            for (Node candidate : remaining) {
                int bestIncrease = Integer.MAX_VALUE;
                int secondBestIncrease = Integer.MAX_VALUE;
                int bestPos = -1;

                for (int pos = 0; pos < order.size(); pos++) {
                    int prevNodeId = order.get(pos);
                    int nextNodeId = order.get((pos + 1) % order.size());

                    int distPrevToNext = instance.distanceMatrix[prevNodeId][nextNodeId];
                    int distPrevToCandidate = instance.distanceMatrix[prevNodeId][candidate.id];
                    int distCandidateToNext = instance.distanceMatrix[candidate.id][nextNodeId];

                    int distanceIncrease = distPrevToCandidate + distCandidateToNext - distPrevToNext;
                    int objectiveIncrease = distanceIncrease + candidate.cost;

                    if (objectiveIncrease < bestIncrease) {
                        secondBestIncrease = bestIncrease;
                        bestIncrease = objectiveIncrease;
                        bestPos = pos + 1;
                    } else if (objectiveIncrease < secondBestIncrease) {
                        secondBestIncrease = objectiveIncrease;
                    }
                }

                int regret = secondBestIncrease - bestIncrease;

                if (regret > maxRegret) {
                    maxRegret = regret;
                    bestCandidate = candidate;
                    bestPosition = bestPos;
                }
            }

            if (bestCandidate != null) {
                selected.add(bestPosition, bestCandidate);
                order.add(bestPosition, bestCandidate.id);
                remaining.remove(bestCandidate);
            }
        }

        int totalDistance = 0;
        for (int i = 0; i < order.size(); i++) {
            int from = order.get(i);
            int to = order.get((i + 1) % order.size());
            totalDistance += instance.distanceMatrix[from][to];
        }

        int totalNodeCost = selected.stream().mapToInt(node -> node.cost).sum();
        int totalCost = totalDistance + totalNodeCost;

        return new Solution(selected, order, totalCost, totalDistance);
    }

    public Solution greedyWeightedRegret(Instance instance, Node startNode, double weightRegret, double weightObjective) {
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
            double maxWeightedScore = Double.NEGATIVE_INFINITY;

            for (Node candidate : remaining) {
                int bestIncrease = Integer.MAX_VALUE;
                int secondBestIncrease = Integer.MAX_VALUE;
                int bestPos = -1;

                for (int pos = 0; pos < order.size(); pos++) {
                    int prevNodeId = order.get(pos);
                    int nextNodeId = order.get((pos + 1) % order.size());

                    int distPrevToNext = instance.distanceMatrix[prevNodeId][nextNodeId];
                    int distPrevToCandidate = instance.distanceMatrix[prevNodeId][candidate.id];
                    int distCandidateToNext = instance.distanceMatrix[candidate.id][nextNodeId];

                    int distanceIncrease = distPrevToCandidate + distCandidateToNext - distPrevToNext;
                    int objectiveIncrease = distanceIncrease + candidate.cost;

                    if (objectiveIncrease < bestIncrease) {
                        secondBestIncrease = bestIncrease;
                        bestIncrease = objectiveIncrease;
                        bestPos = pos + 1;
                    } else if (objectiveIncrease < secondBestIncrease) {
                        secondBestIncrease = objectiveIncrease;
                    }
                }

                int regret = secondBestIncrease - bestIncrease;
                double weightedScore = weightRegret * regret - weightObjective * bestIncrease;

                if (weightedScore > maxWeightedScore) {
                    maxWeightedScore = weightedScore;
                    bestCandidate = candidate;
                    bestPosition = bestPos;
                }
            }

            if (bestCandidate != null) {
                selected.add(bestPosition, bestCandidate);
                order.add(bestPosition, bestCandidate.id);
                remaining.remove(bestCandidate);
            }
        }

        int totalDistance = 0;
        for (int i = 0; i < order.size(); i++) {
            int from = order.get(i);
            int to = order.get((i + 1) % order.size());
            totalDistance += instance.distanceMatrix[from][to];
        }

        int totalNodeCost = selected.stream().mapToInt(node -> node.cost).sum();
        int totalCost = totalDistance + totalNodeCost;

        return new Solution(selected, order, totalCost, totalDistance);
    }




}
