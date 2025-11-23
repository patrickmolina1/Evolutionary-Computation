package LocalSearch.IteratedLocalSearch;

import Utilities.ExperimentResult;
import Utilities.ExperimentRunner;
import Utilities.Instance;
import Utilities.Solution;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class IteratedLocalSearchExperimentRunner extends ExperimentRunner {
    public IteratedLocalSearchSolver solver;
    private String baseOutputDir;

    public IteratedLocalSearchExperimentRunner() {
        this.solver = new IteratedLocalSearchSolver();
    }

    public void setBaseOutputDir(String baseOutputDir) {
        this.baseOutputDir = baseOutputDir;
    }

    public List<ExperimentResult> runExperiments(Instance instance, int numRuns, long timePerRun) {
        List<ExperimentResult> results = new ArrayList<>();

        System.out.printf("\n--- Running ILS %d times (time limit %dms/run) ---\n", numRuns, timePerRun);

        for (int i = 0; i < numRuns; i++) {
            System.out.printf("\n=== Starting ILS Run %d/%d... ===\n", i + 1, numRuns);
            long runStartTime = System.currentTimeMillis();
            List<Solution> solutionsFromRun = solver.iteratedLocalSearch(instance, timePerRun);
            long runEndTime = System.currentTimeMillis();

            ExperimentResult result = experimentStatsCalculations(instance, "IteratedLocalSearch", solutionsFromRun);
            result.runTotalTime = runEndTime - runStartTime;
            result.solutions = solutionsFromRun; // Store solutions for this run

            Solution bestInRun = solutionsFromRun.stream().min(Comparator.comparingInt(s -> s.totalCost)).orElse(null);
            if (bestInRun != null) {
                System.out.printf("ILS Run %d Complete: Best Cost = %d, Total Time = %dms, LS Runs = %d\n",
                        i + 1, bestInRun.totalCost, result.runTotalTime, solutionsFromRun.size());
            }

            results.add(result);

            if (baseOutputDir != null) {
                try {
                    saveRunResults(instance, result, i + 1);
                } catch (IOException e) {
                    System.err.println("Error saving results for run " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        if (baseOutputDir != null) {
            try {
                createOverallSummary(instance, results);
                System.out.println("\nOverall ILS summary created for all " + numRuns + " runs.");
            } catch (IOException e) {
                System.err.println("Error creating overall ILS summary: " + e.getMessage());
            }
        }

        return results;
    }

    private void saveRunResults(Instance instance, ExperimentResult result, int runNumber) throws IOException {
        // Create directory structure: IteratedResults/TSPA or TSPB/run_i
        String instanceDir = baseOutputDir + "/" + instance.name;
        String runDir = instanceDir + "/run_" + runNumber;
        java.io.File dir = new java.io.File(runDir);
        if (!dir.exists()) dir.mkdirs();

        // Save run_summary.csv
        String summaryFile = runDir + "/run_summary.csv";
        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.append("Instance,Method,MinCost,MaxCost,AvgCost,MinTime,MaxTime,AvgTime,MinLsRuns,MaxLsRuns,AvgLsRuns,NumSolutions,BestSolutionID\n");
            writer.append(String.format("%s,%s,%d,%d,%.2f,%d,%d,%.2f,%d,%d,%.2f,%d,%d\n",
                    result.instanceName,
                    result.methodName,
                    result.minCost,
                    result.maxCost,
                    result.avgCost,
                    result.minRunningTime,
                    result.maxRunningTime,
                    result.avgRunningTime,
                    result.numSolutions,  // MinLsRuns (single run, so min=max=avg=numSolutions)
                    result.numSolutions,  // MaxLsRuns
                    (double)result.numSolutions,  // AvgLsRuns
                    result.numSolutions,
                    result.bestSolutionId));
        }

        // Save all_solutions.csv for this run
        String allSolutionsFile = runDir + "/" + instance.name + "_Iterated_all_solutions.csv";
        try (FileWriter writer = new FileWriter(allSolutionsFile)) {
            writer.append("SolutionID,TotalCost,NumNodes,TotalDistance,ObjectiveFunction,TotalRunningTime,Cycle\n");
            for (int i = 0; i < result.solutions.size(); i++) {
                Solution sol = result.solutions.get(i);
                String cycleStr = sol.cycle.toString().replace("[", "").replace("]", "").replace(", ", "-");
                writer.append(String.format("%d,%d,%d,%d,%d,%d,\"%s\"\n",
                        i + 1,
                        sol.totalCost,
                        sol.selectedNodes.size(),
                        sol.totalDistance,
                        sol.totalCost,  // ObjectiveFunction is same as TotalCost
                        sol.totalRunningTime,
                        cycleStr));
            }
        }
    }

    private void createOverallSummary(Instance instance, List<ExperimentResult> results) throws IOException {
        // Create instance directory
        String instanceDir = baseOutputDir + "/" + instance.name;
        java.io.File dir = new java.io.File(instanceDir);
        if (!dir.exists()) dir.mkdirs();

        // Calculate overall statistics
        int minCost = results.stream().mapToInt(r -> r.minCost).min().orElse(0);
        int maxCost = results.stream().mapToInt(r -> r.minCost).max().orElse(0);
        double avgCost = results.stream().mapToInt(r -> r.minCost).average().orElse(0.0);

        long minTime = results.stream().mapToLong(r -> r.runTotalTime).min().orElse(0);
        long maxTime = results.stream().mapToLong(r -> r.runTotalTime).max().orElse(0);
        double avgTime = results.stream().mapToLong(r -> r.runTotalTime).average().orElse(0.0);

        int minLsRuns = results.stream().mapToInt(r -> r.numSolutions).min().orElse(0);
        int maxLsRuns = results.stream().mapToInt(r -> r.numSolutions).max().orElse(0);
        double avgLsRuns = results.stream().mapToInt(r -> r.numSolutions).average().orElse(0.0);

        // Find best solution across all runs
        int bestRunId = 1;
        int bestSolutionId = 1;
        int bestCostOverall = Integer.MAX_VALUE;

        for (int i = 0; i < results.size(); i++) {
            ExperimentResult result = results.get(i);
            if (result.minCost < bestCostOverall) {
                bestCostOverall = result.minCost;
                bestRunId = i + 1;
                bestSolutionId = result.bestSolutionId;
            }
        }

        long totalRunningTime = results.stream().mapToLong(r -> r.runTotalTime).sum();

        // Save runs_overall_summary.csv
        String overallSummaryFile = instanceDir + "/runs_overall_summary.csv";
        try (FileWriter writer = new FileWriter(overallSummaryFile)) {
            writer.append("Instance,Method,totalRuns,MinCost,MaxCost,AvgCost,MinTime,MaxTime,AvgTime,MinLsRuns,MaxLsRuns,AvgLsRuns,BestSolutionRunID,BestSolutionID,TotalRunningTime\n");
            writer.append(String.format("%s,%s,%d,%d,%d,%.2f,%d,%d,%.2f,%d,%d,%.2f,%d,%d,%d\n",
                    instance.name,
                    "IteratedLocalSearch",
                    results.size(),
                    minCost,
                    maxCost,
                    avgCost,
                    minTime,
                    maxTime,
                    avgTime,
                    minLsRuns,
                    maxLsRuns,
                    avgLsRuns,
                    bestRunId,
                    bestSolutionId,
                    totalRunningTime));
        }
    }
}
