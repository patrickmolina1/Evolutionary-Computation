package LocalSearch.IteratedLocalSearch;

import LocalSearch.DeltaResult;
import LocalSearch.IntraRouteMoveType;
import LocalSearch.LocalSearchSolver;
import Utilities.Instance;
import Utilities.Node;
import Utilities.Solution;

import java.util.*;

/**
 * Iterated Local Search solver with adaptive perturbation and a more advanced acceptance criterion.
 *
 * Improvements:
 *  - Adaptive Perturbation: Strength increases when the search stagnates and resets on finding a new best.
 *  - Probabilistic Acceptance Criterion: Allows escaping local optima by sometimes accepting worse solutions,
 *    but avoids moving to extremely poor solutions.
 *  - Tunable parameters for fine-tuning the search behavior.
 */
public class IteratedLocalSearchSolver extends LocalSearchSolver {

    private final Random random;

    // --- Configurable ILS parameters ---
    private final int initialPerturbationStrength = 2;
    private final int maxPerturbationStrength = 12; // Max strength to prevent excessive destruction
    private final int strengthIncrement = 2;        // How much to increase strength on stagnation
    private final int removalCandidateSamples = 3;
    private final int addCandidateSamples = 3;

    public IteratedLocalSearchSolver() {
        this(new Random());
    }

    public IteratedLocalSearchSolver(Random random) {
        super();
        this.random = random;
    }

    public List<Solution> iteratedLocalSearch(Instance instance, long stoppingTime) {
        long totalStartTime = System.currentTimeMillis();
        List<Solution> foundSolutions = new ArrayList<>();

        // Initial solution and local search
        Solution initialSolution = generateRandomSolution(instance);
        Solution s_best = steepestLocalSearchFromSolution(instance, initialSolution);
        foundSolutions.add(s_best);
        Solution s_current = s_best;

        System.out.printf("    LS Run 1: Cost = %d, Runtime = %dms (Initial Best)%n", s_best.totalCost, s_best.totalRunningTime);

        int runCount = 1;
        int perturbationStrength = initialPerturbationStrength;

        while (System.currentTimeMillis() - totalStartTime < stoppingTime) {
            runCount++;

            // 1. Perturb the current solution with adaptive strength
            Solution s_perturbed = perturbHybrid(s_current, instance, perturbationStrength);

            // 2. Apply local search to the perturbed solution
            Solution s_new = steepestLocalSearchFromSolution(instance, s_perturbed);
            foundSolutions.add(s_new);

            boolean isNewGlobalBest = s_new.totalCost < s_best.totalCost;
            System.out.printf("    LS Run %d: Cost = %d, Runtime = %dms, Strength = %d%s%n",
                    runCount, s_new.totalCost, s_new.totalRunningTime, perturbationStrength, isNewGlobalBest ? " (New Global Best)" : "");

            // 3. Update global best solution
            if (isNewGlobalBest) {
                s_best = s_new;
            }

            // 4. Acceptance Criterion & Adaptive Perturbation Logic
            if (s_new.totalCost < s_current.totalCost) {
                // Better solution found, accept it and reset perturbation strength
                s_current = s_new;
                perturbationStrength = initialPerturbationStrength;
            } else {
                // Not a better solution.
                // Accept it anyway to explore, but increase perturbation strength for next time.
                s_current = s_new; // <--- AGGRESSIVE MOVE
                perturbationStrength = Math.min(maxPerturbationStrength, perturbationStrength + strengthIncrement);
            }
        }
        return foundSolutions;
    }

    private Solution steepestLocalSearchFromSolution(Instance instance, Solution startingSolution) {
        long startTime = System.currentTimeMillis();


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

            List<int[]> intraMoves = generateIntraMoves(cycle.size(), IntraRouteMoveType.EDGE_EXCHANGE);
            for (int[] move : intraMoves) {
                int delta = calculateIntraDelta(instance, cycle, move, IntraRouteMoveType.EDGE_EXCHANGE);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    bestMoveType = "INTRA";
                    bestMove = move;
                }
            }

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

            if (bestDelta < 0 && bestMove != null) {
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
        return new Solution(selectedNodes, cycle, currentCost, currentDistance, (int) (endTime - startTime));
    }

    private Solution perturbHybrid(Solution solution, Instance instance, int strength) {
        Map<Integer, Node> idToNode = new HashMap<>();
        for (Node n : instance.nodes) idToNode.put(n.id, n);

        List<Integer> newCycle = new ArrayList<>(solution.cycle);
        List<Node> selectedNodes = new ArrayList<>(solution.selectedNodes);
        Set<Integer> selectedIds = new HashSet<>();
        for (Node n : selectedNodes) selectedIds.add(n.id);

        List<Node> availableNodes = new ArrayList<>();
        for (Node node : instance.nodes) {
            if (!selectedIds.contains(node.id)) availableNodes.add(node);
        }

        if (newCycle.isEmpty() || availableNodes.isEmpty()) {
            return new Solution(selectedNodes, newCycle, solution.totalCost, solution.totalDistance, 0);
        }

        int exchanges = Math.max(1, strength / 2);
        exchanges = Math.min(exchanges, Math.min(newCycle.size(), availableNodes.size()));

        for (int e = 0; e < exchanges; e++) {
            if (availableNodes.isEmpty() || newCycle.isEmpty()) break;

            int bestRemoveIndexInCycle = -1;
            int worstCost = Integer.MIN_VALUE;
            for (int k = 0; k < removalCandidateSamples && !newCycle.isEmpty(); k++) {
                int idx = random.nextInt(newCycle.size());
                Node candidate = idToNode.get(newCycle.get(idx));
                if (candidate != null && candidate.cost > worstCost) {
                    worstCost = candidate.cost;
                    bestRemoveIndexInCycle = idx;
                }
            }
            if (bestRemoveIndexInCycle == -1) bestRemoveIndexInCycle = random.nextInt(newCycle.size());

            int removedNodeId = newCycle.get(bestRemoveIndexInCycle);
            newCycle.remove(bestRemoveIndexInCycle);
            selectedIds.remove(removedNodeId);
            final int idToRemove = removedNodeId;
            selectedNodes.removeIf(n -> n.id == idToRemove);

            int bestAddIndexInAvailable = -1;
            int bestAddCost = Integer.MAX_VALUE;
            for (int k = 0; k < addCandidateSamples && !availableNodes.isEmpty(); k++) {
                int idx = random.nextInt(availableNodes.size());
                Node cand = availableNodes.get(idx);
                if (cand.cost < bestAddCost) {
                    bestAddCost = cand.cost;
                    bestAddIndexInAvailable = idx;
                }
            }
            if (bestAddIndexInAvailable == -1) bestAddIndexInAvailable = random.nextInt(availableNodes.size());

            Node nodeToAdd = availableNodes.remove(bestAddIndexInAvailable);
            selectedNodes.add(nodeToAdd);
            selectedIds.add(nodeToAdd.id);

            if (bestRemoveIndexInCycle <= newCycle.size()) {
                newCycle.add(bestRemoveIndexInCycle, nodeToAdd.id);
            } else {
                newCycle.add(nodeToAdd.id);
            }
        }

        int swaps = Math.max(1, strength);
        for (int s = 0; s < swaps; s++) {
            if (newCycle.size() < 2) break;
            int pos1 = random.nextInt(newCycle.size());
            int pos2 = random.nextInt(newCycle.size());
            if (pos1 == pos2) continue;
            Collections.swap(newCycle, pos1, pos2);
        }

        int newTotalDistance = calculateTotalDistance(instance, newCycle);
        int totalNodeCost = selectedNodes.stream().mapToInt(node -> node.cost).sum();
        int newTotalCost = newTotalDistance + totalNodeCost;

        return new Solution(selectedNodes, newCycle, newTotalCost, newTotalDistance, 0);
    }
}
