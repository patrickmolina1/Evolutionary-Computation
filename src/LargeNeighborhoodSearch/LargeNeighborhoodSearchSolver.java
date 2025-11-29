package LargeNeighborhoodSearch;

import LocalSearch.DeltaResult;
import LocalSearch.IntraRouteMoveType;
import LocalSearch.LocalSearchCandidateMoves.LocalSearchCandidateMovesSolver;
import Utilities.Instance;
import Utilities.Node;
import Utilities.Solution;

import java.util.*;

public class LargeNeighborhoodSearchSolver extends LocalSearchCandidateMovesSolver {

    private static final double DESTROY_PERCENTAGE = 0.30;
    private static final int TIME_LIMIT_MS = 500;
    private static final int NUM_CANDIDATES = 10;

    public LargeNeighborhoodSearchSolver() {

    }

    public Solution steepestLocalSearch(Instance instance, Solution startingSolution, IntraRouteMoveType intraRouteMoveType) {
        int startTime = (int) System.currentTimeMillis();

        Map<Integer, Set<Integer>> candidateEdges = buildCandidateEdges(instance, NUM_CANDIDATES);

        // DO NOT GENERATE RANDOM HERE. USE INPUT.
        Solution currentSolution = startingSolution;

        List<Node> selectedNodes = new ArrayList<>(startingSolution.selectedNodes);
        List<Integer> cycle = new ArrayList<>(startingSolution.cycle);
        Set<Integer> selectedIds = new HashSet<>();
        for (Node n : selectedNodes) selectedIds.add(n.id);

        int currentCost = startingSolution.totalCost;
        int currentDistance = startingSolution.totalDistance;
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


    public Solution runLNS_WithLS(Instance instance, IntraRouteMoveType intraType) {
        int numMainLoop = 0;
        long endTime = System.currentTimeMillis() + TIME_LIMIT_MS;

        // 1. Generate Initial Solution
        long st = System.currentTimeMillis();
        Solution bestSolution = generateRandomSolution(instance);

        // 2. Initial LS
        bestSolution = steepestLocalSearch(instance, bestSolution, intraType);

        Solution currentSolution = bestSolution;

        while (System.currentTimeMillis() < endTime) {
            // 3.1 Destroy
            Solution perturbedCandidate = destroy(instance, currentSolution);

            // 3.2 Repair (Greedy Regret)
            repair(instance, perturbedCandidate);

            // 3.3 Local Search (Applied to the repaired solution)
            perturbedCandidate = steepestLocalSearch(instance, perturbedCandidate, intraType);

            // 3.4 Acceptance (Strict Improvement)
            if (perturbedCandidate.totalCost < currentSolution.totalCost) {
                currentSolution = perturbedCandidate;

                // Update global best
                if (currentSolution.totalCost < bestSolution.totalCost) {
                    bestSolution = currentSolution;
                }
            }
            numMainLoop++;
        }

        System.out.println("LNS with LS completed " + numMainLoop + " main iterations.");
        long et = System.currentTimeMillis();
        bestSolution.totalRunningTime = (int) (et - st);

        return bestSolution;
    }

    // --- 2. LNS WITHOUT LOCAL SEARCH ---
    public Solution runLNS_WithoutLS(Instance instance) {
        long endTime = System.currentTimeMillis() + TIME_LIMIT_MS;

        // 1. Generate Initial Solution
        long st = System.currentTimeMillis();
        Solution bestSolution = generateRandomSolution(instance);

        // Initial LS is still required by prompt
        bestSolution = steepestLocalSearch(instance, bestSolution, null);

        Solution currentSolution = bestSolution;

        while (System.currentTimeMillis() < endTime) {
            // 3.1 Destroy
            Solution perturbedCandidate = destroy(instance, currentSolution);

            // 3.2 Repair (Greedy Regret)
            repair(instance, perturbedCandidate);

            // 3.3 NO Local Search here

            // 3.4 Acceptance
            if (perturbedCandidate.totalCost < currentSolution.totalCost) {
                currentSolution = perturbedCandidate;

                if (currentSolution.totalCost < bestSolution.totalCost) {
                    bestSolution = currentSolution;
                }
            }
        }
        long et = System.currentTimeMillis();
        bestSolution.totalRunningTime = (int) (et - st);
        return bestSolution;
    }

    private Solution destroy(Instance instance, Solution sol) {
        // Deep copy to avoid modifying the original during calculation
        List<Node> newSelectedNodes = new ArrayList<>(sol.selectedNodes);
        List<Integer> newCycle = new ArrayList<>(sol.cycle);
        Solution partialSol = new Solution(newSelectedNodes, newCycle, sol.totalCost, sol.totalDistance, sol.totalRunningTime);

        int targetRemovalCount = (int) (sol.selectedNodes.size() * DESTROY_PERCENTAGE);
        if (targetRemovalCount < 1) targetRemovalCount = 1;

        Random rand = new Random();
        List<Integer> cycle = partialSol.cycle;

        // Strategy: Remove nodes associated with the longest edges (Heuristic),
        // but mix in randomness so it's not deterministic.

        Set<Integer> nodesToRemove = new HashSet<>();

        // Calculate average edge weight
        double totalEdgeWeight = 0;
        int edgeCount = 0;
        for (int i = 0; i < cycle.size(); i++) {
            int u = cycle.get(i);
            int v = cycle.get((i + 1) % cycle.size());
            totalEdgeWeight += instance.distanceMatrix[u][v];
            edgeCount++;
        }
        double averageEdgeWeight = totalEdgeWeight / edgeCount;

        // We will look at edges (i, i+1) and pick nodes to remove based on edge weight
        while (nodesToRemove.size() < targetRemovalCount) {
            // Pick a random index
            int idx = rand.nextInt(cycle.size());
            int nextIdx = (idx + 1) % cycle.size();

            int u = cycle.get(idx);
            int v = cycle.get(nextIdx);

            double dist = instance.distanceMatrix[u][v];

            // Heuristic probability:
            // If edge is long (relative to average), higher chance to remove 'u' or 'v'.
            // Simple approach: standard randomness, but reject "short" edges occasionally
            // to bias towards long edges.

            // If this is a very short edge (better than avg), 50% chance we skip removing it
            // This concentrates destruction on "bad" parts of the graph.
            if (dist < averageEdgeWeight && rand.nextDouble() > 0.5) {
                continue;
            }

            // Remove one of the nodes attached to this edge
            nodesToRemove.add(rand.nextBoolean() ? u : v);
        }

        // Apply removal
        // Note: Removing from Lists while iterating is tricky, so we rebuild.
        List<Integer> finalCycle = new ArrayList<>();
        List<Node> finalSelectedNodes = new ArrayList<>();

        for (int nodeId : cycle) {
            if (!nodesToRemove.contains(nodeId)) {
                finalCycle.add(nodeId);
            }
        }

        for (Node n : partialSol.selectedNodes) {
            if (!nodesToRemove.contains(n.id)) {
                finalSelectedNodes.add(n);
            }
        }

        partialSol.cycle = finalCycle;
        partialSol.selectedNodes = finalSelectedNodes;

        // Recalculate cost/dist for the partial solution
        int totalDist = 0;
        for (int i = 0; i < finalCycle.size(); i++) {
            int u = finalCycle.get(i);
            int v = finalCycle.get((i + 1) % finalCycle.size());
            totalDist += instance.distanceMatrix[u][v];
        }
        int totalCostCalc = totalDist;
        for (Node n : finalSelectedNodes) {
            totalCostCalc += n.cost;
        }
        partialSol.totalDistance = totalDist;
        partialSol.totalCost = totalCostCalc;

        return partialSol;
    }

    // ---------------------------------------------------------
    // --- REPAIR OPERATOR (2-Regret Heuristic) ---
    // ---------------------------------------------------------
    private void repair(Instance instance, Solution sol) {
        int targetSize = instance.nodes.size() / 2; // Assuming problem requires selecting 50% of nodes

        // Identify unselected nodes
        Set<Integer> currentIds = new HashSet<>(sol.cycle);
        List<Integer> unselected = new ArrayList<>();
        for (int i = 0; i < instance.nodes.size(); i++) {
            if (!currentIds.contains(i)) unselected.add(i);
        }

        // Loop until we reach the target size (Greedy Cycle Construction)
        while (sol.cycle.size() < targetSize && !unselected.isEmpty()) {

            int bestNodeId = -1;
            int bestInsertionIndex = -1;
            int maxRegretVal = -1;

            // Calculate Regret for every unselected candidate
            for (int candidateId : unselected) {

                // Find Best and Second Best insertion costs for this candidate
                int bestCost = Integer.MAX_VALUE;
                int secondBestCost = Integer.MAX_VALUE;
                int currentBestIndex = -1;

                // Try inserting at every position in the current cycle
                for (int i = 0; i < sol.cycle.size(); i++) {
                    int u = sol.cycle.get(i);
                    int v = sol.cycle.get((i + 1) % sol.cycle.size());

                    // Cost diff = dist(u, c) + dist(c, v) - dist(u, v) + nodeCost(c)
                    int addedDist = instance.distanceMatrix[u][candidateId] + instance.distanceMatrix[candidateId][v] - instance.distanceMatrix[u][v];
                    int addedCost = addedDist + instance.nodes.get(candidateId).cost;

                    if (addedCost < bestCost) {
                        secondBestCost = bestCost;
                        bestCost = addedCost;
                        currentBestIndex = i;
                    } else if (addedCost < secondBestCost) {
                        secondBestCost = addedCost;
                    }
                }

                // Calculate Regret
                int regret = secondBestCost - bestCost;

                // Weighted Regret (Optional: add a weight factor for the cost itself)
                // Score = Regret. We want to insert the node with HIGHEST regret first.
                if (regret > maxRegretVal) {
                    maxRegretVal = regret;
                    bestNodeId = candidateId;
                    bestInsertionIndex = currentBestIndex;
                }
            }

            // Insert the winner
            if (bestNodeId != -1) {
                sol.cycle.add(bestInsertionIndex + 1, bestNodeId);
                sol.selectedNodes.add(instance.nodes.get(bestNodeId));

                // Remove from unselected list
                // (In efficient implementations, use a fast remove structure or swap)
                unselected.remove((Integer) bestNodeId);
            } else {
                break; // Should not happen if unselected is not empty
            }
        }

        // Final recalculate to ensure data integrity
        int totalDist = 0;
        for (int i = 0; i < sol.cycle.size(); i++) {
            int u = sol.cycle.get(i);
            int v = sol.cycle.get((i + 1) % sol.cycle.size());
            totalDist += instance.distanceMatrix[u][v];
        }
        int totalCostCalc = totalDist;
        for (Node n : sol.selectedNodes) {
            totalCostCalc += n.cost;
        }
        sol.totalDistance = totalDist;
        sol.totalCost = totalCostCalc;
    }


}
