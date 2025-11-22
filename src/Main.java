
import GreedyHeuristics.GreedyHeuristicsExperimentRunner;
import GreedyHeuristics.GreedyHeuristicsSolver;
import GreedyRegretHeuristics.GreedyRegretHeuristicsExperimentRunner;
import LocalSearch.LocalSearchCandidateMoves.LocalSearchCandidateMovesExperimentRunner;
import LocalSearch.LocalSearchExperimentRunner;
import LocalSearch.MultiStartLocalSearch.MultiStartLocalSearchExperimentRunner;
import LocalSearch.MultiStartLocalSearch.MultiStartLocalSearchSolver;
import Utilities.ExperimentRunner;
import Utilities.*;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        Instance instance = new Instance("./raw_data/TSPB.csv", "TSPB");
        for (int i = 0;i<5;i++){

            System.out.println("\n\n================== Experiment Run " + (i+1) + " ==================");
                // Create experiment runner

                long startTime = System.currentTimeMillis();

                MultiStartLocalSearchExperimentRunner runner = new MultiStartLocalSearchExperimentRunner();

                // Run experiments (e.g., 100 iterations per method)
                System.out.println("Running experiments...");
                List<ExperimentResult> results = runner.runExperiments(instance, 5);

                // Export results to CSV
                System.out.println("Exporting results...");
                // ensure output directory exists
                java.io.File outDir = new java.io.File("src/Results/MultiStartLocalSearch/"+ instance.name+"/iteration"+(i+1));
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

        }
    }
}

