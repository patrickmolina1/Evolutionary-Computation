package GlobalConvexity;

import LocalSearch.IntraRouteMoveType;
import LocalSearch.LocalSearchSolver;
import LocalSearch.StartingSolutionType;
import Utilities.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GlobalConvexityExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {

    public LocalSearchSolver solver;
    public Solution bestSolutionEver;

    public GlobalConvexityExperimentRunner() {
        this.solver = new LocalSearchSolver();

    }

    @Override
    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
        List<ExperimentResult> results = new ArrayList<>();

        try {
            bestSolutionEver = Utils.getBestSolution("./raw_data/", instance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Test each method
        results.add(testMethod(instance, "greedyLocalSearch", numIterations));


        return results;
    }

    @Override
    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {
        List<Solution> solutions = new ArrayList<>();

        for (int i = 0; i < numIterations; i++) {
            Solution solution = null;

            if (methodName.equals("greedyLocalSearch")) {
                solution = solver.greedyLocalSearch(instance, StartingSolutionType.RANDOM, IntraRouteMoveType.EDGE_EXCHANGE);
            }

            if (solution != null) {
                solutions.add(solution);
            }
        }

        return experimentStatsCalculations(instance, methodName, solutions);
    }
    
    
    public double calculateSimilarity(Solution sol1, Solution sol2, SimilarityType similarityType){
        
        return switch (similarityType) {
            case EDGE_BASED -> SimilarityMetrics.edgeBasedSimilarity(sol1, sol2);
            case NODE_BASED -> SimilarityMetrics.cyclicNodeBasedSimilarity(sol1, sol2);
        };

    }

    @Override
    public void exportResults(List<ExperimentResult> results, String outputDir) throws IOException {
        // Export summary CSV
        exportSummaryCSV(results, outputDir + "/experiment_summary.csv");

        // Export detailed solutions for each method
        for (ExperimentResult result : results) {
            String filename = String.format("%s/%s_%s_solutions.csv",
                    outputDir,
                    result.instanceName,
                    result.methodName);
            exportSolutionsCSV(result, filename, result.bestSolutionId);
        }
    }

    private void exportSummaryCSV(List<ExperimentResult> results, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.append("Instance,Method,MinCost,MaxCost,AvgCost,MinTime,MaxTime,AvgTime,NumSolutions,BestSolutionID\n");

            // Write data
            for (ExperimentResult result : results) {
                writer.append(String.format("%s,%s,%d,%d,%.2f,%d,%d,%.2f,%d,%d\n",
                        result.instanceName,
                        result.methodName,
                        result.minCost,
                        result.maxCost,
                        result.avgCost,
                        result.minRunningTime,
                        result.maxRunningTime,
                        result.avgRunningTime,
                        result.numSolutions,
                        result.bestSolutionId));
            }
        }
    }

    private void exportSolutionsCSV(ExperimentResult result, String filename, int bestSolutionId) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.append("SolutionID,TotalCost,NumNodes,TotalDistance,ObjectiveFunction,TotalRunningTime,Cycle," +
                    "Avg1000Edge,Avg1000Node,BestOf1000Edge,BestOf1000Node,BestEverEdge,BestEverNode\n");

            // Write each solution
            for (int i = 0; i < result.solutions.size(); i++) {
                Solution sol = result.solutions.get(i);

                double BestOf1000Edge = calculateSimilarity(sol, result.solutions.get(bestSolutionId - 1), SimilarityType.EDGE_BASED);
                double BestOf1000Node = calculateSimilarity(sol, result.solutions.get(bestSolutionId - 1), SimilarityType.NODE_BASED);

                double Avg1000Edge = 0.0;
                double Avg1000Node = 0.0;

                for (int j = 0; j < result.solutions.size(); j++) {
                    if (i == j) continue;
                    Solution otherSol = result.solutions.get(j);
                    Avg1000Edge += calculateSimilarity(sol, otherSol, SimilarityType.EDGE_BASED);
                    Avg1000Node += calculateSimilarity(sol, otherSol, SimilarityType.NODE_BASED);
                }
                Avg1000Edge /= result.solutions.size()-1;
                Avg1000Node /= result.solutions.size()-1;

                double BestEverEdge = calculateSimilarity(sol, bestSolutionEver, SimilarityType.EDGE_BASED);
                double BestEverNode = calculateSimilarity(sol, bestSolutionEver, SimilarityType.NODE_BASED);


                int objectiveFunction = sol.totalCost + sol.totalDistance;

                // Convert cycle to string
                String cycleStr = sol.cycle.toString()
                        .replace("[", "")
                        .replace("]", "")
                        .replace(", ", "-");

                writer.append(String.format("%d,%d,%d,%d,%d,%d,\"%s\",%f,%f,%f,%f,%f,%f\n",
                        i + 1,
                        sol.totalCost,
                        sol.selectedNodes.size(),
                        sol.totalDistance,
                        objectiveFunction,
                        sol.totalRunningTime,
                        cycleStr,
                        Avg1000Edge,
                        Avg1000Node,
                        BestOf1000Edge,
                        BestOf1000Node,
                        BestEverEdge,
                        BestEverNode

                ));
            }
        }
    }

}
