package LocalSearch.LocalSearchCandidateMoves;

import LocalSearch.DeltaResult;
import LocalSearch.IntraRouteMoveType;
import LocalSearch.LocalSearchSolver;
import LocalSearch.StartingSolutionType;
import Utilities.*;
import java.util.*;

public class LocalSearchCandidateMovesSolver extends LocalSearchSolver {

    private static final int NUM_CANDIDATES = 10; // Can be tuned experimentally


    LocalSearchCandidateMovesSolver(){

    }

    @Override
    public Solution steepestLocalSearch(Instance instance, StartingSolutionType startingSolutionType, IntraRouteMoveType intraRouteMoveType) {
        int startTime = (int) System.currentTimeMillis();

        // --- Step 1: Build candidate edges ---
        Map<Integer, Set<Integer>> candidateEdges = buildCandidateEdges(instance, NUM_CANDIDATES);

        // --- Step 2: Generate random starting solution ---
        Solution currentSolution = generateRandomSolution(instance);
        List<Node> selectedNodes = new ArrayList<>(currentSolution.selectedNodes);
        List<Integer> cycle = new ArrayList<>(currentSolution.cycle);
        Set<Integer> selectedIds = new HashSet<>();
        for (Node n : selectedNodes) selectedIds.add(n.id);
        Map<Integer, Integer> nodeIndexMap = new HashMap<>();
        for (int i = 0; i < cycle.size(); i++) {
            nodeIndexMap.put(cycle.get(i), i);
        }

        int currentCost = currentSolution.totalCost;
        int currentDistance = currentSolution.totalDistance;
        boolean improved = true;

        // --- Step 3: Local Search Loop ---
        while (improved) {
            improved = false;
            int bestDelta = 0;
            int bestDistanceDelta = 0;
            String bestMoveType = null;
            int[] bestMove = null;

            // --- Step 4 & 5: Iterate over candidate moves ---
            for (int nodeId : selectedIds) {
                Set<Integer> neighbors = candidateEdges.get(nodeId);
                if (neighbors == null) continue;

                for (int neighborId : neighbors) {
                    if (selectedIds.contains(neighborId)) {
                        // --- Potential INTRA-route move ---
                        // This means (nodeId, neighborId) is a candidate edge.
                        // We evaluate the 2-opt move that creates this edge.
                        int i = nodeIndexMap.get(nodeId);
                        int j = nodeIndexMap.get(neighborId);

                        // To create edge (i, j), we must break (i, i+1) and (j-1, j)
                        // This is one of two possible 2-opt moves. We check the one that is not a simple reversal.
                        if (j != (i + 1) % cycle.size()) {
                            int[] move = new int[]{i, j};
                            int delta = calculateIntraDelta(instance, cycle, move, intraRouteMoveType);
                            if (delta < bestDelta) {
                                bestDelta = delta;
                                bestMove = move;
                                bestMoveType = "INTRA";
                            }
                        }
                    } else {
                        // --- Potential INTER-route move ---
                        // Swap selected `nodeId` with non-selected `neighborId`
                        DeltaResult deltaResult = calculateInterDeltaDetailed(instance, cycle, selectedNodes,
                                nodeId, neighborId);

                        if (deltaResult.totalDelta < bestDelta) {
                            bestDelta = deltaResult.totalDelta;
                            bestDistanceDelta = deltaResult.distanceDelta;
                            bestMove = new int[]{nodeId, neighborId};
                            bestMoveType = "INTER";
                        }
                    }
                }
            }

            // --- Step 6: Apply best move ---
            if (bestDelta < 0 && bestMoveType != null) {
                improved = true;

                if (bestMoveType.equals("INTRA")) {
                    applyIntraMove(cycle, bestMove, intraRouteMoveType);
                    currentDistance += bestDelta;
                    currentCost += bestDelta;
                    // Rebuild index map after cycle modification
                    nodeIndexMap.clear();
                    for (int i = 0; i < cycle.size(); i++) {
                        nodeIndexMap.put(cycle.get(i), i);
                    }
                } else {
                    int selectedNodeId = bestMove[0];
                    int nonSelectedNodeId = bestMove[1];
                    applyInterMove(instance, selectedNodes, cycle, selectedIds, selectedNodeId, nonSelectedNodeId);
                    currentDistance += bestDistanceDelta;
                    currentCost += bestDelta;
                    // Rebuild index map after cycle modification
                    nodeIndexMap.clear();
                    for (int i = 0; i < cycle.size(); i++) {
                        nodeIndexMap.put(cycle.get(i), i);
                    }
                }
            }
        }

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selectedNodes, cycle, currentCost, currentDistance, endTime - startTime);
    }


    private Map<Integer, Set<Integer>> buildCandidateEdges(Instance instance, int k) {
        Map<Integer, Set<Integer>> candidateEdges = new HashMap<>();
        int n = instance.nodes.size();

        for (Node node : instance.nodes) {
            int id = node.id;
            List<int[]> neighbors = new ArrayList<>();

            for (Node other : instance.nodes) {
                if (node.id == other.id) continue;
                int metric = instance.distanceMatrix[node.id][other.id] + other.cost;
                neighbors.add(new int[]{other.id, metric});
            }

            // Sort by (distance + cost)
            neighbors.sort(Comparator.comparingInt(a -> a[1]));

            // Keep top k
            Set<Integer> nearest = new HashSet<>();
            for (int i = 0; i < Math.min(k, neighbors.size()); i++) {
                nearest.add(neighbors.get(i)[0]);
            }

            candidateEdges.put(id, nearest);
        }

        return candidateEdges;
    }





}
