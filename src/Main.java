import LocalSearch.IteratedLocalSearch.IteratedLocalSearchExperimentRunner;
import Utilities.ExperimentResult;
import Utilities.Instance;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        Instance instance = new Instance("./raw_data/TSPB.csv", "TSPB");
        long totalStartTime = System.currentTimeMillis();

        // --- Manually enter the average MSLS *run* time here ---
        // This is the stopping condition for each ILS run.
        // From your file, this value is 50284.55, which we can round to 50285.
        long avgMslsRunTime = 502;

        int numIlsRuns = 1; // Run the whole ILS process 20 times.

        // --- RUN ILS EXPERIMENT ---
        System.out.println("--- Running ILS Experiment ---");
        IteratedLocalSearchExperimentRunner ilsRunner = new IteratedLocalSearchExperimentRunner();

        // Set the base output directory for all results
        String ilsOutputDir = "./results/IteratedResults" + instance.name;
        ilsRunner.setBaseOutputDir(ilsOutputDir);

        // Run the experiments
        List<ExperimentResult> ilsResults = ilsRunner.runExperiments(instance, numIlsRuns, avgMslsRunTime);

        // --- FINAL CONSOLE SUMMARY ---
        System.out.println("\n--- ILS Experiment Completed ---");
        System.out.println("Full results saved in: " + ilsOutputDir);

        // Calculate and print overall statistics from the best result of each run
        int minCost = ilsResults.stream().mapToInt(r -> r.minCost).min().orElse(0);
        int maxCost = ilsResults.stream().mapToInt(r -> r.minCost).max().orElse(0);
        double avgCost = ilsResults.stream().mapToInt(r -> r.minCost).average().orElse(0.0);
        double avgRunTime = ilsResults.stream().mapToLong(r -> r.runTotalTime).average().orElse(0.0);
        double avgLsRuns = ilsResults.stream().mapToInt(r -> r.numSolutions).average().orElse(0.0);

        System.out.printf("\n--- Overall ILS Results (from %d runs) ---\n", numIlsRuns);
        System.out.printf("Min Best Cost: %d\n", minCost);
        System.out.printf("Max Best Cost: %d\n", maxCost);
        System.out.printf("Avg Best Cost: %.2f\n", avgCost);
        System.out.printf("Avg Total Running Time per run: %.2f ms\n", avgRunTime);
        System.out.printf("Avg Basic Local Searches per run: %.2f\n", avgLsRuns);

        long totalEndTime = System.currentTimeMillis();
        System.out.println("\nTotal Execution Time: " + (totalEndTime - totalStartTime) + " ms");
    }
}
