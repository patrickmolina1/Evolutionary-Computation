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
        List<Solution> allSolutionsAcrossRuns = new ArrayList<>();

        System.out.printf("\n--- Running ILS %d times (time limit %dms/run) ---\n", numRuns, timePerRun);

        for (int i = 0; i < numRuns; i++) {
            System.out.printf("\n=== Starting ILS Run %d/%d... ===\n", i + 1, numRuns);
            long runStartTime = System.currentTimeMillis();
            List<Solution> solutionsFromRun = solver.iteratedLocalSearch(instance, timePerRun);
            long runEndTime = System.currentTimeMillis();

            ExperimentResult result = experimentStatsCalculations(instance, "ILS_Run_" + (i + 1), solutionsFromRun);
            result.runTotalTime = runEndTime - runStartTime;
            result.solutions = solutionsFromRun; // Store solutions for this run

            Solution bestInRun = solutionsFromRun.stream().min(Comparator.comparingInt(s -> s.totalCost)).orElse(null);
            if (bestInRun != null) {
                System.out.printf("ILS Run %d Complete: Best Cost = %d, Total Time = %dms, LS Runs = %d\n",
                        i + 1, bestInRun.totalCost, result.runTotalTime, solutionsFromRun.size());
            }

            results.add(result);
            allSolutionsAcrossRuns.addAll(solutionsFromRun);

            if (baseOutputDir != null) {
                try {
                    saveRunResults(result, i + 1);
                } catch (IOException e) {
                    System.err.println("Error saving results for run " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        if (baseOutputDir != null) {
            try {
                createOverallSummary(instance, results, allSolutionsAcrossRuns);
                System.out.println("\nOverall ILS summary created for all " + numRuns + " runs.");
            } catch (IOException e) {
                System.err.println("Error creating overall ILS summary: " + e.getMessage());
            }
        }

        return results;
    }

    private void saveRunResults(ExperimentResult result, int runNumber) throws IOException {
        String runDir = baseOutputDir + "/run_" + runNumber;
        java.io.File dir = new java.io.File(runDir);
        if (!dir.exists()) dir.mkdirs();

        String summaryFile = runDir + "/run_summary.csv";
        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.append("Metric,Value\n");
            writer.append("BestCost,").append(String.valueOf(result.minCost)).append("\n");
            writer.append("TotalRunTime,").append(String.valueOf(result.runTotalTime)).append("\n");
            writer.append("LocalSearchRuns,").append(String.valueOf(result.numSolutions)).append("\n");
        }
    }

    private void createOverallSummary(Instance instance, List<ExperimentResult> results, List<Solution> allSolutions) throws IOException {
        java.io.File dir = new java.io.File(baseOutputDir);
        if (!dir.exists()) dir.mkdirs();

        // Overall summary CSV with run-by-run breakdown
        String summaryFile = baseOutputDir + "/overall_summary.csv";
        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.append("Run,BestCost,TotalRunTime,LocalSearchRuns\n");
            for (int i = 0; i < results.size(); i++) {
                ExperimentResult res = results.get(i);
                writer.append(String.format("%d,%d,%d,%d\n",
                        i + 1,
                        res.minCost,
                        res.runTotalTime,
                        res.numSolutions));
            }

            // Calculate overall stats
            double avgCost = results.stream().mapToInt(r -> r.minCost).average().orElse(0.0);
            int minCost = results.stream().mapToInt(r -> r.minCost).min().orElse(0);
            int maxCost = results.stream().mapToInt(r -> r.minCost).max().orElse(0);

            // Calculate local search runs stats
            int minLsRuns = results.stream().mapToInt(r -> r.numSolutions).min().orElse(0);
            int maxLsRuns = results.stream().mapToInt(r -> r.numSolutions).max().orElse(0);
            double avgLsRuns = results.stream().mapToInt(r -> r.numSolutions).average().orElse(0.0);

            double avgRunTime = results.stream().mapToLong(r -> r.runTotalTime).average().orElse(0.0);

            writer.append("\nOVERALL,MinCost,MaxCost,AvgCost,MinLocalSearchRuns,MaxLocalSearchRuns,AvgLocalSearchRuns,AvgRunTime\n");
            writer.append(String.format(",%d,%d,%.2f,%d,%d,%.2f,%.2f\n",
                    minCost, maxCost, avgCost, minLsRuns, maxLsRuns, avgLsRuns, avgRunTime));
        }

        // All solutions to a single CSV
        String allSolutionsFile = baseOutputDir + "/all_solutions.csv";
        try (FileWriter writer = new FileWriter(allSolutionsFile)) {
            writer.append("Run,IterationInRun,TotalCost,TotalDistance,IterationRunningTime,Cycle\n");
            int globalIteration = 1;
            for (int i = 0; i < results.size(); i++) {
                List<Solution> runSolutions = results.get(i).solutions;
                for (int j = 0; j < runSolutions.size(); j++) {
                    Solution sol = runSolutions.get(j);
                    String cycleStr = sol.cycle.toString().replace("[", "").replace("]", "").replace(", ", "-");
                    writer.append(String.format("%d,%d,%d,%d,%d,\"%s\"\n",
                            i + 1,
                            j + 1,
                            sol.totalCost,
                            sol.totalDistance,
                            sol.totalRunningTime,
                            cycleStr));
                }
            }
        }
    }
}
