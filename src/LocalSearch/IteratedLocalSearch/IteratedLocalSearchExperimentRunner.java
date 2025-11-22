package LocalSearch.IteratedLocalSearch;

import LocalSearch.IntraRouteMoveType;
import LocalSearch.StartingSolutionType;
import Utilities.*;

import java.util.ArrayList;
import java.util.List;

public class IteratedLocalSearchExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {
    public IteratedLocalSearchSolver solver;

    public IteratedLocalSearchExperimentRunner(){
        this.solver = new IteratedLocalSearchSolver();
    }

    @Override
    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
        List<ExperimentResult> results = new ArrayList<>();


        return results;
    }

    @Override
    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {

        List<Solution> solutions = new ArrayList<>();



        for (int i=0;i<numIterations;i++){
            Solution solution = null;

            switch (methodName){
                case "method":
                    solution = solver.steepestLocalSearch(instance, StartingSolutionType.RANDOM, IntraRouteMoveType.NODE_EXCHANGE);
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