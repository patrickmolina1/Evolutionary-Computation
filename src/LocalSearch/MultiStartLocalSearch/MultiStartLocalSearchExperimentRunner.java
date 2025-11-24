package LocalSearch.MultiStartLocalSearch;

import LocalSearch.IntraRouteMoveType;
import LocalSearch.StartingSolutionType;
import Utilities.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;

public class MultiStartLocalSearchExperimentRunner extends ExperimentRunner {
    public MultiStartLocalSearchSolver solver;
    private String baseOutputDir;

    public MultiStartLocalSearchExperimentRunner(){
        this.solver = new MultiStartLocalSearchSolver();
    }

    public void setBaseOutputDir(String baseOutputDir) {
        this.baseOutputDir = baseOutputDir;
    }

    public List<ExperimentResult> runExperiments(Instance instance, int numIterations, int numRuns) {
        List<ExperimentResult> results = new ArrayList<>();
        List<Solution> allSolutions = new ArrayList<>();

        for (int run = 0; run < numRuns; run++){
            System.out.println("\n=== Starting Run " + (run + 1) + " of " + numRuns + " ===");

            long runStartTime = System.currentTimeMillis();
            ExperimentResult result = testMethod(instance, "MSLS", numIterations);
            long runEndTime = System.currentTimeMillis();
            result.runTotalTime = runEndTime - runStartTime;

            System.out.println("Run " + (run + 1) + " completed in " + result.runTotalTime + " ms (" + (result.runTotalTime / 1000.0) + " seconds)");

            results.add(result);
            allSolutions.addAll(result.solutions);

            // Save this run's results in its own folder
            if (baseOutputDir != null) {
                try {
                    String runDir = baseOutputDir + "/run_" + (run + 1);
                    java.io.File dir = new java.io.File(runDir);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    // Export this run's results
                    List<ExperimentResult> singleRunResult = new ArrayList<>();
                    singleRunResult.add(result);
                    exportResults(singleRunResult, runDir);

                    //System.out.println("Run " + (run + 1) + " results saved to: " + runDir);
                } catch (IOException e) {
                    System.err.println("Error saving results for run " + (run + 1) + ": " + e.getMessage());
                }
            }
        }

        // Calculate and display run time statistics
        double avgRunTime = results.stream().mapToLong(r -> r.runTotalTime).average().orElse(0.0);
        long minRunTime = results.stream().mapToLong(r -> r.runTotalTime).min().orElse(0);
        long maxRunTime = results.stream().mapToLong(r -> r.runTotalTime).max().orElse(0);

        System.out.println("\n=== Run Time Statistics ===");
        System.out.println("Min Run Time: " + minRunTime + " ms (" + (minRunTime / 1000.0) + " seconds)");
        System.out.println("Max Run Time: " + maxRunTime + " ms (" + (maxRunTime / 1000.0) + " seconds)");
        System.out.println("Avg Run Time: " + String.format("%.2f", avgRunTime) + " ms (" + String.format("%.2f", avgRunTime / 1000.0) + " seconds)");

        // Create overall summary for all runs
        if (baseOutputDir != null) {
            try {
                createOverallSummary(instance, results, allSolutions, baseOutputDir);
                System.out.println("\nOverall summary created for all " + numRuns + " runs (" + allSolutions.size() + " total solutions)");
            } catch (IOException e) {
                System.err.println("Error creating overall summary: " + e.getMessage());
            }
        }

        return results;
    }

    private void createOverallSummary(Instance instance, List<ExperimentResult> results,
                                     List<Solution> allSolutions, String outputDir) throws IOException {
        // Create overall statistics for all solutions
        ExperimentResult overallResult = experimentStatsCalculations(instance, "MSLS_Overall", allSolutions);

        // Calculate average run time
        double avgRunTime = results.stream().mapToLong(r -> r.runTotalTime).average().orElse(0.0);
        long minRunTime = results.stream().mapToLong(r -> r.runTotalTime).min().orElse(0);
        long maxRunTime = results.stream().mapToLong(r -> r.runTotalTime).max().orElse(0);

        // Export overall summary CSV with run-by-run breakdown
        String summaryFile = outputDir + "/overall_summary.csv";
        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.append("Run,MinCost,MaxCost,AvgCost,MinTime,MaxTime,AvgTime,NumSolutions,BestSolutionID,TotalRunTime\n");

            // Write each run's summary
            for (int i = 0; i < results.size(); i++) {
                ExperimentResult result = results.get(i);
                writer.append(String.format("%d,%d,%d,%.2f,%d,%d,%.2f,%d,%d,%d\n",
                        i + 1,
                        result.minCost,
                        result.maxCost,
                        result.avgCost,
                        result.minRunningTime,
                        result.maxRunningTime,
                        result.avgRunningTime,
                        result.numSolutions,
                        result.bestSolutionId,
                        result.runTotalTime));
            }

            // Write overall statistics
            writer.append(String.format("\nOVERALL,%d,%d,%.2f,%d,%d,%.2f,%d,%d,%.2f\n",
                    overallResult.minCost,
                    overallResult.maxCost,
                    overallResult.avgCost,
                    overallResult.minRunningTime,
                    overallResult.maxRunningTime,
                    overallResult.avgRunningTime,
                    overallResult.numSolutions,
                    overallResult.bestSolutionId,
                    avgRunTime));

            // Write run time statistics
            writer.append(String.format("\nRUN TIME STATS:,MinRunTime,MaxRunTime,AvgRunTime\n"));
            writer.append(String.format(",%d,%d,%.2f\n", minRunTime, maxRunTime, avgRunTime));
        }

        // Export all solutions to a single CSV
        String allSolutionsFile = outputDir + "/all_solutions.csv";
        try (FileWriter writer = new FileWriter(allSolutionsFile)) {
            writer.append("Run,IterationInRun,GlobalIteration,TotalCost,NumNodes,TotalDistance,ObjectiveFunction,TotalRunningTime,Cycle\n");

            int globalIteration = 1;
            for (int run = 0; run < results.size(); run++) {
                List<Solution> runSolutions = results.get(run).solutions;
                for (int i = 0; i < runSolutions.size(); i++) {
                    Solution sol = runSolutions.get(i);
                    int objectiveFunction = sol.totalCost + sol.totalDistance;
                    String cycleStr = sol.cycle.toString()
                            .replace("[", "")
                            .replace("]", "")
                            .replace(", ", "-");

                    writer.append(String.format("%d,%d,%d,%d,%d,%d,%d,%d,\"%s\"\n",
                            run + 1,
                            i + 1,
                            globalIteration++,
                            sol.totalCost,
                            sol.selectedNodes.size(),
                            sol.totalDistance,
                            objectiveFunction,
                            sol.totalRunningTime,
                            cycleStr));
                }
            }
        }
    }

    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {

        List<Solution> solutions = new ArrayList<>();

        for (int i=0;i<numIterations;i++){
            Solution solution = null;

            switch (methodName){
                case "MSLS":
                    solution = solver.steepestLocalSearch(instance, StartingSolutionType.RANDOM, IntraRouteMoveType.EDGE_EXCHANGE);
                    break;
            }

            if (solution != null){
                System.out.println("Iteration " + (i+1) + ": Cost = " + solution.totalCost + ", Running Time = " + solution.totalRunningTime + "ms");
                solutions.add(solution);
            }

        }

        return experimentStatsCalculations(instance, methodName, solutions);
    }


}