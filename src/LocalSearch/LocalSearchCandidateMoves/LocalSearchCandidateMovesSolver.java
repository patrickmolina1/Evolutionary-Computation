package LocalSearch.LocalSearchCandidateMoves;

import LocalSearch.DeltaResult;
import LocalSearch.IntraRouteMoveType;
import LocalSearch.LocalSearchSolver;
import LocalSearch.StartingSolutionType;
import Utilities.*;
import java.util.*;
//import java.util.stream.Node;

public class LocalSearchCandidateMovesSolver extends LocalSearchSolver {

    private static final int NUM_CANDIDATES = 10; // Can be tuned experimentally


    public LocalSearchCandidateMovesSolver(){

    }

    @Override
    public Solution steepestLocalSearch(Instance instance, StartingSolutionType startingSolutionType, IntraRouteMoveType intraRouteMoveType) {
        int startTime = (int) System.currentTimeMillis();

        Map<Integer, Set<Integer>> candidateEdges = buildCandidateEdges(instance, NUM_CANDIDATES);

        Solution currentSolution = generateRandomSolution(instance);
        List<Node> selectedNodes = new ArrayList<>(currentSolution.selectedNodes);
        List<Integer> cycle = new ArrayList<>(currentSolution.cycle);
        Set<Integer> selectedIds = new HashSet<>();
        for (Node n : selectedNodes) selectedIds.add(n.id);

        int currentCost = currentSolution.totalCost;
        int currentDistance = currentSolution.totalDistance;
        boolean improved = true;

        while (improved) {
            improved = false;
            int bestDelta = 0;
            int bestDistanceDelta = 0;
            String bestMoveType = null;
            int[] bestMove = null;

            // Iterate over selected nodes
            for (int idx = 0; idx < cycle.size(); idx++) {
                int nodeId = cycle.get(idx);
                Set<Integer> neighbors = candidateEdges.get(nodeId);
                if (neighbors == null) continue;

                int prevIdx = (idx == 0) ? cycle.size() - 1 : idx - 1;
                int nextIdx = (idx == cycle.size() - 1) ? 0 : idx + 1;
                int prevNodeId = cycle.get(prevIdx);
                int nextNodeId = cycle.get(nextIdx);

                for (int neighborId : neighbors) {
                    if (selectedIds.contains(neighborId)) {
                        // --- INTRA-route candidate move ---
                        // The candidate edge (nodeId, neighborId) should be introduced
                        int neighborIdx = cycle.indexOf(neighborId);

                        // Skip if already adjacent in cycle
                        if (Math.abs(idx - neighborIdx) == 1 ||
                                (idx == 0 && neighborIdx == cycle.size() - 1) ||
                                (neighborIdx == 0 && idx == cycle.size() - 1)) {
                            continue;
                        }

                        int[] move = new int[]{idx, neighborIdx};
                        int delta = calculateIntraDelta(instance, cycle, move, intraRouteMoveType);
                        if (delta < bestDelta) {
                            bestDelta = delta;
                            bestMove = move;
                            bestMoveType = "INTRA";
                        }
                    } else {
                        // --- INTER-route candidate move ---
                        // Exchange a selected node with non-selected neighborId
                        // Two options: exchange prevNodeId or nextNodeId with neighborId
                        // This introduces edge (nodeId, neighborId)

                        DeltaResult delta1 = calculateInterDeltaDetailed(instance, cycle, selectedNodes, prevNodeId, neighborId);
                        if (delta1.totalDelta < bestDelta) {
                            bestDelta = delta1.totalDelta;
                            bestDistanceDelta = delta1.distanceDelta;
                            bestMove = new int[]{prevNodeId, neighborId};
                            bestMoveType = "INTER";
                        }

                        DeltaResult delta2 = calculateInterDeltaDetailed(instance, cycle, selectedNodes, nextNodeId, neighborId);
                        if (delta2.totalDelta < bestDelta) {
                            bestDelta = delta2.totalDelta;
                            bestDistanceDelta = delta2.distanceDelta;
                            bestMove = new int[]{nextNodeId, neighborId};
                            bestMoveType = "INTER";
                        }
                    }
                }
            }

            if (bestDelta < 0 && bestMoveType != null) {
                improved = true;

                if (bestMoveType.equals("INTRA")) {
                    applyIntraMove(cycle, bestMove, intraRouteMoveType);
                    currentDistance += bestDelta;
                    currentCost += bestDelta;
                } else {
                    int selectedNodeId = bestMove[0];
                    int nonSelectedNodeId = bestMove[1];
                    applyInterMove(instance, selectedNodes, cycle, selectedIds, selectedNodeId, nonSelectedNodeId);
                    currentDistance += bestDistanceDelta;
                    currentCost += bestDelta;
                }
            }
        }

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selectedNodes, cycle, currentCost, currentDistance, endTime - startTime);
    }



    protected Map<Integer, Set<Integer>> buildCandidateEdges(Instance instance, int k) {
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

            Set<Integer> nearest = new HashSet<>();
            for (int i = 0; i < Math.min(k, neighbors.size()); i++) {
                nearest.add(neighbors.get(i)[0]);
            }

            candidateEdges.put(id, nearest);
        }

        return candidateEdges;
    }
}
