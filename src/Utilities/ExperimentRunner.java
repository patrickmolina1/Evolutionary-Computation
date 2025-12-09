package Utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public abstract class ExperimentRunner {



    public ExperimentRunner() {

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
            writer.append("Instance,Method,MinCost,MaxCost,AvgCost,MinTime,MaxTime,AvgTime,MinIterations,MaxIterations,AvgIterations,NumSolutions,BestSolutionID");
            writer.append(System.lineSeparator());

            // Write data
            for (ExperimentResult result : results) {
                writer.append(String.format("%s,%s,%d,%d,%.2f,%d,%d,%.2f,%d,%d,%.2f,%d,%d",
                        result.instanceName,
                        result.methodName,
                        result.minCost,
                        result.maxCost,
                        result.avgCost,
                        result.minRunningTime,
                        result.maxRunningTime,
                        result.avgRunningTime,
                        result.minIterations,
                        result.maxIterations,
                        result.avgIterations,
                        result.numSolutions,
                        result.bestSolutionId));
                writer.append(System.lineSeparator());
            }
        }
    }


    private void exportSolutionsCSV(ExperimentResult result, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.append("SolutionID,TotalCost,NumNodes,TotalDistance,ObjectiveFunction,TotalRunningTime,TotalIterations,Cycle\n");

            // Write each solution
            for (int i = 0; i < result.solutions.size(); i++) {
                Solution sol = result.solutions.get(i);

                int objectiveFunction = sol.totalCost + sol.totalDistance;

                // Convert cycle to string
                String cycleStr = sol.cycle.toString()
                        .replace("[", "")
                        .replace("]", "")
                        .replace(", ", "-");

                writer.append(String.format("%d,%d,%d,%d,%d,%d,%d,\"%s\"\n",
                        i + 1,
                        sol.totalCost,
                        sol.selectedNodes.size(),
                        sol.totalDistance,
                        objectiveFunction,
                        sol.totalRunningTime,
                        sol.iterations,
                        cycleStr));
            }
        }
    }

    public ExperimentResult experimentStatsCalculations(Instance instance, String methodName, List<Solution> solutions){
        // Calculate statistics and find best solution
        int minCost = Integer.MAX_VALUE;
        int maxCost = 0;

        int bestSolutionId = 0;

        int minRunningTime = Integer.MAX_VALUE;
        int maxRunningTime = 0;

        int minIterations = Integer.MAX_VALUE;
        int maxIterations = 0;

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

            int iterations = solutions.get(i).iterations;
            if (iterations < minIterations) {
                minIterations = iterations;
            }
            if (iterations > maxIterations) {
                maxIterations = iterations;
            }
        }

        double avgCost = solutions.stream().mapToInt(s -> s.totalCost).average().orElse(0.0);
        double avgRunningTime = solutions.stream().mapToInt(s -> s.totalRunningTime).average().orElse(0.0);
        double avgIterations = solutions.stream().mapToInt(s -> s.iterations).average().orElse(0.0);

        return new ExperimentResult(
                instance.name,
                methodName,
                minCost,
                maxCost,
                avgCost,
                minRunningTime,
                maxRunningTime,
                avgRunningTime,
                minIterations,
                maxIterations,
                avgIterations,
                solutions.size(),
                bestSolutionId,
                solutions
        );
    }


}
