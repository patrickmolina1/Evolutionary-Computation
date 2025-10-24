
import GreedyHeuristics.GreedyHeuristicsExperimentRunner;
import GreedyHeuristics.GreedyHeuristicsSolver;
import GreedyRegretHeuristics.GreedyRegretHeuristicsExperimentRunner;
import LocalSearch.LocalSearchExperimentRunner;
import Utilities.ExperimentRunner;
import Utilities.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {



        try {
            Instance instance = new Instance("./raw_data/TSPA.csv", "TSPA");

            // Create experiment runner

            long startTime = System.currentTimeMillis();

            LocalSearchExperimentRunner runner = new LocalSearchExperimentRunner();

            // Run experiments (e.g., 100 iterations per method)
            System.out.println("Running experiments...");
            List<ExperimentResult> results = runner.runExperiments(instance, 200);

            // Export results to CSV
            System.out.println("Exporting results...");
            // ensure output directory exists
            java.io.File outDir = new java.io.File("src/Results/LocalSearch/" + instance.name);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            runner.exportResults(results, outDir.getPath());

            System.out.println("Experiments completed successfully!");

            // Print summary
            for (ExperimentResult result : results) {
                System.out.printf("%s - Min: %d, Max: %d, Avg: %.2f, RunningTime: %.2f\n",
                        result.methodName, result.minCost, result.maxCost, result.avgCost, result.avgRunningTime);
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Total Execution Time: " + (endTime - startTime));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

