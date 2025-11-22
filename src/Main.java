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

        long startTime = System.currentTimeMillis();

        MultiStartLocalSearchExperimentRunner runner = new MultiStartLocalSearchExperimentRunner();

        // ensure output directory exists
        java.io.File outDir = new java.io.File("src/Results/MultiStartLocalSearch/"+ instance.name);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        // Set the base output directory so the runner can save results
        runner.setBaseOutputDir(outDir.getPath());

        // Run experiments (e.g., 100 iterations per method)
        System.out.println("Running experiments...");
        List<ExperimentResult> results = runner.runExperiments(instance, 200,20);

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
