package LargeNeighborhoodSearch;

import LocalSearch.DeltaResult;
import LocalSearch.IntraRouteMoveType;
import LocalSearch.LocalSearchCandidateMoves.LocalSearchCandidateMovesSolver;
import LocalSearch.StartingSolutionType;
import Utilities.Instance;
import Utilities.Node;
import Utilities.Solution;

import java.util.*;

public class LargeNeighborhoodSearchSolver extends LocalSearchCandidateMovesSolver {

    private static final double DESTROY_PERCENTAGE = 0.30;
    private static final int NUM_CANDIDATES = 10;

    public LargeNeighborhoodSearchSolver() {

    }

    public Solution steepestLocalSearch(Instance instance, Solution startingSolution, IntraRouteMoveType intraRouteMoveType){
        int startTime = (int) System.currentTimeMillis();


        // Make copies of the solution data
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
            String bestMoveType = null;
            int[] bestMove = null;
            int bestDistanceDelta = 0;

            // Evaluate all intra-route moves
            List<int[]> intraMoves = generateIntraMoves(cycle.size(), intraRouteMoveType);
            for (int[] move : intraMoves) {
                int delta = calculateIntraDelta(instance, cycle, move, intraRouteMoveType);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    bestMoveType = "INTRA";
                    bestMove = move;
                }
            }

            // Evaluate all inter-route moves
            List<int[]> interMoves = generateInterMoves(instance, selectedIds);
            for (int[] move : interMoves) {
                int selectedNodeId = move[0];
                int nonSelectedNodeId = move[1];

                DeltaResult deltaResult = calculateInterDeltaDetailed(instance, cycle, selectedNodes,
                        selectedNodeId, nonSelectedNodeId);
                if (deltaResult.totalDelta < bestDelta) {
                    bestDelta = deltaResult.totalDelta;
                    bestDistanceDelta = deltaResult.distanceDelta;
                    bestMoveType = "INTER";
                    bestMove = move;
                }
            }

            // Apply best move if improving
            if (bestDelta < 0) {
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




    public Solution runLNS_WithLS(Instance instance, IntraRouteMoveType intraType, int timeLimitMS) {
        int numMainLoop = 0;
        long endTime = System.currentTimeMillis() + timeLimitMS;

        // 1. Generate Initial Solution
        long st = System.currentTimeMillis();
        Solution bestSolution = generateRandomSolution(instance);

        // 2. Initial LS
        bestSolution = steepestLocalSearch(instance, bestSolution, intraType);

        Solution currentSolution = bestSolution;

        while (System.currentTimeMillis() < endTime) {
            // 3.1 Destroy
            Solution perturbedCandidate = destroyHybrid(instance, currentSolution);

            // 3.2 Repair (Greedy Regret)
            repairWeighted(instance, perturbedCandidate, 0.5, 0.5);

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

        bestSolution.iterations = numMainLoop;
        return bestSolution;
    }

    // --- 2. LNS WITHOUT LOCAL SEARCH ---
    public Solution runLNS_WithoutLS(Instance instance, int timeLimitMS) {
        int numMainLoop = 0;

        long endTime = System.currentTimeMillis() + timeLimitMS;

        // 1. Generate Initial Solution
        long st = System.currentTimeMillis();
        Solution bestSolution = generateRandomSolution(instance);

        // Initial LS is still required by prompt
        bestSolution = steepestLocalSearch(instance, bestSolution, null);

        Solution currentSolution = bestSolution;

        while (System.currentTimeMillis() < endTime) {
            // 3.1 Destroy
            Solution perturbedCandidate = destroyHybrid(instance, currentSolution);

            // 3.2 Repair (Greedy Regret)
            repairWeighted(instance, perturbedCandidate, 0.5, 0.5);

            // 3.3 NO Local Search here

            // 3.4 Acceptance
            if (perturbedCandidate.totalCost < currentSolution.totalCost) {
                currentSolution = perturbedCandidate;

                if (currentSolution.totalCost < bestSolution.totalCost) {
                    bestSolution = currentSolution;
                }
            }

            numMainLoop++;
        }

        System.out.println("LNS without LS completed " + numMainLoop + " main iterations.");
        long et = System.currentTimeMillis();
        bestSolution.totalRunningTime = (int) (et - st);

        bestSolution.iterations = numMainLoop;
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
//    private void repair(Instance instance, Solution sol) {
//        int targetSize = instance.nodes.size() / 2; // Assuming problem requires selecting 50% of nodes
//
//        // Identify unselected nodes
//        Set<Integer> currentIds = new HashSet<>(sol.cycle);
//        List<Integer> unselected = new ArrayList<>();
//        for (int i = 0; i < instance.nodes.size(); i++) {
//            if (!currentIds.contains(i)) unselected.add(i);
//        }
//
//        // Loop until we reach the target size (Greedy Cycle Construction)
//        while (sol.cycle.size() < targetSize && !unselected.isEmpty()) {
//
//            int bestNodeId = -1;
//            int bestInsertionIndex = -1;
//            int maxRegretVal = -1;
//
//            // Calculate Regret for every unselected candidate
//            for (int candidateId : unselected) {
//
//                // Find Best and Second Best insertion costs for this candidate
//                int bestCost = Integer.MAX_VALUE;
//                int secondBestCost = Integer.MAX_VALUE;
//                int currentBestIndex = -1;
//
//                // Try inserting at every position in the current cycle
//                for (int i = 0; i < sol.cycle.size(); i++) {
//                    int u = sol.cycle.get(i);
//                    int v = sol.cycle.get((i + 1) % sol.cycle.size());
//
//                    // Cost diff = dist(u, c) + dist(c, v) - dist(u, v) + nodeCost(c)
//                    int addedDist = instance.distanceMatrix[u][candidateId] + instance.distanceMatrix[candidateId][v] - instance.distanceMatrix[u][v];
//                    int addedCost = addedDist + instance.nodes.get(candidateId).cost;
//
//                    if (addedCost < bestCost) {
//                        secondBestCost = bestCost;
//                        bestCost = addedCost;
//                        currentBestIndex = i;
//                    } else if (addedCost < secondBestCost) {
//                        secondBestCost = addedCost;
//                    }
//                }
//
//                // Calculate Regret
//                int regret = secondBestCost - bestCost;
//
//                // Weighted Regret (Optional: add a weight factor for the cost itself)
//                // Score = Regret. We want to insert the node with HIGHEST regret first.
//                if (regret > maxRegretVal) {
//                    maxRegretVal = regret;
//                    bestNodeId = candidateId;
//                    bestInsertionIndex = currentBestIndex;
//                }
//            }
//
//            // Insert the winner
//            if (bestNodeId != -1) {
//                sol.cycle.add(bestInsertionIndex + 1, bestNodeId);
//                sol.selectedNodes.add(instance.nodes.get(bestNodeId));
//
//                // Remove from unselected list
//                // (In efficient implementations, use a fast remove structure or swap)
//                unselected.remove((Integer) bestNodeId);
//            } else {
//                break; // Should not happen if unselected is not empty
//            }
//        }
//
//        // Final recalculate to ensure data integrity
//        int totalDist = 0;
//        for (int i = 0; i < sol.cycle.size(); i++) {
//            int u = sol.cycle.get(i);
//            int v = sol.cycle.get((i + 1) % sol.cycle.size());
//            totalDist += instance.distanceMatrix[u][v];
//        }
//        int totalCostCalc = totalDist;
//        for (Node n : sol.selectedNodes) {
//            totalCostCalc += n.cost;
//        }
//        sol.totalDistance = totalDist;
//        sol.totalCost = totalCostCalc;
//    }

    private void repairWeighted(Instance instance, Solution sol, double weightRegret, double weightObjective) {
        int targetSize = instance.nodes.size() / 2;

        Set<Integer> currentIds = new HashSet<>(sol.cycle);
        List<Integer> unselected = new ArrayList<>();
        for (int i = 0; i < instance.nodes.size(); i++) {
            if (!currentIds.contains(i)) unselected.add(i);
        }

        while (sol.cycle.size() < targetSize && !unselected.isEmpty()) {
            int bestNodeId = -1;
            int bestInsertionIndex = -1;
            double maxWeightedScore = Double.NEGATIVE_INFINITY;

            for (int candidateId : unselected) {
                int bestCost = Integer.MAX_VALUE;
                int secondBestCost = Integer.MAX_VALUE;
                int currentBestIndex = -1;

                for (int i = 0; i < sol.cycle.size(); i++) {
                    int u = sol.cycle.get(i);
                    int v = sol.cycle.get((i + 1) % sol.cycle.size());

                    int addedDist = instance.distanceMatrix[u][candidateId]
                            + instance.distanceMatrix[candidateId][v]
                            - instance.distanceMatrix[u][v];
                    int addedCost = addedDist + instance.nodes.get(candidateId).cost;

                    if (addedCost < bestCost) {
                        secondBestCost = bestCost;
                        bestCost = addedCost;
                        currentBestIndex = i;
                    } else if (addedCost < secondBestCost) {
                        secondBestCost = addedCost;
                    }
                }

                int regret = secondBestCost - bestCost;

                // Weighted score: balance between regret (diversification) and cost (quality)
                double weightedScore = weightRegret * regret - weightObjective * bestCost;

                if (weightedScore > maxWeightedScore) {
                    maxWeightedScore = weightedScore;
                    bestNodeId = candidateId;
                    bestInsertionIndex = currentBestIndex;
                }
            }

            if (bestNodeId != -1) {
                sol.cycle.add(bestInsertionIndex + 1, bestNodeId);
                sol.selectedNodes.add(instance.nodes.get(bestNodeId));
                unselected.remove((Integer) bestNodeId);
            } else {
                break;
            }
        }

        recalculateCosts(instance, sol);
    }


    private Solution destroySubpath(Instance instance, Solution sol) {
        List<Node> newSelectedNodes = new ArrayList<>(sol.selectedNodes);
        List<Integer> newCycle = new ArrayList<>(sol.cycle);
        Solution partialSol = new Solution(newSelectedNodes, newCycle, sol.totalCost, sol.totalDistance, sol.totalRunningTime);

        int targetRemovalCount = (int) (sol.selectedNodes.size() * DESTROY_PERCENTAGE);
        if (targetRemovalCount < 1) targetRemovalCount = 1;

        Random rand = new Random();
        List<Integer> cycle = partialSol.cycle;

        // Pick a random starting position
        int startIdx = rand.nextInt(cycle.size());

        // Remove a contiguous subpath
        Set<Integer> nodesToRemove = new HashSet<>();
        for (int i = 0; i < targetRemovalCount; i++) {
            int idx = (startIdx + i) % cycle.size();
            nodesToRemove.add(cycle.get(idx));
        }

        // Rebuild cycle and selected nodes
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

        // Recalculate costs
        recalculateCosts(instance, partialSol);
        return partialSol;
    }


    private Solution destroyMultipleSubpaths(Instance instance, Solution sol) {
        List<Node> newSelectedNodes = new ArrayList<>(sol.selectedNodes);
        List<Integer> newCycle = new ArrayList<>(sol.cycle);
        Solution partialSol = new Solution(newSelectedNodes, newCycle, sol.totalCost, sol.totalDistance, sol.totalRunningTime);

        int targetRemovalCount = (int) (sol.selectedNodes.size() * DESTROY_PERCENTAGE);
        if (targetRemovalCount < 1) targetRemovalCount = 1;

        Random rand = new Random();
        List<Integer> cycle = partialSol.cycle;
        Set<Integer> nodesToRemove = new HashSet<>();

        // Number of subpaths (2-4 segments)
        int numSubpaths = 2 + rand.nextInt(3); // 2, 3, or 4 subpaths
        int nodesPerSubpath = Math.max(1, targetRemovalCount / numSubpaths);

        for (int s = 0; s < numSubpaths && nodesToRemove.size() < targetRemovalCount; s++) {
            // Random starting position for this subpath
            int startIdx = rand.nextInt(cycle.size());

            // Remove a segment
            for (int i = 0; i < nodesPerSubpath && nodesToRemove.size() < targetRemovalCount; i++) {
                int idx = (startIdx + i) % cycle.size();
                nodesToRemove.add(cycle.get(idx));
            }
        }

        // Rebuild cycle and selected nodes
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

        recalculateCosts(instance, partialSol);
        return partialSol;
    }

    private Solution destroyHybrid(Instance instance, Solution sol) {
        Random rand = new Random();
        double strategy = rand.nextDouble();

        if (strategy < 0.4) {
            // 40% chance: scattered removal (your current method)
            return destroy(instance, sol);
        } else if (strategy < 0.7) {
            // 30% chance: single subpath
            return destroySubpath(instance, sol);
        } else {
            // 30% chance: multiple subpaths
            return destroyMultipleSubpaths(instance, sol);
        }
    }
    private void recalculateCosts(Instance instance, Solution sol) {
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
