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

        Solution currentSolution = generateStartingSolution(instance, startingSolutionType);

        List<Node> selectedNodes = new ArrayList<>(currentSolution.selectedNodes);
        List<Integer> cycle = new ArrayList<>(currentSolution.cycle);
        Set<Integer> selectedIds = new HashSet<>();
        for (Node n : selectedNodes) selectedIds.add(n.id);

        int currentCost = currentSolution.totalCost;
        int currentDistance = currentSolution.totalDistance;

        List<StoredMove> lm = new ArrayList<>();

        performFullScan(instance, cycle, selectedNodes, selectedIds, intraRouteMoveType, lm);

        while (!lm.isEmpty()) {
            boolean moveApplied = false;

            Iterator<StoredMove> iterator = lm.iterator();
            while (iterator.hasNext()) {
                StoredMove storedMove = iterator.next();

                if (storedMove.moveType.equals("INTRA") && storedMove.intraType == IntraRouteMoveType.EDGE_EXCHANGE) {
                    // Check if stored edges still exist in the cycle
                    EdgeCheckResult checkResult = checkEdgeExistence(cycle, storedMove);

                    // Case 1: At least one edge doesn't exist
                    if (checkResult == EdgeCheckResult.NOT_EXIST) {
                        iterator.remove();
                        continue;
                    }

                    // Case 2: Different relative direction - skip but keep in LM
                    if (checkResult == EdgeCheckResult.DIFFERENT_DIRECTION) {
                        continue; // Don't apply, don't remove, continue browsing
                    }

                    // Case 3: Same relative direction (normal or both reversed) - apply move
                    if (checkResult == EdgeCheckResult.SAME_DIRECTION) {
                        applyIntraMove(cycle, storedMove.move, storedMove.intraType);
                        currentDistance += storedMove.delta;
                        currentCost += storedMove.delta;
                        iterator.remove();
                        moveApplied = true;

                        // Evaluate new moves created by this application
                        evaluateNewMoves(instance, cycle, selectedNodes, selectedIds,
                                intraRouteMoveType, lm, storedMove.move);
                        break;
                    }

                } else if (storedMove.moveType.equals("INTRA") && storedMove.intraType == IntraRouteMoveType.NODE_EXCHANGE) {
                    // Node exchange - recalculate delta (positions may have changed)
                    int pos1 = storedMove.move[0];
                    int pos2 = storedMove.move[1];

                    if (pos1 >= cycle.size() || pos2 >= cycle.size()) {
                        iterator.remove();
                        continue;
                    }

                    int currentDelta = calculateIntraDelta(instance, cycle, storedMove.move, storedMove.intraType);
                    if (currentDelta >= 0) {
                        iterator.remove();
                        continue;
                    }

                    applyIntraMove(cycle, storedMove.move, storedMove.intraType);
                    currentDistance += currentDelta;
                    currentCost += currentDelta;
                    iterator.remove();
                    moveApplied = true;

                    evaluateNewMoves(instance, cycle, selectedNodes, selectedIds,
                            intraRouteMoveType, lm, storedMove.move);
                    break;

                } else { // INTER move
                    int selectedNodeId = storedMove.move[0];
                    int nonSelectedNodeId = storedMove.move[1];

                    // Check if selected node still exists in cycle
                    if (!selectedIds.contains(selectedNodeId)) {
                        iterator.remove();
                        continue;
                    }

                    // Recalculate delta
                    DeltaResult deltaResult = calculateInterDeltaDetailed(instance, cycle, selectedNodes,
                            selectedNodeId, nonSelectedNodeId);

                    if (deltaResult.totalDelta >= 0) {
                        iterator.remove();
                        continue;
                    }

                    applyInterMove(instance, selectedNodes, cycle, selectedIds, selectedNodeId, nonSelectedNodeId);
                    currentDistance += deltaResult.distanceDelta;
                    currentCost += deltaResult.totalDelta;
                    iterator.remove();
                    moveApplied = true;

                    // Inter-route moves affect many edges, do full rescan
                    lm.clear();
                    performFullScan(instance, cycle, selectedNodes, selectedIds, intraRouteMoveType, lm);
                    break;
                }
            }

            if (!moveApplied) {
                break;
            }
        }

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selectedNodes, cycle, currentCost, currentDistance, endTime - startTime);
    }

    private enum EdgeCheckResult {
        NOT_EXIST,           // Case 1: Remove from LM
        DIFFERENT_DIRECTION, // Case 2: Keep in LM, don't apply
        SAME_DIRECTION       // Case 3: Apply and remove from LM
    }

    private EdgeCheckResult checkEdgeExistence(List<Integer> cycle, StoredMove storedMove) {
        // Find where the stored edges are in the current cycle
        int edge1Pos = findEdgePosition(cycle, storedMove.edge1Start, storedMove.edge1End);
        int edge2Pos = findEdgePosition(cycle, storedMove.edge2Start, storedMove.edge2End);

        // Also check inverted edges
        int edge1InvPos = findEdgePosition(cycle, storedMove.edge1End, storedMove.edge1Start);
        int edge2InvPos = findEdgePosition(cycle, storedMove.edge2End, storedMove.edge2Start);

        boolean edge1Exists = (edge1Pos != -1 || edge1InvPos != -1);
        boolean edge2Exists = (edge2Pos != -1 || edge2InvPos != -1);

        // Case 1: At least one edge doesn't exist
        if (!edge1Exists || !edge2Exists) {
            return EdgeCheckResult.NOT_EXIST;
        }

        // Check relative direction
        boolean normalDirection = (edge1Pos != -1 && edge2Pos != -1);
        boolean bothReversed = (edge1InvPos != -1 && edge2InvPos != -1);

        // Case 3: Same relative direction (both normal OR both reversed)
        if (normalDirection || bothReversed) {
            // Update move positions to current positions
            if (normalDirection) {
                storedMove.move[0] = edge1Pos;
                storedMove.move[1] = edge2Pos;
            } else {
                storedMove.move[0] = edge1InvPos;
                storedMove.move[1] = edge2InvPos;
            }
            return EdgeCheckResult.SAME_DIRECTION;
        }

        // Case 2: Different relative direction
        return EdgeCheckResult.DIFFERENT_DIRECTION;
    }

    private int findEdgePosition(List<Integer> cycle, int start, int end) {
        int n = cycle.size();
        for (int i = 0; i < n; i++) {
            int edgeStart = cycle.get(i);
            int edgeEnd = cycle.get((i + 1) % n);
            if (edgeStart == start && edgeEnd == end) {
                return i;
            }
        }
        return -1;
    }

    private void evaluateNewMoves(Instance instance, List<Integer> cycle, List<Node> selectedNodes,
                                  Set<Integer> selectedIds, IntraRouteMoveType intraRouteMoveType,
                                  List<StoredMove> lm, int[] appliedMove) {

        if (intraRouteMoveType != IntraRouteMoveType.EDGE_EXCHANGE) {
            return; // For node exchanges, new moves less predictable, skip
        }

        // involving the newly created edges
        int i = appliedMove[0];
        int j = appliedMove[1];
        int n = cycle.size();

        // Evaluate moves involving these edges
        for (int k = 0; k < n; k++) {
            if (k == i || k == j || Math.abs(k - i) < 2 || Math.abs(k - j) < 2) {
                continue; // Skip adjacent or same edges
            }

            int delta1 = calculateIntraDelta(instance, cycle, new int[]{i, k}, intraRouteMoveType);
            if (delta1 < 0) {
                int edge1Start = cycle.get(i);
                int edge1End = cycle.get((i + 1) % n);
                int edge2Start = cycle.get(k);
                int edge2End = cycle.get((k + 1) % n);

                StoredMove newMove = new StoredMove("INTRA", new int[]{i, k}, delta1, intraRouteMoveType,
                        edge1Start, edge1End, edge2Start, edge2End);
                insertSorted(lm, newMove);
            }


            int delta2 = calculateIntraDelta(instance, cycle, new int[]{j, k}, intraRouteMoveType);
            if (delta2 < 0) {
                int edge1Start = cycle.get(j);
                int edge1End = cycle.get((j + 1) % n);
                int edge2Start = cycle.get(k);
                int edge2End = cycle.get((k + 1) % n);

                StoredMove newMove = new StoredMove("INTRA", new int[]{j, k}, delta2, intraRouteMoveType,
                        edge1Start, edge1End, edge2Start, edge2End);
                insertSorted(lm, newMove);
            }
        }
    }

    private void insertSorted(List<StoredMove> lm, StoredMove move) {
        int index = Collections.binarySearch(lm, move, Comparator.comparingInt(m -> m.delta));
        if (index < 0) {
            index = -index - 1;
        }
        lm.add(index, move);
    }

    private void performFullScan(Instance instance, List<Integer> cycle, List<Node> selectedNodes,
                                 Set<Integer> selectedIds, IntraRouteMoveType intraRouteMoveType,
                                 List<StoredMove> lm) {

        if (intraRouteMoveType == IntraRouteMoveType.EDGE_EXCHANGE) {
            for (int i = 0; i < cycle.size(); i++) {
                for (int j = i + 2; j < cycle.size(); j++) {
                    if (i == 0 && j == cycle.size() - 1) continue;

                    int delta = calculateIntraDelta(instance, cycle, new int[]{i, j}, intraRouteMoveType);
                    if (delta < 0) {
                        int n = cycle.size();
                        int edge1Start = cycle.get(i);
                        int edge1End = cycle.get((i + 1) % n);
                        int edge2Start = cycle.get(j);
                        int edge2End = cycle.get((j + 1) % n);

                        // Store only once (not inverted version)
                        lm.add(new StoredMove("INTRA", new int[]{i, j}, delta, intraRouteMoveType,
                                edge1Start, edge1End, edge2Start, edge2End));


                    }
                }
            }
        } else {
            for (int i = 0; i < cycle.size(); i++) {
                for (int j = i + 1; j < cycle.size(); j++) {
                    int delta = calculateIntraDelta(instance, cycle, new int[]{i, j}, intraRouteMoveType);
                    if (delta < 0) {
                        lm.add(new StoredMove("INTRA", new int[]{i, j}, delta, intraRouteMoveType,
                                0, 0, 0, 0));
                    }
                }
            }
        }

        for (int selectedNodeId : selectedIds) {
            for (Node node : instance.nodes) {
                if (!selectedIds.contains(node.id)) {
                    DeltaResult deltaResult = calculateInterDeltaDetailed(instance, cycle, selectedNodes,
                            selectedNodeId, node.id);

                    if (deltaResult.totalDelta < 0) {
                        lm.add(new StoredMove("INTER",
                                new int[]{selectedNodeId, node.id},
                                deltaResult.totalDelta,
                                deltaResult.distanceDelta,
                                0, 0, 0, 0));
                    }
                }
            }
        }

        lm.sort(Comparator.comparingInt(m -> m.delta));
    }
}
