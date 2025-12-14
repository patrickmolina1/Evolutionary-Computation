package HybridEvolutionary;

import LargeNeighborhoodSearch.LargeNeighborhoodSearchSolver;
import LocalSearch.IntraRouteMoveType;
import LocalSearch.LocalSearchSolver;
import LocalSearch.StartingSolutionType;
import Utilities.*;

import java.util.*;

public class HybridEvolutionarySolver extends Solver {

    private LocalSearchSolver localSearchSolver;
    private Random random;
    private static final int POPULATION_SIZE = 20;

    public HybridEvolutionarySolver() {
        this.localSearchSolver = new LocalSearchSolver();
        this.random = new Random();
    }

    public Solution hybridEvolutionary(Instance instance, long timeLimitMs, RecombinationOperator operator, boolean useLocalSearchAfterRecombination) {
        long startTime = System.currentTimeMillis();
        int numLs = 0;

        // Initialize population
        List<Solution> population = initializePopulation(instance);

        Solution bestSolution = getBestSolution(population);

        while (System.currentTimeMillis() - startTime < timeLimitMs) {
            // Select two parents randomly
            Solution parent1 = selectParent(population);
            Solution parent2 = selectParent(population);

            // Apply recombination
            Solution offspring = null;
            if (operator == RecombinationOperator.OPERATOR_1) {
                offspring = recombinationOperator1(instance, parent1, parent2);
            } else {
                offspring = recombinationOperator2(instance, parent1, parent2);
            }

            // Apply local search to offspring
            if (useLocalSearchAfterRecombination) {
                offspring = applyLocalSearch(instance, offspring);
                numLs++;
            }

            // Add to population if unique and better than worst
            if (!isDuplicateInPopulation(offspring, population)) {
                // Replace worst solution in population
                int worstIdx = getWorstSolutionIndex(population);
                if (offspring.totalCost < population.get(worstIdx).totalCost) {
                    population.set(worstIdx, offspring);
                }
            }

            // Update best solution
            if (offspring.totalCost < bestSolution.totalCost) {
                bestSolution = offspring;
            }
        }

        int totalTime = (int) (System.currentTimeMillis() - startTime);
        bestSolution.totalRunningTime = totalTime;
        bestSolution.iterations = numLs;
        return bestSolution;
    }

    private List<Solution> initializePopulation(Instance instance) {
        List<Solution> population = new ArrayList<>();

        while (population.size() < POPULATION_SIZE) {
            // Generate initial solution using local search
            Solution solution = localSearchSolver.greedyLocalSearch(
                    instance,
                    StartingSolutionType.RANDOM,
                    IntraRouteMoveType.EDGE_EXCHANGE
            );

            // Add if unique
            if (!isDuplicateInPopulation(solution, population)) {
                population.add(solution);
            }
        }

        return population;
    }

    private Solution selectParent(List<Solution> population) {
        return population.get(random.nextInt(population.size()));
    }

    private Solution recombinationOperator1(Instance instance, Solution parent1, Solution parent2) {
        Set<Integer> p1Nodes = new HashSet<>(parent1.cycle);
        Set<Integer> p2Nodes = new HashSet<>(parent2.cycle);

        // Find common nodes
        Set<Integer> commonNodes = new HashSet<>(p1Nodes);
        commonNodes.retainAll(p2Nodes);

        // Find common edges
        Set<String> p1Edges = getEdges(parent1.cycle);
        Set<String> p2Edges = getEdges(parent2.cycle);
        Set<String> commonEdges = new HashSet<>(p1Edges);
        commonEdges.retainAll(p2Edges);

        // Build common subpaths
        List<List<Integer>> subpaths = buildSubpathsFromCommonEdges(parent1.cycle, commonEdges, commonNodes);

        // Calculate how many nodes we need
        int targetSize = (int) Math.ceil(instance.nodes.size() / 2.0);
        int currentSize = commonNodes.size();

        // Add random nodes to reach target size
        List<Node> availableNodes = new ArrayList<>();
        for (Node node : instance.nodes) {
            if (!commonNodes.contains(node.id)) {
                availableNodes.add(node);
            }
        }
        Collections.shuffle(availableNodes, random);

        for (int i = 0; i < targetSize - currentSize && i < availableNodes.size(); i++) {
            // Add as single-node subpath
            List<Integer> singleNode = new ArrayList<>();
            singleNode.add(availableNodes.get(i).id);
            subpaths.add(singleNode);
        }

        // Connect subpaths randomly
        List<Integer> cycle = connectSubpathsRandomly(instance, subpaths);

        // Build solution
        return buildSolutionFromCycle(instance, cycle);
    }

    private Solution recombinationOperator2(Instance instance, Solution parent1, Solution parent2) {
        // Choose one parent as base
        Solution baseParent = random.nextBoolean() ? parent1 : parent2;
        Solution otherParent = (baseParent == parent1) ? parent2 : parent1;

        Set<Integer> otherNodes = new HashSet<>(otherParent.cycle);

        // Keep only common nodes
        List<Integer> filteredCycle = new ArrayList<>();
        List<Node> filteredSelectedNodes = new ArrayList<>();

        for (Integer nodeId : baseParent.cycle) {
            if (otherNodes.contains(nodeId)) {
                filteredCycle.add(nodeId);
                // Find the node object
                for (Node node : instance.nodes) {
                    if (node.id == nodeId) {
                        filteredSelectedNodes.add(node);
                        break;
                    }
                }
            }
        }

        // Create partial solution
        Solution partialSolution = buildSolutionFromCycle(instance, filteredCycle);

        // Repair using LNS repair method (greedy-regret)
        LargeNeighborhoodSearchSolver lnsSolver = new LargeNeighborhoodSearchSolver();
        lnsSolver.repairWeighted(instance, partialSolution, 0.5, 0.5);

        return partialSolution;
    }

    private Set<String> getEdges(List<Integer> cycle) {
        Set<String> edges = new HashSet<>();
        for (int i = 0; i < cycle.size(); i++) {
            int from = cycle.get(i);
            int to = cycle.get((i + 1) % cycle.size());
            // Store edge in canonical form (smaller id first)
            String edge = from < to ? from + "-" + to : to + "-" + from;
            edges.add(edge);
        }
        return edges;
    }

    private List<List<Integer>> buildSubpathsFromCommonEdges(List<Integer> cycle, Set<String> commonEdges, Set<Integer> commonNodes) {
        List<List<Integer>> subpaths = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        for (int i = 0; i < cycle.size(); i++) {
            int nodeId = cycle.get(i);

            if (!commonNodes.contains(nodeId) || visited.contains(nodeId)) {
                continue;
            }

            // Start a new subpath
            List<Integer> subpath = new ArrayList<>();
            subpath.add(nodeId);
            visited.add(nodeId);

            // Extend forward as long as we have common edges
            int current = i;
            while (true) {
                int next = (current + 1) % cycle.size();
                int nextNode = cycle.get(next);

                if (!commonNodes.contains(nextNode) || visited.contains(nextNode)) {
                    break;
                }

                String edge = nodeId < nextNode ? nodeId + "-" + nextNode : nextNode + "-" + nodeId;
                if (!commonEdges.contains(edge)) {
                    break;
                }

                subpath.add(nextNode);
                visited.add(nextNode);
                current = next;
                nodeId = nextNode;
            }

            subpaths.add(subpath);
        }

        return subpaths;
    }

    private List<Integer> connectSubpathsRandomly(Instance instance, List<List<Integer>> subpaths) {
        if (subpaths.isEmpty()) {
            return new ArrayList<>();
        }

        Collections.shuffle(subpaths, random);

        List<Integer> cycle = new ArrayList<>();

        for (List<Integer> subpath : subpaths) {
            // Randomly decide direction
            if (random.nextBoolean()) {
                Collections.reverse(subpath);
            }
            cycle.addAll(subpath);
        }

        return cycle;
    }

    private int calculateInsertionCost(Instance instance, List<Integer> cycle, int nodeId, int pos) {
        if (cycle.isEmpty()) {
            return 0;
        }

        if (pos == 0) {
            return instance.distanceMatrix[nodeId][cycle.get(0)] +
                    instance.distanceMatrix[cycle.get(cycle.size() - 1)][nodeId] -
                    instance.distanceMatrix[cycle.get(cycle.size() - 1)][cycle.get(0)];
        } else if (pos == cycle.size()) {
            return instance.distanceMatrix[cycle.get(cycle.size() - 1)][nodeId] +
                    instance.distanceMatrix[nodeId][cycle.get(0)] -
                    instance.distanceMatrix[cycle.get(cycle.size() - 1)][cycle.get(0)];
        } else {
            int prev = cycle.get(pos - 1);
            int next = cycle.get(pos);
            return instance.distanceMatrix[prev][nodeId] +
                    instance.distanceMatrix[nodeId][next] -
                    instance.distanceMatrix[prev][next];
        }
    }

    private Solution buildSolutionFromCycle(Instance instance, List<Integer> cycle) {
        List<Node> selectedNodes = new ArrayList<>();
        for (int nodeId : cycle) {
            for (Node node : instance.nodes) {
                if (node.id == nodeId) {
                    selectedNodes.add(node);
                    break;
                }
            }
        }

        int totalDistance = 0;
        for (int i = 0; i < cycle.size(); i++) {
            int from = cycle.get(i);
            int to = cycle.get((i + 1) % cycle.size());
            totalDistance += instance.distanceMatrix[from][to];
        }

        int totalNodeCost = selectedNodes.stream().mapToInt(n -> n.cost).sum();
        int totalCost = totalDistance + totalNodeCost;

        return new Solution(selectedNodes, cycle, totalCost, totalDistance, 0);
    }

    private Solution applyLocalSearch(Instance instance, Solution offspring) {
        // Save the offspring cycle temporarily
        List<Integer> offspringCycle = new ArrayList<>(offspring.cycle);
        List<Node> offspringNodes = new ArrayList<>(offspring.selectedNodes);

        // Create a custom starting solution in LocalSearchSolver
        // by temporarily modifying it to use this specific solution
        // OR: Just run steepest LS directly if you have access to it

        // Better approach: Use steepest local search from LNS
        LargeNeighborhoodSearchSolver lnsSolver = new LargeNeighborhoodSearchSolver();
        return lnsSolver.steepestLocalSearch(instance, offspring, IntraRouteMoveType.EDGE_EXCHANGE);
    }

    private boolean isDuplicateInPopulation(Solution solution, List<Solution> population) {
        for (Solution existing : population) {
            if (existing.totalCost == solution.totalCost) {
                return true;
            }
        }
        return false;
    }

    private Solution getBestSolution(List<Solution> population) {
        Solution best = population.get(0);
        for (Solution sol : population) {
            if (sol.totalCost < best.totalCost) {
                best = sol;
            }
        }
        return best;
    }

    private int getWorstSolutionIndex(List<Solution> population) {
        int worstIdx = 0;
        int worstCost = population.get(0).totalCost;

        for (int i = 1; i < population.size(); i++) {
            if (population.get(i).totalCost > worstCost) {
                worstCost = population.get(i).totalCost;
                worstIdx = i;
            }
        }

        return worstIdx;
    }
}
