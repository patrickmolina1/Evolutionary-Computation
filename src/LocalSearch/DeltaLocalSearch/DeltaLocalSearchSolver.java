package LocalSearch.DeltaLocalSearch;

import LocalSearch.DeltaResult;
import LocalSearch.IntraRouteMoveType;
import LocalSearch.LocalSearchSolver;
import LocalSearch.StartingSolutionType;
import Utilities.Instance;
import Utilities.Node;
import Utilities.Solution;

import java.util.*;

public class DeltaLocalSearchSolver extends LocalSearchSolver {

    public DeltaLocalSearchSolver(){
    }

    public Solution deltaLocalSearch(Instance instance, StartingSolutionType startingSolutionType,
                                     IntraRouteMoveType intraRouteMoveType){
        int startTime = (int) System.currentTimeMillis();

        // Generate starting solution
        Solution currentSolution = generateStartingSolution(instance, startingSolutionType);

        // Make copies of the solution data
        List<Node> selectedNodes = new ArrayList<>(currentSolution.selectedNodes);
        List<Integer> cycle = new ArrayList<>(currentSolution.cycle);
        Set<Integer> selectedIds = new HashSet<>();
        for (Node n : selectedNodes) selectedIds.add(n.id);

        int currentCost = currentSolution.totalCost;
        int currentDistance = currentSolution.totalDistance;

        // List of Improving Moves (LM)
        List<StoredMove> lm = new ArrayList<>();

        // Initial full scan to populate LM
        performFullScan(instance, cycle, selectedNodes, selectedIds, intraRouteMoveType, lm, true);

        // Main search loop
        while (!lm.isEmpty()) {
            boolean moveApplied = false;

            // Iterate through LM to find applicable move
            Iterator<StoredMove> iterator = lm.iterator();
            while (iterator.hasNext()) {
                StoredMove storedMove = iterator.next();

                if (storedMove.moveType.equals("INTER")) {
                    // Validate inter-route move
                    int selectedNodeId = storedMove.move[0];
                    int nonSelectedNodeId = storedMove.move[1];

                    // Check if selected node is still in the cycle
                    int pos = cycle.indexOf(selectedNodeId);
                    if (pos == -1) {
                        // Case 1: Edge doesn't exist anymore
                        iterator.remove();
                        continue;
                    }

                    // Check if the node is still selected
                    if (!selectedIds.contains(selectedNodeId)) {
                        // Case 1: Node no longer selected
                        iterator.remove();
                        continue;
                    }

                    // Check edge directions
                    int n = cycle.size();
                    int currentPrev = cycle.get((pos - 1 + n) % n);
                    int currentNext = cycle.get((pos + 1) % n);

                    boolean prevEdgeExists = (currentPrev == storedMove.prevNode || currentPrev == selectedNodeId);
                    boolean nextEdgeExists = (currentNext == storedMove.nextNode || currentNext == selectedNodeId);

                    // Check if edges exist
                    if (currentPrev != storedMove.prevNode && currentNext != storedMove.nextNode) {
                        // Neither edge matches - Case 1
                        iterator.remove();
                        continue;
                    }

                    // Check edge directions
                    boolean prevMatches = (currentPrev == storedMove.prevNode);
                    boolean nextMatches = (currentNext == storedMove.nextNode);

                    if (!prevMatches || !nextMatches) {
                        // Case 2: Edges exist but direction is different
                        continue;
                    }

                    // Case 3: Valid and improving move
                    applyInterMove(instance, selectedNodes, cycle, selectedIds, selectedNodeId, nonSelectedNodeId);
                    currentDistance += storedMove.distanceDelta;
                    currentCost += storedMove.delta;
                    iterator.remove();
                    moveApplied = true;
                    break;

                } else { // INTRA move
                    // For intra-route moves, just check if positions are still valid
                    int pos1 = storedMove.move[0];
                    int pos2 = storedMove.move[1];

                    if (pos1 >= cycle.size() || pos2 >= cycle.size()) {
                        // Invalid positions (shouldn't happen for intra, but check anyway)
                        iterator.remove();
                        continue;
                    }

                    // Recalculate delta to verify it's still improving
                    int currentDelta = calculateIntraDelta(instance, cycle, storedMove.move, storedMove.intraType);

                    if (currentDelta >= 0) {
                        // No longer improving
                        iterator.remove();
                        continue;
                    }

                    // Apply the move
                    applyIntraMove(cycle, storedMove.move, storedMove.intraType);
                    currentDistance += currentDelta;
                    currentCost += currentDelta;
                    iterator.remove();
                    moveApplied = true;
                    break;
                }
            }

            // If no move was applied, LM is stale - perform full scan
            if (!moveApplied) {
                lm.clear();
                performFullScan(instance, cycle, selectedNodes, selectedIds, intraRouteMoveType, lm, false);

                // If no improving moves found, terminate
                if (lm.isEmpty()) {
                    break;
                }

                // Apply the best move from the full scan
                if (!lm.isEmpty()) {
                    StoredMove bestMove = lm.remove(0);

                    if (bestMove.moveType.equals("INTRA")) {
                        applyIntraMove(cycle, bestMove.move, bestMove.intraType);
                        currentDistance += bestMove.delta;
                        currentCost += bestMove.delta;
                    } else {
                        int selectedNodeId = bestMove.move[0];
                        int nonSelectedNodeId = bestMove.move[1];
                        applyInterMove(instance, selectedNodes, cycle, selectedIds, selectedNodeId, nonSelectedNodeId);
                        currentDistance += bestMove.distanceDelta;
                        currentCost += bestMove.delta;
                    }
                }
            }
        }

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selectedNodes, cycle, currentCost, currentDistance, endTime - startTime);
    }

    private void performFullScan(Instance instance, List<Integer> cycle, List<Node> selectedNodes,
                                  Set<Integer> selectedIds, IntraRouteMoveType intraRouteMoveType,
                                  List<StoredMove> lm, boolean isInitial) {
        // Evaluate all intra-route moves
        List<int[]> intraMoves = generateIntraMoves(cycle.size(), intraRouteMoveType);
        for (int[] move : intraMoves) {
            int delta = calculateIntraDelta(instance, cycle, move, intraRouteMoveType);
            if (delta < 0) {
                lm.add(new StoredMove("INTRA", move, delta, intraRouteMoveType));
            }
        }

        // Evaluate all inter-route moves (including inverted edges)
        for (int selectedNodeId : selectedIds) {
            int pos = cycle.indexOf(selectedNodeId);
            if (pos == -1) continue;

            int n = cycle.size();
            int prevNode = cycle.get((pos - 1 + n) % n);
            int nextNode = cycle.get((pos + 1) % n);

            for (Node node : instance.nodes) {
                if (!selectedIds.contains(node.id)) {
                    int nonSelectedNodeId = node.id;

                    // Calculate delta for normal edge orientation
                    DeltaResult deltaResult = calculateInterDeltaDetailed(instance, cycle, selectedNodes,
                            selectedNodeId, nonSelectedNodeId);

                    if (deltaResult.totalDelta < 0) {
                        lm.add(new StoredMove("INTER",
                                new int[]{selectedNodeId, nonSelectedNodeId},
                                deltaResult.totalDelta,
                                deltaResult.distanceDelta,
                                prevNode, nextNode, false, false));
                    }

                }
            }
        }

        // Sort LM by delta (best improvements first)
        lm.sort(Comparator.comparingInt(m -> m.delta));
    }

    private List<int[]> generateIntraMoves(int cycleSize, IntraRouteMoveType moveType) {
        List<int[]> moves = new ArrayList<>();

        if (moveType == IntraRouteMoveType.NODE_EXCHANGE) {
            for (int i = 0; i < cycleSize; i++) {
                for (int j = i + 1; j < cycleSize; j++) {
                    moves.add(new int[]{i, j});
                }
            }
        } else { // EDGE_EXCHANGE
            for (int i = 0; i < cycleSize; i++) {
                for (int j = i + 2; j < cycleSize; j++) {
                    if (i == 0 && j == cycleSize - 1) continue;
                    moves.add(new int[]{i, j});
                }
            }
        }

        return moves;
    }
}
