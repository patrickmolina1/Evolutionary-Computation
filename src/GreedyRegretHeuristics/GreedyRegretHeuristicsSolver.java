package GreedyRegretHeuristics;

import Utilities.Instance;
import Utilities.Node;
import Utilities.Solution;
import Utilities.Solver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GreedyRegretHeuristicsSolver extends Solver {

    public GreedyRegretHeuristicsSolver() {
    }

    public Solution greedy2RegretGreedyCycle(Instance instance, Node startNode) {
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
            int maxRegret = Integer.MIN_VALUE;

            for (Node candidate : remaining) {
                int bestIncrease = Integer.MAX_VALUE;
                int secondBestIncrease = Integer.MAX_VALUE;
                int bestPos = -1;

                // Iterates N times, for N edges in the cycle.
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

                int regret = (secondBestIncrease == Integer.MAX_VALUE) ? bestIncrease : (secondBestIncrease - bestIncrease);

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
            } else {
                break;
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

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selected, order, totalCost, totalDistance, endTime - startTime);
    }

    public Solution greedyWeightedRegretGreedyCycle(Instance instance, Node startNode, double weightRegret, double weightObjective) {

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
            double maxWeightedScore = Double.NEGATIVE_INFINITY;

            for (Node candidate : remaining) {
                int bestIncrease = Integer.MAX_VALUE;
                int secondBestIncrease = Integer.MAX_VALUE;
                int bestPos = -1;


                // Iterates N times, for N edges in the cycle.
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

                int regret = (secondBestIncrease == Integer.MAX_VALUE) ? bestIncrease : (secondBestIncrease - bestIncrease);
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
            } else {
                break;
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

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selected, order, totalCost, totalDistance, endTime - startTime);
    }


    public Solution greedy2RegretNearestNeighbor(Instance instance, Node startNode) {
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
            // Step 1: Find best candidate to add based on regret
            Node bestCandidateToAdd = null;
            int maxRegret = Integer.MIN_VALUE;

            for (Node candidate : remaining) {
                // Find minimum distance to any node in tour
                int minDistanceToTour = Integer.MAX_VALUE;
                int secondMinDistanceToTour = Integer.MAX_VALUE;

                for (Node inTour : selected) {
                    int distance = instance.distanceMatrix[inTour.id][candidate.id];
                    if (distance < minDistanceToTour) {
                        secondMinDistanceToTour = minDistanceToTour;
                        minDistanceToTour = distance;
                    } else if (distance < secondMinDistanceToTour) {
                        secondMinDistanceToTour = distance;
                    }
                }

                int bestMetric = minDistanceToTour + candidate.cost;
                int secondBestMetric = secondMinDistanceToTour + candidate.cost;
                int regret = secondBestMetric - bestMetric;

                if (regret > maxRegret) {
                    maxRegret = regret;
                    bestCandidateToAdd = candidate;
                }
            }

            // Step 2: Find best position to insert the selected candidate
            if (bestCandidateToAdd != null) {
                int bestPosition = -1;
                int minIncrease = Integer.MAX_VALUE;

                for (int pos = 0; pos < order.size(); pos++) {
                    int prevNodeId = order.get(pos);
                    int nextNodeId = order.get((pos + 1) % order.size());

                    int distPrevToNext = instance.distanceMatrix[prevNodeId][nextNodeId];
                    int distPrevToCandidate = instance.distanceMatrix[prevNodeId][bestCandidateToAdd.id];
                    int distCandidateToNext = instance.distanceMatrix[bestCandidateToAdd.id][nextNodeId];

                    int distanceIncrease = distPrevToCandidate + distCandidateToNext - distPrevToNext;
                    int objectiveIncrease = distanceIncrease + bestCandidateToAdd.cost;

                    if (objectiveIncrease < minIncrease) {
                        minIncrease = objectiveIncrease;
                        bestPosition = pos + 1;
                    }
                }

                selected.add(bestPosition, bestCandidateToAdd);
                order.add(bestPosition, bestCandidateToAdd.id);
                remaining.remove(bestCandidateToAdd);
            } else {
                break;
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

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selected, order, totalCost, totalDistance, endTime - startTime);
    }


    public Solution greedyWeightedRegretNearestNeighbor(Instance instance, Node startNode, double weightRegret, double weightObjective) {
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
            Node bestCandidateToAdd = null;
            double maxWeightedScore = Double.NEGATIVE_INFINITY;

            for (Node candidate : remaining) {
                int minDistanceToTour = Integer.MAX_VALUE;
                int secondMinDistanceToTour = Integer.MAX_VALUE;

                for (Node inTour : selected) {
                    int distance = instance.distanceMatrix[inTour.id][candidate.id];
                    if (distance < minDistanceToTour) {
                        secondMinDistanceToTour = minDistanceToTour;
                        minDistanceToTour = distance;
                    } else if (distance < secondMinDistanceToTour) {
                        secondMinDistanceToTour = distance;
                    }
                }

                int bestMetric = minDistanceToTour + candidate.cost;
                int secondBestMetric = secondMinDistanceToTour + candidate.cost;
                int regret = secondBestMetric - bestMetric;

                double weightedScore = weightRegret * regret - weightObjective * bestMetric;

                if (weightedScore > maxWeightedScore) {
                    maxWeightedScore = weightedScore;
                    bestCandidateToAdd = candidate;
                }
            }

            if (bestCandidateToAdd != null) {
                int bestPosition = -1;
                int minIncrease = Integer.MAX_VALUE;

                for (int pos = 0; pos < order.size(); pos++) {
                    int prevNodeId = order.get(pos);
                    int nextNodeId = order.get((pos + 1) % order.size());

                    int distPrevToNext = instance.distanceMatrix[prevNodeId][nextNodeId];
                    int distPrevToCandidate = instance.distanceMatrix[prevNodeId][bestCandidateToAdd.id];
                    int distCandidateToNext = instance.distanceMatrix[bestCandidateToAdd.id][nextNodeId];

                    int distanceIncrease = distPrevToCandidate + distCandidateToNext - distPrevToNext;
                    int objectiveIncrease = distanceIncrease + bestCandidateToAdd.cost;

                    if (objectiveIncrease < minIncrease) {
                        minIncrease = objectiveIncrease;
                        bestPosition = pos + 1;
                    }
                }

                selected.add(bestPosition, bestCandidateToAdd);
                order.add(bestPosition, bestCandidateToAdd.id);
                remaining.remove(bestCandidateToAdd);
            } else {
                break;
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

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selected, order, totalCost, totalDistance, endTime - startTime);
    }

}