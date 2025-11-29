package LargeNeighborhoodSearch;

import LocalSearch.IntraRouteMoveType;
import LocalSearch.StartingSolutionType;
import Utilities.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LargeNeighborhoodSearchExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {

    public LargeNeighborhoodSearchSolver solver;

    public LargeNeighborhoodSearchExperimentRunner() {
        this.solver = new LargeNeighborhoodSearchSolver();

    }
    @Override
    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
        List<ExperimentResult> results = new ArrayList<>();

        results.add(testMethod(instance, "LNS_LS", numIterations));
        results.add(testMethod(instance, "LNS_NOLS", numIterations));

        return results;
    }

    @Override
    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {
        List<Solution> solutions = new ArrayList<>();


        for (int i = 0; i < numIterations; i++) {
            Solution solution = null;
            int r = new Random().nextInt(instance.nodes.size());
            switch (methodName) {
                case "LNS_LS":
                    solution = solver.runLNS_WithLS(instance, IntraRouteMoveType.EDGE_EXCHANGE);
                    break;
                case "LNS_NOLS":
                    solution = solver.runLNS_WithoutLS(instance);
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
