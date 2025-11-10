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


        return experimentStatsCalculations(instance, methodName, solutions);
    }
}