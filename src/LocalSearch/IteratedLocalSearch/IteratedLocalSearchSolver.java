package LocalSearch.IteratedLocalSearch;

import LocalSearch.DeltaResult;
import LocalSearch.IntraRouteMoveType;
import LocalSearch.LocalSearchSolver;
import Utilities.Instance;
import Utilities.Node;
import Utilities.Solution;

import java.util.*;

public class IteratedLocalSearchSolver extends LocalSearchSolver {

    private final Random random;

    public IteratedLocalSearchSolver() {
        super();
        this.random = new Random();
    }

    public Solution iteratedLocalSearch(Instance instance, int numIterations, long timePerIteration) {
        long totalStartTime = System.currentTimeMillis();

        Solution initialSolution = generateRandomSolution(instance);
        Solution bestSolution = steepestLocalSearchFromSolution(instance, initialSolution, timePerIteration);
        System.out.printf("    Iteration 0: Cost = %d, Runtime = %dms (Current Best)\n", bestSolution.totalCost, bestSolution.totalRunningTime);
        Solution currentSolution = bestSolution;


        for (int i = 1; i < numIterations; i++) {
            Solution perturbedSolution = perturb(currentSolution, instance, 4);
            Solution newLocalOptimum = steepestLocalSearchFromSolution(instance, perturbedSolution, timePerIteration);

            boolean isNewBest = newLocalOptimum.totalCost < bestSolution.totalCost;
            System.out.printf("    Iteration %d: Cost = %d, Runtime = %dms%s\n",
                    i, newLocalOptimum.totalCost, newLocalOptimum.totalRunningTime, isNewBest ? " (New Best)" : "");

            if (isNewBest) {
                bestSolution = newLocalOptimum;
                currentSolution = newLocalOptimum;
            }
        }

        bestSolution.totalRunningTime = (int) (System.currentTimeMillis() - totalStartTime);
        bestSolution.localSearchRuns = numIterations;
        return bestSolution;
    }

    /**
     * Performs a steepest descent local search with a time limit.
     */
    private Solution steepestLocalSearchFromSolution(Instance instance, Solution startingSolution, long stoppingTime) {
        long startTime = System.currentTimeMillis();

        List<Node> selectedNodes = new ArrayList<>(startingSolution.selectedNodes);
        List<Integer> cycle = new ArrayList<>(startingSolution.cycle);
        Set<Integer> selectedIds = new HashSet<>();
        for (Node n : selectedNodes) selectedIds.add(n.id);

        int currentCost = startingSolution.totalCost;
        int currentDistance = startingSolution.totalDistance;

        boolean improved = true;
        while (improved) {
            if (System.currentTimeMillis() - startTime > stoppingTime) {
                break; // Stop if the time limit for this iteration is exceeded
            }

            improved = false;
            int bestDelta = 0;
            String bestMoveType = null;
            int[] bestMove = null;
            int bestDistanceDelta = 0;

            // Evaluate all intra-route moves (EDGE_EXCHANGE)
            List<int[]> intraMoves = generateIntraMoves(cycle.size(), IntraRouteMoveType.EDGE_EXCHANGE);
            for (int[] move : intraMoves) {
                int delta = calculateIntraDelta(instance, cycle, move, IntraRouteMoveType.EDGE_EXCHANGE);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    bestMoveType = "INTRA";
                    bestMove = move;
                }
            }

            // Evaluate all inter-route moves
            List<int[]> interMoves = generateInterMoves(instance, selectedIds);
            for (int[] move : interMoves) {
                DeltaResult deltaResult = calculateInterDeltaDetailed(instance, cycle, selectedNodes, move[0], move[1]);
                if (deltaResult.totalDelta < bestDelta) {
                    bestDelta = deltaResult.totalDelta;
                    bestDistanceDelta = deltaResult.distanceDelta;
                    bestMoveType = "INTER";
                    bestMove = move;
                }
            }

            if (bestDelta < 0) {
                improved = true;
                if ("INTRA".equals(bestMoveType)) {
                    applyIntraMove(cycle, bestMove, IntraRouteMoveType.EDGE_EXCHANGE);
                    currentDistance += bestDelta;
                    currentCost += bestDelta;
                } else { // INTER
                    applyInterMove(instance, selectedNodes, cycle, selectedIds, bestMove[0], bestMove[1]);
                    currentDistance += bestDistanceDelta;
                    currentCost += bestDelta;
                }
            }
        }

        long endTime = System.currentTimeMillis();
        return new Solution(selectedNodes, cycle, currentCost, currentDistance, (int)(endTime - startTime));
    }

    private Solution perturb(Solution solution, Instance instance, int strength) {
        return perturbHybrid(solution, instance, strength);
    }

    private Solution perturbHybrid(Solution solution, Instance instance, int strength) {
        List<Node> selectedNodes = new ArrayList<>(solution.selectedNodes);
        Set<Integer> selectedIds = new HashSet<>();
        for (Node n : selectedNodes) selectedIds.add(n.id);

        List<Node> unselectedNodes = new ArrayList<>();
        for (Node node : instance.nodes) {
            if (!selectedIds.contains(node.id)) {
                unselectedNodes.add(node);
            }
        }

        if (!unselectedNodes.isEmpty()) {
            int nodeReplacements = Math.max(1, strength / 2);
            nodeReplacements = Math.min(nodeReplacements, Math.min(selectedNodes.size(), unselectedNodes.size()));

            for (int i = 0; i < nodeReplacements; i++) {
                int removeIdx = random.nextInt(selectedNodes.size());
                int addIdx = random.nextInt(unselectedNodes.size());
                Node removed = selectedNodes.remove(removeIdx);
                Node added = unselectedNodes.remove(addIdx);
                selectedNodes.add(added);
                unselectedNodes.add(removed);
                selectedIds.remove(removed.id);
                selectedIds.add(added.id);
            }
        }

        List<Integer> newCycle = new ArrayList<>();
        for (Node n : selectedNodes) newCycle.add(n.id);
        java.util.Collections.shuffle(newCycle, random);

        int swaps = strength;
        for (int i = 0; i < swaps && newCycle.size() >= 2; i++) {
            int pos1 = random.nextInt(newCycle.size());
            int pos2 = random.nextInt(newCycle.size());
            while (pos1 == pos2) {
                pos2 = random.nextInt(newCycle.size());
            }
            Collections.swap(newCycle, pos1, pos2);
        }

        int newTotalDistance = calculateTotalDistance(instance, newCycle);
        int totalNodeCost = selectedNodes.stream().mapToInt(node -> node.cost).sum();
        int newTotalCost = newTotalDistance + totalNodeCost;

        return new Solution(selectedNodes, newCycle, newTotalCost, newTotalDistance, 0);
    }
}
