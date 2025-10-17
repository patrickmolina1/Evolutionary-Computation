package Utilities;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public abstract class ExperimentRunner {



    public ExperimentRunner() {

    }

    public void exportResults(List<ExperimentResult> results, String outputDir) throws IOException {
        // Export summary CSV
        exportSummaryCSV(results, outputDir + "/experiment_summary.csv");

        // Export detailed solutions for each method
        for (ExperimentResult result : results) {
            String filename = String.format("%s/%s_%s_solutions.csv",
                    outputDir,
                    result.instanceName,
                    result.methodName);
            exportSolutionsCSV(result, filename);
        }
    }

    private void exportSummaryCSV(List<ExperimentResult> results, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.append("Instance,Method,MinCost,MaxCost,AvgCost,NumSolutions,BestSolutionID\n");

            // Write data
            for (ExperimentResult result : results) {
                writer.append(String.format("%s,%s,%d,%d,%.2f,%d,%d\n",
                        result.instanceName,
                        result.methodName,
                        result.minCost,
                        result.maxCost,
                        result.avgCost,
                        result.numSolutions,
                        result.bestSolutionId));
            }
        }
    }

    private void exportSolutionsCSV(ExperimentResult result, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.append("SolutionID,TotalCost,NumNodes,TotalDistance,ObjectiveFunction,Cycle\n");

            // Write each solution
            for (int i = 0; i < result.solutions.size(); i++) {
                Solution sol = result.solutions.get(i);

                int objectiveFunction = sol.totalCost + sol.totalDistance;

                // Convert cycle to string
                String cycleStr = sol.cycle.toString()
                        .replace("[", "")
                        .replace("]", "")
                        .replace(", ", "-");

                writer.append(String.format("%d,%d,%d,%d,%d,\"%s\"\n",
                        i + 1,
                        sol.totalCost,
                        sol.selectedNodes.size(),
                        sol.totalDistance,
                        objectiveFunction,
                        cycleStr));
            }
        }
    }
}
