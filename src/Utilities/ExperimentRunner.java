package Utilities;

import GreedyHeuristics.Solver;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ExperimentRunner {

    private Solver solver;

    public ExperimentRunner() {
        this.solver = new Solver();
    }

    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
        List<ExperimentResult> results = new ArrayList<>();

        // Test each method
        results.add(testMethod(instance, "RandomSolution", numIterations));
        results.add(testMethod(instance, "NearestNeighborEndOnly", numIterations));
        results.add(testMethod(instance, "NearestNeighborAllPositions", numIterations));
        results.add(testMethod(instance, "GreedyCycle", numIterations));

        return results;
    }

    private ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {
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

        // Calculate statistics and find best solution
        int minCost = Integer.MAX_VALUE;
        int maxCost = 0;
        int bestSolutionId = 0;

        for (int i = 0; i < solutions.size(); i++) {
            int cost = solutions.get(i).totalCost;
            if (cost < minCost) {
                minCost = cost;
                bestSolutionId = i + 1; // 1-based index
            }
            if (cost > maxCost) {
                maxCost = cost;
            }
        }

        double avgCost = solutions.stream().mapToInt(s -> s.totalCost).average().orElse(0.0);

        return new ExperimentResult(
                instance.name,
                methodName,
                minCost,
                maxCost,
                avgCost,
                solutions.size(),
                bestSolutionId,
                solutions
        );
    }

    public void exportResults(List<ExperimentResult> results, String outputDir) throws IOException {
        // Export summary CSV
        exportSummaryCSV(results, outputDir + "/experiment_summary.csv");

        // Export detailed solutions for each method
        for (ExperimentResult result : results) {
            String filename = String.format("%s/%s_%s_solutions.csv",
                    outputDir,
                    result.instanceName,
                    result.methodName);
            exportSolutionsCSV(result, filename);
        }
    }

    private void exportSummaryCSV(List<ExperimentResult> results, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.append("Instance,Method,MinCost,MaxCost,AvgCost,NumSolutions,BestSolutionID\n");

            // Write data
            for (ExperimentResult result : results) {
                writer.append(String.format("%s,%s,%d,%d,%.2f,%d,%d\n",
                        result.instanceName,
                        result.methodName,
                        result.minCost,
                        result.maxCost,
                        result.avgCost,
                        result.numSolutions,
                        result.bestSolutionId));
            }
        }
    }

    private void exportSolutionsCSV(ExperimentResult result, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.append("SolutionID,TotalCost,NumNodes,TotalDistance,ObjectiveFunction,Cycle\n");

            // Write each solution
            for (int i = 0; i < result.solutions.size(); i++) {
                Solution sol = result.solutions.get(i);

                int objectiveFunction = sol.totalCost + sol.totalDistance;

                // Convert cycle to string
                String cycleStr = sol.cycle.toString()
                        .replace("[", "")
                        .replace("]", "")
                        .replace(", ", "-");

                writer.append(String.format("%d,%d,%d,%d,%d,\"%s\"\n",
                        i + 1,
                        sol.totalCost,
                        sol.selectedNodes.size(),
                        sol.totalDistance,
                        objectiveFunction,
                        cycleStr));
            }
        }
    }
}
