package GreedyHeuristics;

import Utilities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GreedyHeuristicsExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {

    public GreedyHeuristicsSolver solver;

    public GreedyHeuristicsExperimentRunner() {
        this.solver = new GreedyHeuristicsSolver();

    }

    @Override
    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
        List<ExperimentResult> results = new ArrayList<>();

        // Test each method
        results.add(testMethod(instance, "RandomSolution", numIterations));
        results.add(testMethod(instance, "NearestNeighborEndOnly", numIterations));
        results.add(testMethod(instance, "NearestNeighborAllPositions", numIterations));
        results.add(testMethod(instance, "GreedyCycle", numIterations));


        return results;
    }

    @Override
    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {
        List<Solution> solutions = new ArrayList<>();

        for (int i = 0; i < numIterations; i++) {
            Solution solution = null;

            switch (methodName) {
                case "RandomSolution":
                    solution = solver.randomSolution(instance);
                    break;
                case "NearestNeighborEndOnly":
                    solution = solver.nearestNeighborEndOnly(instance);
                    break;
                case "NearestNeighborAllPositions":
                    solution = solver.nearestNeighborAllPositions(instance);
                    break;
                case "GreedyCycle":
                    // Run for each node as starting point
                    Random rand = new Random();
                    Node startNode = instance.nodes.get(rand.nextInt(instance.nodes.size()));
                    solution = solver.greedyCycle(instance, startNode);
                    break;
            }

            if (solution != null) {
                solutions.add(solution);
            }
        }

        return experimentStatsCalculations(instance, methodName, solutions);
    }



}
