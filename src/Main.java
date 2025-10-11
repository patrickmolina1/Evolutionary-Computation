
import Utilities.*;
import GreedyHeuristics.*;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {



        try {
            Instance instance = new Instance("./raw_data/TSPB.csv", "TSPB");

            // Create experiment runner
            ExperimentRunner runner = new ExperimentRunner();

            // Run experiments (e.g., 100 iterations per method)
            System.out.println("Running experiments...");
            List<ExperimentResult> results = runner.runExperiments(instance, 100);

            // Export results to CSV
            System.out.println("Exporting results...");
            // ensure output directory exists
            java.io.File outDir = new java.io.File("src/Results/" + instance.name);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            runner.exportResults(results, outDir.getPath());

            System.out.println("Experiments completed successfully!");

            // Print summary
            for (ExperimentResult result : results) {
                System.out.printf("%s - Min: %d, Max: %d, Avg: %.2f\n",
                        result.methodName, result.minCost, result.maxCost, result.avgCost);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

