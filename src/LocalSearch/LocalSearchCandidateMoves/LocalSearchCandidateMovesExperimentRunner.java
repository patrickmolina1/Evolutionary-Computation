package LocalSearch.LocalSearchCandidateMoves;

import LocalSearch.IntraRouteMoveType;
import LocalSearch.StartingSolutionType;
import Utilities.*;

import java.util.ArrayList;
import java.util.List;

public class LocalSearchCandidateMovesExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {
    public LocalSearchCandidateMovesSolver solver;

    public LocalSearchCandidateMovesExperimentRunner(){
        this.solver = new LocalSearchCandidateMovesSolver();
    }

    @Override
    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
        List<ExperimentResult> results = new ArrayList<>();

        // Test with NODE_EXCHANGE
        ExperimentResult nodeExchangeResult = testMethod(instance, "SteepestLS_RandomStart_NodeExchangeCandidate", numIterations);
        results.add(nodeExchangeResult);

        // Test with EDGE_EXCHANGE
        ExperimentResult edgeExchangeResult = testMethod(instance, "SteepestLS_RandomStart_EdgeExchangeCandidate", numIterations);
        results.add(edgeExchangeResult);

        return results;
    }

    @Override
    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {

        List<Solution> solutions = new ArrayList<>();



        for (int i=0;i<numIterations;i++){
            Solution solution = null;

            switch (methodName){
                case "SteepestLS_RandomStart_NodeExchangeCandidate":
                    solution = solver.steepestLocalSearch(instance, StartingSolutionType.RANDOM, IntraRouteMoveType.NODE_EXCHANGE);
                    break;
                case "SteepestLS_RandomStart_EdgeExchangeCandidate":
                    solution = solver.steepestLocalSearch(instance,StartingSolutionType.RANDOM, IntraRouteMoveType.EDGE_EXCHANGE);
                    break;
            }

            if (solution != null){
                System.out.println("Iteration " + (i+1) + ": Cost = " + solution.totalCost + ", Running Time = " + solution.totalRunningTime + "ms");
                solutions.add(solution);
            }

        }


        // Calculate statistics and find best solution
        int minCost = Integer.MAX_VALUE;
        int maxCost = 0;
        int bestSolutionId = 0;
        int minRunningTime = Integer.MAX_VALUE;
        int maxRunningTime = 0;

        for (int i = 0; i < solutions.size(); i++) {
            int cost = solutions.get(i).totalCost;
            if (cost < minCost) {
                minCost = cost;
                bestSolutionId = i + 1; // 1-based index
            }
            if (cost > maxCost) {
                maxCost = cost;
            }
            int runningTime = solutions.get(i).totalRunningTime;
            if (runningTime < minRunningTime) {
                minRunningTime = runningTime;
            }
            if (runningTime > maxRunningTime) {
                maxRunningTime = runningTime;
            }
        }

        double avgCost = solutions.stream().mapToInt(s -> s.totalCost).average().orElse(0.0);
        double avgRunningTime = solutions.stream().mapToInt(s -> s.totalRunningTime).average().orElse(0.0);

        return new ExperimentResult(
                instance.name,
                methodName,
                minCost,
                maxCost,
                avgCost,
                minRunningTime,
                maxRunningTime,
                avgRunningTime,
                solutions.size(),
                bestSolutionId,
                solutions
        );
    }
}