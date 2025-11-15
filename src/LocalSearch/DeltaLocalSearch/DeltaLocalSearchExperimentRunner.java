package LocalSearch.DeltaLocalSearch;

import LocalSearch.IntraRouteMoveType;
import LocalSearch.StartingSolutionType;
import Utilities.*;

import java.util.ArrayList;
import java.util.List;

public class DeltaLocalSearchExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {

    public DeltaLocalSearchSolver solver;

    public DeltaLocalSearchExperimentRunner() {
        this.solver = new DeltaLocalSearchSolver();
    }

    @Override
    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
        List<ExperimentResult> results = new ArrayList<>();

        results.add(testMethod(instance, "DeltaLS_RandomStart_NodeExchange", numIterations));
        results.add(testMethod(instance, "DeltaLS_RandomStart_EdgeExchange", numIterations));

        return results;
    }

    @Override
    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {
        List<Solution> solutions = new ArrayList<>();

        for (int i = 0; i < numIterations; i++) {
            Solution solution = null;

            switch (methodName) {
                case "DeltaLS_RandomStart_NodeExchange":
                    solution = solver.deltaLocalSearch(instance, StartingSolutionType.RANDOM, IntraRouteMoveType.NODE_EXCHANGE);
                    break;
                case "DeltaLS_RandomStart_EdgeExchange":
                    solution = solver.deltaLocalSearch(instance, StartingSolutionType.RANDOM, IntraRouteMoveType.EDGE_EXCHANGE);
                    break;
            }

            if (solution != null) {
                System.out.println("Iteration " + (i+1) + ": Cost = " + solution.totalCost + ", Running Time = " + solution.totalRunningTime + "ms");
                solutions.add(solution);
            }
        }

        return experimentStatsCalculations(instance, methodName, solutions);
    }
}

