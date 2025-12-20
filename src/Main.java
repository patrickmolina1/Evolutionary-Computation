import HybridEvolutionary.HybridEvolutionaryExperimentRunner;

import OwnMethod.OwnMethodExperimentRunner;
import Utilities.ExperimentResult;
import Utilities.Instance;
import java.io.IOException;
import java.util.List;



public class Main {

    public static void main(String[] args) throws IOException {
        try {
            Instance instance = new Instance("./raw_data/TSPA.csv", "TSPA");

            // Create experiment runner
            OwnMethodExperimentRunner runner = new OwnMethodExperimentRunner();

            // Run experiments (e.g., 100 iterations per method)
            System.out.println("Running experiments...");
            List<ExperimentResult> results = runner.runExperiments(instance, 20);

            // Export results to CSV
            System.out.println("Exporting results...");

            // ensure output directory exists
            java.io.File outDir = new java.io.File("src/Results/OwnMethod/" + instance.name);
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