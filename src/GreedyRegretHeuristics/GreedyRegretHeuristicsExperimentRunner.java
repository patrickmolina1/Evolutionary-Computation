package GreedyRegretHeuristics;

import Utilities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GreedyRegretHeuristicsExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {

    public GreedyRegretHeuristicsSolver solver;

    public GreedyRegretHeuristicsExperimentRunner() {
        this.solver = new GreedyRegretHeuristicsSolver();

    }

    @Override
    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
        List<ExperimentResult> results = new ArrayList<>();

        // Test each method
        results.add(testMethod(instance, "greedy2RegretNearest", numIterations));
        results.add(testMethod(instance, "greedyWeightedRegret", numIterations));


        return results;
    }

    @Override
    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {
        List<Solution> solutions = new ArrayList<>();


        for (int i = 0; i < numIterations; i++) {
            Solution solution = null;

            switch (methodName) {
                case "greedy2RegretNearest":
                    solution = solver.greedy2RegretNearest(instance, instance.nodes.get(i));
                    break;
                case "greedyWeightedRegret":
                    solution = solver.greedyWeightedRegret(instance, instance.nodes.get(i), 0.5, 0.5);
                    break;
            }

            if (solution != null) {
                solutions.add(solution);
            }
        }

        // Calculate statistics and find best solution
        int minCost = Integer.MAX_VALUE;
        int maxCost = 0;
        int bestSolutionId = 0;
        int minRunningTime = Integer.MAX_VALUE;
        int maxRunningTime = 0;

        for (int i = 0; i < solutions.size(); i++) {
            int cost = solutions.get(i).totalCost;
            if (cost < minCost) {
                minCost = cost;
                bestSolutionId = i + 1; // 1-based index
            }
            if (cost > maxCost) {
                maxCost = cost;
            }
            int runningTime = solutions.get(i).totalRunningTime;
            if (runningTime < minRunningTime) {
                minRunningTime = runningTime;
            }
            if (runningTime > maxRunningTime) {
                maxRunningTime = runningTime;
            }
        }

        double avgCost = solutions.stream().mapToInt(s -> s.totalCost).average().orElse(0.0);
        double avgRunningTime = solutions.stream().mapToInt(s -> s.totalRunningTime).average().orElse(0.0);

        return new ExperimentResult(
                instance.name,
                methodName,
                minCost,
                maxCost,
                avgCost,
                minRunningTime,
                maxRunningTime,
                avgRunningTime,
                solutions.size(),
                bestSolutionId,
                solutions
        );
    }


}
