package LocalSearch;

import GreedyRegretHeuristics.GreedyRegretHeuristicsSolver;
import Utilities.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LocalSearchSolver extends Solver {

    private GreedyRegretHeuristicsSolver greedySolver;
    private Random random;

    public LocalSearchSolver(){
        this.greedySolver = new GreedyRegretHeuristicsSolver();
        this.random = new Random();
    }

    public Solution greedyLocalSearch(Instance instance, StartingSolutionType startingSolutionType, IntraRouteMoveType intraRouteMoveType){
        int startTime = (int) System.currentTimeMillis();

        Solution currentSolution = generateStartingSolution(instance, startingSolutionType);

        List<Node> selectedNodes = new ArrayList<>(currentSolution.selectedNodes);
        List<Integer> cycle = new ArrayList<>(currentSolution.cycle);
        Set<Integer> selectedIds = new HashSet<>();
        for (Node n : selectedNodes) selectedIds.add(n.id);

        int currentCost = currentSolution.totalCost;
        int currentDistance = currentSolution.totalDistance;

        boolean improved = true;

        while (improved) {
            improved = false;

            boolean tryIntraFirst = random.nextBoolean();

            if (tryIntraFirst) {
                improved = tryIntraRouteMove(instance, cycle, intraRouteMoveType);
                if (improved) {
                    // Recalculate costs after intra move
                    currentDistance = calculateTotalDistance(instance, cycle);
                    int totalNodeCost = selectedNodes.stream().mapToInt(n -> n.cost).sum();
                    currentCost = currentDistance + totalNodeCost;
                    continue;
                }
            }

            // Try inter-route move if intra didn't improve (or wasn't tried first)
            MoveResult interResult = tryInterRouteMove(instance, cycle, selectedNodes, selectedIds);
            if (interResult.improved) {
                improved = true;
                currentDistance += interResult.distanceDelta;
                currentCost += interResult.totalDelta;
                continue;
            }

            // If we tried intra first and it didn't work, don't try it again
            // If we tried inter first, now try intra
            if (!tryIntraFirst) {
                improved = tryIntraRouteMove(instance, cycle, intraRouteMoveType);
                if (improved) {
                    currentDistance = calculateTotalDistance(instance, cycle);
                    int totalNodeCost = selectedNodes.stream().mapToInt(n -> n.cost).sum();
                    currentCost = currentDistance + totalNodeCost;
                }
            }
        }

        int endTime = (int) System.currentTimeMillis();
        return new Solution(selectedNodes, cycle, currentCost, currentDistance, endTime - startTime);
    }

    private boolean tryIntraRouteMove(Instance instance, List<Integer> cycle, IntraRouteMoveType intraRouteMoveType) {
        // Generate randomized positions
        List<Integer> positions = IntStream.range(0, cycle.size())
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(positions, random);

        // Try moves in random order, stop at first improvement
        for (int i = 0; i < positions.size(); i++) {
            int pos1 = positions.get(i);

            for (int j = i + 1; j < positions.size(); j++) {
                int pos2 = positions.get(j);

                // Ensure pos1 < pos2 for consistency
                if (pos1 > pos2) {
                    int temp = pos1;
                    pos1 = pos2;
                    pos2 = temp;
                }

                // For edge exchange, skip if not valid
                if (intraRouteMoveType == IntraRouteMoveType.EDGE_EXCHANGE) {
                    if (pos2 - pos1 < 2) continue;
                    if (pos1 == 0 && pos2 == cycle.size() - 1) continue;
                }

                int[] move = new int[]{pos1, pos2};
                int delta = calculateIntraDelta(instance, cycle, move, intraRouteMoveType);

                if (delta < 0) {
                    applyIntraMove(cycle, move, intraRouteMoveType);
                    return true;
                }
            }
        }

        return false;
    }

    private MoveResult tryInterRouteMove(Instance instance, List<Integer> cycle,
                                         List<Node> selectedNodes, Set<Integer> selectedIds) {
        // Create randomized list of selected nodes
        List<Integer> selectedList = new ArrayList<>(selectedIds);
        Collections.shuffle(selectedList, random);

        // Create randomized list of non-selected nodes
        List<Integer> nonSelectedList = new ArrayList<>();
        for (Node node : instance.nodes) {
            if (!selectedIds.contains(node.id)) {
                nonSelectedList.add(node.id);
            }
        }
        Collections.shuffle(nonSelectedList, random);

        // Try moves in random order
        for (int selectedNodeId : selectedList) {
            for (int nonSelectedNodeId : nonSelectedList) {
                DeltaResult deltaResult = calculateInterDeltaDetailed(instance, cycle, selectedNodes,
                        selectedNodeId, nonSelectedNodeId);

                if (deltaResult.totalDelta < 0) {
                    // Apply the move
                    applyInterMove(instance, selectedNodes, cycle, selectedIds, selectedNodeId, nonSelectedNodeId);
                    return new MoveResult(true, deltaResult.totalDelta, deltaResult.distanceDelta);
                }
            }
        }

        return new MoveResult(false, 0, 0);
    }

    public Solution steepestLocalSearch(Instance instance, StartingSolutionType startingSolutionType, IntraRouteMoveType intraRouteMoveType){
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

    protected Solution generateStartingSolution(Instance instance, StartingSolutionType type) {
        if (type == StartingSolutionType.RANDOM) {
            return generateRandomSolution(instance);
        } else {
            // Use best greedy heuristic - weighted regret with greedy cycle
            Node startNode = instance.nodes.get(random.nextInt(instance.nodes.size()));
            return greedySolver.greedyWeightedRegretGreedyCycle(instance, startNode, 0.5, 0.5);
        }
    }

    protected Solution generateRandomSolution(Instance instance) {
        int n = instance.nodes.size();
        int numToSelect = (int) Math.ceil(n / 2.0);

        List<Node> shuffled = new ArrayList<>(instance.nodes);
        Collections.shuffle(shuffled, random);
        List<Node> selected = new ArrayList<>(shuffled.subList(0, numToSelect));

        List<Integer> order = new ArrayList<>();
        for (Node node : selected) order.add(node.id);
        Collections.shuffle(order, random);

        int totalDistance = calculateTotalDistance(instance, order);
        int totalNodeCost = selected.stream().mapToInt(node -> node.cost).sum();
        int totalCost = totalDistance + totalNodeCost;

        return new Solution(selected, order, totalCost, totalDistance, 0);
    }

    private List<int[]> generateIntraMoves(int cycleSize, IntraRouteMoveType moveType) {
        List<int[]> moves = new ArrayList<>();

        if (moveType == IntraRouteMoveType.NODE_EXCHANGE) {
            // All pairs of positions to swap nodes
            for (int i = 0; i < cycleSize; i++) {
                for (int j = i + 1; j < cycleSize; j++) {
                    moves.add(new int[]{i, j});
                }
            }
        } else {
            // EDGE_EXCHANGE: All pairs of edges to swap
            for (int i = 0; i < cycleSize; i++) {
                for (int j = i + 2; j < cycleSize; j++) {
                    if (i == 0 && j == cycleSize - 1) continue; // Skip adjacent edges
                    moves.add(new int[]{i, j});
                }
            }
        }

        return moves;
    }

    private List<int[]> generateInterMoves(Instance instance, Set<Integer> selectedIds) {
        List<int[]> moves = new ArrayList<>();

        for (int selectedId : selectedIds) {
            for (Node node : instance.nodes) {
                if (!selectedIds.contains(node.id)) {
                    moves.add(new int[]{selectedId, node.id});
                }
            }
        }

        return moves;
    }

    protected int calculateIntraDelta(Instance instance, List<Integer> cycle, int[] move, IntraRouteMoveType moveType) {
        int i = move[0];
        int j = move[1];

        if (moveType == IntraRouteMoveType.NODE_EXCHANGE) {
            return calculateNodeExchangeDelta(instance, cycle, i, j);
        } else {
            return calculateEdgeExchangeDelta(instance, cycle, i, j);
        }
    }

    protected int calculateNodeExchangeDelta(Instance instance, List<Integer> cycle, int pos1, int pos2) {
        int n = cycle.size();

        int node1 = cycle.get(pos1);
        int node2 = cycle.get(pos2);

        int prev1 = cycle.get((pos1 - 1 + n) % n);
        int next1 = cycle.get((pos1 + 1) % n);
        int prev2 = cycle.get((pos2 - 1 + n) % n);
        int next2 = cycle.get((pos2 + 1) % n);

        // If nodes are adjacent, handle specially
        if ((pos1 + 1) % n == pos2 || (pos2 + 1) % n == pos1) {
            // Adjacent nodes
            if ((pos1 + 1) % n == pos2) {
                // node1 -> node2 -> next2
                int oldCost = instance.distanceMatrix[prev1][node1] +
                        instance.distanceMatrix[node1][node2] +
                        instance.distanceMatrix[node2][next2];
                int newCost = instance.distanceMatrix[prev1][node2] +
                        instance.distanceMatrix[node2][node1] +
                        instance.distanceMatrix[node1][next2];
                return newCost - oldCost;
            } else {
                // node2 -> node1 -> next1
                int oldCost = instance.distanceMatrix[prev2][node2] +
                        instance.distanceMatrix[node2][node1] +
                        instance.distanceMatrix[node1][next1];
                int newCost = instance.distanceMatrix[prev2][node1] +
                        instance.distanceMatrix[node1][node2] +
                        instance.distanceMatrix[node2][next1];
                return newCost - oldCost;
            }
        }

        // Non-adjacent nodes
        int oldCost = instance.distanceMatrix[prev1][node1] +
                instance.distanceMatrix[node1][next1] +
                instance.distanceMatrix[prev2][node2] +
                instance.distanceMatrix[node2][next2];

        int newCost = instance.distanceMatrix[prev1][node2] +
                instance.distanceMatrix[node2][next1] +
                instance.distanceMatrix[prev2][node1] +
                instance.distanceMatrix[node1][next2];

        return newCost - oldCost;
    }

    protected int calculateEdgeExchangeDelta(Instance instance, List<Integer> cycle, int i, int j) {
        // Edge exchange (2-opt): reverse the segment between positions i and j
        int n = cycle.size();

        int node1 = cycle.get(i);
        int node2 = cycle.get((i + 1) % n);
        int node3 = cycle.get(j);
        int node4 = cycle.get((j + 1) % n);

        // Old edges: (node1, node2) and (node3, node4)
        // New edges: (node1, node3) and (node2, node4)
        int oldCost = instance.distanceMatrix[node1][node2] + instance.distanceMatrix[node3][node4];
        int newCost = instance.distanceMatrix[node1][node3] + instance.distanceMatrix[node2][node4];

        return newCost - oldCost;
    }

    protected DeltaResult calculateInterDeltaDetailed(Instance instance, List<Integer> cycle, List<Node> selectedNodes,
                                                    int selectedNodeId, int nonSelectedNodeId) {
        // Find position of selected node in cycle
        int pos = cycle.indexOf(selectedNodeId);
        if (pos == -1) return new DeltaResult(Integer.MAX_VALUE, 0);

        int n = cycle.size();
        int prev = cycle.get((pos - 1 + n) % n);
        int next = cycle.get((pos + 1) % n);

        // Calculate node cost difference
        Node selectedNode = null;
        Node nonSelectedNode = null;
        for (Node node : selectedNodes) {
            if (node.id == selectedNodeId) {
                selectedNode = node;
                break;
            }
        }
        for (Node node : instance.nodes) {
            if (node.id == nonSelectedNodeId) {
                nonSelectedNode = node;
                break;
            }
        }

        if (selectedNode == null || nonSelectedNode == null) {
            return new DeltaResult(Integer.MAX_VALUE, 0);
        }

        int costDelta = nonSelectedNode.cost - selectedNode.cost;

        // Calculate distance difference
        int oldDistance = instance.distanceMatrix[prev][selectedNodeId] + instance.distanceMatrix[selectedNodeId][next];
        int newDistance = instance.distanceMatrix[prev][nonSelectedNodeId] + instance.distanceMatrix[nonSelectedNodeId][next];
        int distanceDelta = newDistance - oldDistance;

        return new DeltaResult(costDelta + distanceDelta, distanceDelta);
    }

    protected void applyIntraMove(List<Integer> cycle, int[] move, IntraRouteMoveType moveType) {
        int i = move[0];
        int j = move[1];

        if (moveType == IntraRouteMoveType.NODE_EXCHANGE) {
            // Swap nodes at positions i and j
            int temp = cycle.get(i);
            cycle.set(i, cycle.get(j));
            cycle.set(j, temp);
        } else {
            // Edge exchange: reverse segment between i+1 and j
            int start = (i + 1) % cycle.size();
            int end = j;

            if (start <= end) {
                Collections.reverse(cycle.subList(start, end + 1));
            } else {
                // Handle wrap-around
                List<Integer> temp = new ArrayList<>();
                for (int k = start; k < cycle.size(); k++) temp.add(cycle.get(k));
                for (int k = 0; k <= end; k++) temp.add(cycle.get(k));
                Collections.reverse(temp);

                int idx = 0;
                for (int k = start; k < cycle.size(); k++) cycle.set(k, temp.get(idx++));
                for (int k = 0; k <= end; k++) cycle.set(k, temp.get(idx++));
            }
        }
    }

    protected void applyInterMove(Instance instance, List<Node> selectedNodes, List<Integer> cycle,
                                Set<Integer> selectedIds, int selectedNodeId, int nonSelectedNodeId) {
        // Find position in cycle
        int pos = cycle.indexOf(selectedNodeId);

        // Update cycle
        cycle.set(pos, nonSelectedNodeId);

        // Update selected nodes list
        Node nonSelectedNode = null;
        for (Node node : instance.nodes) {
            if (node.id == nonSelectedNodeId) {
                nonSelectedNode = node;
                break;
            }
        }

        for (int i = 0; i < selectedNodes.size(); i++) {
            if (selectedNodes.get(i).id == selectedNodeId) {
                selectedNodes.set(i, nonSelectedNode);
                break;
            }
        }

        // Update selectedIds set
        selectedIds.remove(selectedNodeId);
        selectedIds.add(nonSelectedNodeId);
    }

    protected int calculateTotalDistance(Instance instance, List<Integer> cycle) {
        int totalDistance = 0;
        for (int i = 0; i < cycle.size(); i++) {
            int from = cycle.get(i);
            int to = cycle.get((i + 1) % cycle.size());
            totalDistance += instance.distanceMatrix[from][to];
        }
        return totalDistance;
    }




}