package LocalSearch.IteratedLocalSearch;

import Utilities.ExperimentResult;
import Utilities.ExperimentRunner;
import Utilities.Instance;
import Utilities.Solution;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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

    public List<Solution> runExperiments(Instance instance, int numRuns, int numIterationsPerRun, long timePerIteration) {
        List<Solution> solutions = new ArrayList<>();
        System.out.printf("\n--- Running ILS %d times (%d iterations each, time limit %dms/iter) ---\n", numRuns, numIterationsPerRun, timePerIteration);

        for (int i = 0; i < numRuns; i++) {
            System.out.printf("Starting ILS Run %d/%d...\n", i + 1, numRuns);
            Solution solution = solver.iteratedLocalSearch(instance, numIterationsPerRun, timePerIteration);
            System.out.printf("ILS Run %d Complete: Cost = %d, Total Time = %dms\n", i + 1, solution.totalCost, solution.totalRunningTime);
            solutions.add(solution);
        }

        if (baseOutputDir != null) {
            try {
                saveResults(instance, solutions, baseOutputDir);
                System.out.println("ILS results saved to: " + baseOutputDir);
            } catch (IOException e) {
                System.err.println("Error saving ILS results: " + e.getMessage());
            }
        }

        return solutions;
    }

    private void saveResults(Instance instance, List<Solution> solutions, String outputDir) throws IOException {
        java.io.File dir = new java.io.File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ExperimentResult overallResult = experimentStatsCalculations(instance, "ILS_Overall", solutions);
        String summaryFile = outputDir + "/ils_summary.csv";
        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.append("Statistic,Value\n");
            writer.append("MinCost,").append(String.valueOf(overallResult.minCost)).append("\n");
            writer.append("MaxCost,").append(String.valueOf(overallResult.maxCost)).append("\n");
            writer.append("AvgCost,").append(String.format("%.2f", overallResult.avgCost)).append("\n");
            writer.append("AvgRunningTime,").append(String.format("%.2f", overallResult.avgRunningTime)).append("\n");
        }

        String solutionsFile = outputDir + "/ils_solutions.csv";
        try (FileWriter writer = new FileWriter(solutionsFile)) {
            writer.append("Run,TotalCost,TotalDistance,TotalRunningTime,LocalSearchRuns,Cycle\n");
            for (int i = 0; i < solutions.size(); i++) {
                Solution sol = solutions.get(i);
                String cycleStr = sol.cycle.toString().replace("[", "").replace("]", "").replace(", ", "-");
                writer.append(String.format("%d,%d,%d,%d,%d,\"%s\"\n",
                        i + 1,
                        sol.totalCost,
                        sol.totalDistance,
                        sol.totalRunningTime,
                        sol.localSearchRuns,
                        cycleStr));
            }
        }
    }
}
