package OwnMethod;

import HybridEvolutionary.HybridEvolutionarySolver;
import HybridEvolutionary.RecombinationOperator;
import LargeNeighborhoodSearch.LargeNeighborhoodSearchSolver;
import LocalSearch.IntraRouteMoveType;
import LocalSearch.LocalSearchSolver;
import LocalSearch.StartingSolutionType;
import Utilities.*;

import java.util.*;


public class OwnMethodSolver extends Solver {

    private LocalSearchSolver localSearchSolver;
    private LargeNeighborhoodSearchSolver lnsSolver;
    private Random random;

    private static final int POPULATION_SIZE = 35; // Larger population for diversity
    private static final int ELITE_SIZE = 8; // Top solutions to preserve
    private static final double DIVERSITY_THRESHOLD = 0.12; // Min diversity to maintain

    // Adaptive operator selection
    private Map<String, OperatorStats> operatorStats;

    public OwnMethodSolver() {
        this.localSearchSolver = new LocalSearchSolver();
        this.lnsSolver = new LargeNeighborhoodSearchSolver();
        this.random = new Random();
        this.operatorStats = new HashMap<>();
        initializeOperatorStats();
    }


    public Solution solve(Instance instance, int timeLimit) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeLimit;

        System.out.println("=== Enhanced Hybrid Evolutionary Algorithm ===");

        // PHASE 1: Initialize diverse population (15% time)
        System.out.println("\n=== Phase 1: Population Initialization ===");
        long phase1End = startTime + (timeLimit * 15 / 100);
        List<Solution> population = initializeDiversePopulation(instance, phase1End);

        Solution bestSolution = getBestSolution(population);
        System.out.println("Initial best: " + bestSolution.totalCost);
        System.out.println("Population diversity: " + String.format("%.2f", calculateDiversity(population)));

        // PHASE 2: Main evolutionary loop (70% time)
        System.out.println("\n=== Phase 2: Evolutionary Search ===");
        long phase2End = startTime + (timeLimit * 85 / 100);

        int generation = 0;
        int improvementCounter = 0;
        long lastPrintTime = System.currentTimeMillis();

        while (System.currentTimeMillis() < phase2End) {
            generation++;

            // Select parents using tournament selection
            Solution parent1 = tournamentSelection(population, 3);
            Solution parent2 = tournamentSelection(population, 3);

            // Adaptive recombination operator selection
            String operatorName = selectOperatorAdaptively();
            Solution offspring = applyRecombination(instance, parent1, parent2, operatorName);

            // Apply variable neighborhood descent
            offspring = variableNeighborhoodDescent(instance, offspring);

            // Update operator statistics
            int improvement = Math.max(0, Math.min(parent1.totalCost, parent2.totalCost) - offspring.totalCost);
            updateOperatorStats(operatorName, improvement);

            // Population management
            if (offspring.totalCost < bestSolution.totalCost) {
                bestSolution = cloneSolution(offspring);
                improvementCounter++;
                System.out.println("  Gen " + generation + " - New best: " + bestSolution.totalCost +
                                 " (operator: " + operatorName + ")");
            }

            // Add to population if it improves diversity or quality
            if (shouldAddToPopulation(offspring, population)) {
                int worstIdx = getWorstSolutionIndex(population);
                population.set(worstIdx, offspring);
            }

            // Periodic diversity management
            if (generation % 20 == 0) {
                double diversity = calculateDiversity(population);
                if (diversity < DIVERSITY_THRESHOLD) {
                    System.out.println("  Gen " + generation + " - Restoring diversity (current: " +
                                     String.format("%.2f", diversity) + ")");
                    restoreDiversity(instance, population);
                }
            }

            // Periodic intensification on best solution
            if (generation % 50 == 0) {
                System.out.println("  Gen " + generation + " - Intensification phase");
                Solution intensified = intensifyBestSolution(instance, bestSolution, phase2End);
                if (intensified.totalCost < bestSolution.totalCost) {
                    bestSolution = intensified;
                    System.out.println("  Intensified to: " + bestSolution.totalCost);
                }
            }

            // Progress output
            long currentTime = System.currentTimeMillis();
            if (generation % 25 == 0 || (currentTime - lastPrintTime) > 3000) {
                printProgress(generation, bestSolution, population, phase2End - currentTime);
                lastPrintTime = currentTime;
            }
        }

        System.out.println("\nEvolution complete:");
        System.out.println("  Generations: " + generation);
        System.out.println("  Improvements: " + improvementCounter);
        System.out.println("  Best cost: " + bestSolution.totalCost);
        printOperatorStats();

        // PHASE 3: Final intensive optimization (15% time)
        System.out.println("\n=== Phase 3: Final Optimization ===");
        bestSolution = finalIntensiveOptimization(instance, bestSolution, endTime);

        System.out.println("Final best: " + bestSolution.totalCost);

        bestSolution.totalRunningTime = (int)(System.currentTimeMillis() - startTime);
        return bestSolution;
    }

    // ==================== POPULATION INITIALIZATION ====================

    private List<Solution> initializeDiversePopulation(Instance instance, long timeLimit) {
        List<Solution> population = new ArrayList<>();

        // Use ALL move types for more diversity
        IntraRouteMoveType[] moveTypes = {
                IntraRouteMoveType.EDGE_EXCHANGE,
                IntraRouteMoveType.NODE_EXCHANGE
        };

        StartingSolutionType[] startTypes = {
                StartingSolutionType.RANDOM,
                StartingSolutionType.GREEDY
        };

        // Generate solutions with different configurations
        for (IntraRouteMoveType moveType : moveTypes) {
            for (StartingSolutionType startType : startTypes) {
                if (System.currentTimeMillis() >= timeLimit) break;

                // Multiple runs per config with different seeds
                for (int i = 0; i < 3; i++) {
                    Solution sol = localSearchSolver.steepestLocalSearch(instance, startType, moveType);
                    if (!isDuplicateInPopulation(sol, population)) {
                        population.add(sol);
                        System.out.println("  Solution " + population.size() + ": " + sol.totalCost);
                    }
                }
            }
        }

        // Add perturbation-based solutions
        while (population.size() < POPULATION_SIZE && System.currentTimeMillis() < timeLimit) {
            Solution base = population.get(random.nextInt(Math.min(5, population.size())));
            Solution perturbed = perturbSolution(instance, base, 0.3);
            if (!isDuplicateInPopulation(perturbed, population)) {
                population.add(perturbed);
            }
        }

        return population;
    }

    // Add perturbation method
    private Solution perturbSolution(Instance instance, Solution base, double perturbStrength) {
        Solution perturbed = cloneSolution(base);
        int numChanges = (int)(perturbed.cycle.size() * perturbStrength);

        for (int i = 0; i < numChanges; i++) {
            if (random.nextBoolean() && perturbed.cycle.size() > 2) {
                perturbed.cycle.remove(random.nextInt(perturbed.cycle.size()));
            } else {
                List<Integer> available = new ArrayList<>();
                for (Node n : instance.nodes) {
                    if (!perturbed.cycle.contains(n.id)) available.add(n.id);
                }
                if (!available.isEmpty()) {
                    insertAtBestPosition(instance, perturbed.cycle,
                            available.get(random.nextInt(available.size())));
                }
            }
        }

        return buildSolutionFromCycle(instance, perturbed.cycle);
    }



    // ==================== ADAPTIVE OPERATOR SELECTION ====================

    private void initializeOperatorStats() {
        operatorStats.put("OPERATOR_1", new OperatorStats());
        operatorStats.put("OPERATOR_2", new OperatorStats());
        operatorStats.put("PATH_RELINKING", new OperatorStats());
    }

    private String selectOperatorAdaptively() {
        // Use UCB1 (Upper Confidence Bound) for operator selection
        double totalAttempts = operatorStats.values().stream()
            .mapToDouble(s -> s.attempts)
            .sum();

        if (totalAttempts < 30) {
            // Exploration phase: round-robin
            List<String> operators = new ArrayList<>(operatorStats.keySet());
            return operators.get((int)(totalAttempts % operators.size()));
        }

        // Exploitation with exploration bonus
        String bestOperator = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Map.Entry<String, OperatorStats> entry : operatorStats.entrySet()) {
            OperatorStats stats = entry.getValue();
            double avgReward = stats.attempts > 0 ? stats.totalImprovement / stats.attempts : 0;
            double explorationBonus = Math.sqrt(2 * Math.log(totalAttempts) / Math.max(1, stats.attempts));
            double score = avgReward + 1000 * explorationBonus; // Scale exploration bonus

            if (score > bestScore) {
                bestScore = score;
                bestOperator = entry.getKey();
            }
        }

        return bestOperator;
    }

    private void updateOperatorStats(String operatorName, int improvement) {
        OperatorStats stats = operatorStats.get(operatorName);
        stats.attempts++;
        stats.totalImprovement += improvement;
        if (improvement > 0) {
            stats.successes++;
        }
    }

    // ==================== RECOMBINATION OPERATORS ====================

    private Solution applyRecombination(Instance instance, Solution parent1, Solution parent2, String operatorName) {
        switch (operatorName) {
            case "OPERATOR_1":
                return recombinationOperator1(instance, parent1, parent2);
            case "OPERATOR_2":
                return recombinationOperator2(instance, parent1, parent2);
            case "PATH_RELINKING":
                return pathRelinking(instance, parent1, parent2);
            default:
                return recombinationOperator1(instance, parent1, parent2);
        }
    }

    private Solution recombinationOperator1(Instance instance, Solution parent1, Solution parent2) {
        HybridEvolutionarySolver haeSolver = new HybridEvolutionarySolver();
        // Use reflection or create a helper method to access recombination
        // For now, reimplement simplified version

        Set<Integer> commonNodes = new HashSet<>(parent1.cycle);
        commonNodes.retainAll(parent2.cycle);

        List<Integer> cycle = new ArrayList<>(commonNodes);
        Collections.shuffle(cycle, random);

        // Add missing nodes greedily
        Set<Integer> used = new HashSet<>(cycle);
        int targetSize = instance.nodes.size() / 2;

        while (cycle.size() < targetSize) {
            int bestNode = -1;
            int bestCost = Integer.MAX_VALUE;

            for (Node node : instance.nodes) {
                if (!used.contains(node.id)) {
                    int cost = calculateBestInsertionCost(instance, cycle, node.id);
                    if (cost < bestCost) {
                        bestCost = cost;
                        bestNode = node.id;
                    }
                }
            }

            if (bestNode == -1) break;

            insertAtBestPosition(instance, cycle, bestNode);
            used.add(bestNode);
        }

        return buildSolutionFromCycle(instance, cycle);
    }

    private Solution recombinationOperator2(Instance instance, Solution parent1, Solution parent2) {
        Solution base = random.nextBoolean() ? cloneSolution(parent1) : cloneSolution(parent2);
        Solution other = (base == parent1) ? parent2 : parent1;

        Set<Integer> otherNodes = new HashSet<>(other.cycle);
        base.cycle.removeIf(nodeId -> !otherNodes.contains(nodeId));

        // Rebuild selectedNodes
        base.selectedNodes.clear();
        for (int nodeId : base.cycle) {
            for (Node node : instance.nodes) {
                if (node.id == nodeId) {
                    base.selectedNodes.add(node);
                    break;
                }
            }
        }

        // Repair using LNS
        lnsSolver.repairWeighted(instance, base, 0.4, 0.6);
        return base;
    }

    private Solution pathRelinking(Instance instance, Solution parent1, Solution parent2) {
        Solution current = cloneSolution(parent1);
        Solution target = parent2;

        Set<Integer> targetNodes = new HashSet<>(target.cycle);
        List<Integer> toRemove = new ArrayList<>();

        for (int nodeId : current.cycle) {
            if (!targetNodes.contains(nodeId)) {
                toRemove.add(nodeId);
            }
        }

        // Remove 30% of different nodes and add from target
        int numToChange = Math.min(3, toRemove.size());
        Collections.shuffle(toRemove, random);

        for (int i = 0; i < numToChange; i++) {
            current.cycle.remove(toRemove.get(i));
        }

        // Add random nodes from target
        for (int nodeId : target.cycle) {
            if (!current.cycle.contains(nodeId) && current.cycle.size() < instance.nodes.size() / 2) {
                insertAtBestPosition(instance, current.cycle, nodeId);
            }
        }

        return buildSolutionFromCycle(instance, current.cycle);
    }

    // ==================== VARIABLE NEIGHBORHOOD DESCENT ====================

    private Solution variableNeighborhoodDescent(Instance instance, Solution solution) {
        Solution current = cloneSolution(solution);

        IntraRouteMoveType[] neighborhoods = {
            IntraRouteMoveType.EDGE_EXCHANGE,
            IntraRouteMoveType.NODE_EXCHANGE
        };

        int k = 0;
        while (k < neighborhoods.length) {
            Solution neighbor = lnsSolver.steepestLocalSearch(instance, current, neighborhoods[k]);

            if (neighbor.totalCost < current.totalCost) {
                current = neighbor;
                k = 0; // Restart from first neighborhood
            } else {
                k++; // Try next neighborhood
            }
        }

        return current;
    }

    // ==================== POPULATION MANAGEMENT ====================

    private Solution tournamentSelection(List<Solution> population, int tournamentSize) {
        Solution best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Solution candidate = population.get(random.nextInt(population.size()));
            if (best == null || candidate.totalCost < best.totalCost) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean shouldAddToPopulation(Solution solution, List<Solution> population) {
        // Don't add duplicates
        if (isDuplicateInPopulation(solution, population)) {
            return false;
        }

        // Always add if better than worst
        Solution worst = population.get(getWorstSolutionIndex(population));
        if (solution.totalCost < worst.totalCost) {
            return true;
        }

        // Add if it increases diversity
        double currentDiversity = calculateDiversity(population);
        List<Solution> testPopulation = new ArrayList<>(population);
        testPopulation.set(getWorstSolutionIndex(testPopulation), solution);
        double newDiversity = calculateDiversity(testPopulation);

        return newDiversity > currentDiversity;
    }

    private double calculateDiversity(List<Solution> population) {
        if (population.size() < 2) return 1.0;

        double sumDifferences = 0;
        int comparisons = 0;

        for (int i = 0; i < population.size(); i++) {
            for (int j = i + 1; j < population.size(); j++) {
                sumDifferences += Math.abs(population.get(i).totalCost - population.get(j).totalCost);
                comparisons++;
            }
        }

        double avgCost = population.stream().mapToInt(s -> s.totalCost).average().orElse(1.0);
        return (sumDifferences / comparisons) / avgCost;
    }

    private void restoreDiversity(Instance instance, List<Solution> population) {
        // Keep elite solutions
        population.sort(Comparator.comparingInt(s -> s.totalCost));

        // Replace solutions after elite with new diverse solutions
        int replaceStart = ELITE_SIZE;
        int replaceCount = population.size() - ELITE_SIZE;

        for (int i = replaceStart; i < population.size(); i++) {
            Solution newSol = localSearchSolver.greedyLocalSearch(
                    instance, StartingSolutionType.RANDOM, IntraRouteMoveType.EDGE_EXCHANGE);

            // Only replace if not duplicate
            if (!isDuplicateInPopulation(newSol, population.subList(0, i))) {
                population.set(i, newSol);
            }
        }
    }


    // ==================== INTENSIFICATION ====================

    private Solution intensifyBestSolution(Instance instance, Solution solution, long timeLimit) {
        Solution best = cloneSolution(solution);
        long intensifyTime = Math.min(2000, (timeLimit - System.currentTimeMillis()) / 10);
        long intensifyEnd = System.currentTimeMillis() + intensifyTime;

        while (System.currentTimeMillis() < intensifyEnd) {
            // Try both move types
            for (IntraRouteMoveType moveType : IntraRouteMoveType.values()) {
                Solution improved = lnsSolver.steepestLocalSearch(instance, best, moveType);
                if (improved.totalCost < best.totalCost) {
                    best = improved;
                }
            }
        }

        return best;
    }

    private Solution finalIntensiveOptimization(Instance instance, Solution solution, long timeLimit) {
        Solution best = cloneSolution(solution);

        System.out.println("  Starting cost: " + best.totalCost);

        while (System.currentTimeMillis() < timeLimit) {
            boolean improved = false;

            // Exhaustive 2-opt
            for (IntraRouteMoveType moveType : IntraRouteMoveType.values()) {
                Solution optimized = lnsSolver.steepestLocalSearch(instance, best, moveType);
                if (optimized.totalCost < best.totalCost) {
                    best = optimized;
                    improved = true;
                    System.out.println("  Improved to: " + best.totalCost);
                }
            }

            if (!improved) break;
        }

        return best;
    }

    // ==================== UTILITIES ====================

    private int calculateBestInsertionCost(Instance instance, List<Integer> cycle, int nodeId) {
        if (cycle.isEmpty()) return 0;

        int bestCost = Integer.MAX_VALUE;
        for (int pos = 0; pos <= cycle.size(); pos++) {
            int cost = calculateInsertionCost(instance, cycle, nodeId, pos);
            if (cost < bestCost) {
                bestCost = cost;
            }
        }
        return bestCost;
    }

    private void insertAtBestPosition(Instance instance, List<Integer> cycle, int nodeId) {
        int bestPos = 0;
        int bestCost = Integer.MAX_VALUE;

        for (int pos = 0; pos <= cycle.size(); pos++) {
            int cost = calculateInsertionCost(instance, cycle, nodeId, pos);
            if (cost < bestCost) {
                bestCost = cost;
                bestPos = pos;
            }
        }

        cycle.add(bestPos, nodeId);
    }

    private int calculateInsertionCost(Instance instance, List<Integer> cycle, int nodeId, int pos) {
        if (cycle.isEmpty()) return 0;

        int prev = cycle.get((pos - 1 + cycle.size()) % cycle.size());
        int next = cycle.get(pos % cycle.size());

        return instance.distanceMatrix[prev][nodeId] +
               instance.distanceMatrix[nodeId][next] -
               instance.distanceMatrix[prev][next];
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

        int nodeCostSum = selectedNodes.stream().mapToInt(n -> n.cost).sum();
        return new Solution(selectedNodes, cycle, totalDistance + nodeCostSum, totalDistance, 0);
    }

    private Solution cloneSolution(Solution s) {
        return new Solution(
            new ArrayList<>(s.selectedNodes),
            new ArrayList<>(s.cycle),
            s.totalCost,
            s.totalDistance,
            0
        );
    }

    private boolean isDuplicateInPopulation(Solution solution, List<Solution> population) {
        for (Solution existing : population) {
            // Check both cost similarity AND route similarity
            if (Math.abs(existing.totalCost - solution.totalCost) < 5) {
                Set<Integer> nodes1 = new HashSet<>(existing.cycle);
                Set<Integer> nodes2 = new HashSet<>(solution.cycle);
                Set<Integer> intersection = new HashSet<>(nodes1);
                intersection.retainAll(nodes2);

                double similarity = (double)intersection.size() / Math.max(nodes1.size(), nodes2.size());
                if (similarity > 0.8) return true;
            }
        }
        return false;
    }

    private Solution getBestSolution(List<Solution> population) {
        return population.stream()
            .min(Comparator.comparingInt(s -> s.totalCost))
            .orElse(population.get(0));
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

    private void printProgress(int generation, Solution best, List<Solution> population, long timeRemaining) {
        double avgCost = population.stream().mapToInt(s -> s.totalCost).average().orElse(0);
        double diversity = calculateDiversity(population);

        System.out.println(String.format("  Gen %d - Best: %d, Avg: %.0f, Diversity: %.2f, Time left: %dms",
            generation, best.totalCost, avgCost, diversity, timeRemaining));
    }

    private void printOperatorStats() {
        System.out.println("\nOperator Statistics:");
        for (Map.Entry<String, OperatorStats> entry : operatorStats.entrySet()) {
            OperatorStats stats = entry.getValue();
            double successRate = stats.attempts > 0 ? (double)stats.successes / stats.attempts * 100 : 0;
            double avgImprovement = stats.attempts > 0 ? (double)stats.totalImprovement / stats.attempts : 0;
            System.out.println(String.format("  %s: %d attempts, %.1f%% success, avg improvement: %.1f",
                entry.getKey(), stats.attempts, successRate, avgImprovement));
        }
    }

    // Helper class for operator statistics
    private static class OperatorStats {
        int attempts = 0;
        int successes = 0;
        int totalImprovement = 0;
    }
}