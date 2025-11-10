package LocalSearch;

import GreedyHeuristics.GreedyHeuristicsSolver;
import Utilities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LocalSearchExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {

    public LocalSearchSolver solver;

    public LocalSearchExperimentRunner() {
        this.solver = new LocalSearchSolver();

    }

    @Override
    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
        List<ExperimentResult> results = new ArrayList<>();

        results.add(testMethod(instance, "GreedyLS_RandomStart_NodeExchange", numIterations));
        results.add(testMethod(instance, "GreedyLS_RandomStart_EdgeExchange", numIterations));
        results.add(testMethod(instance, "GreedyLS_GreedyStart_NodeExchange", numIterations));
        results.add(testMethod(instance, "GreedyLS_GreedyStart_EdgeExchange", numIterations));

        results.add(testMethod(instance, "SteepestLS_RandomStart_NodeExchange", numIterations));
        results.add(testMethod(instance, "SteepestLS_RandomStart_EdgeExchange", numIterations));
        results.add(testMethod(instance, "SteepestLS_GreedyStart_NodeExchange", numIterations));
        results.add(testMethod(instance, "SteepestLS_GreedyStart_EdgeExchange", numIterations));

        return results;
    }

    @Override
    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {
        List<Solution> solutions = new ArrayList<>();


        for (int i = 0; i < numIterations; i++) {
            Solution solution = null;
            int r = new Random().nextInt(instance.nodes.size());
            switch (methodName) {
                case "GreedyLS_RandomStart_NodeExchange":
                    solution = solver.greedyLocalSearch(instance, StartingSolutionType.RANDOM, IntraRouteMoveType.NODE_EXCHANGE);
                    break;
                case "GreedyLS_RandomStart_EdgeExchange":
                    solution = solver.greedyLocalSearch(instance, StartingSolutionType.RANDOM, IntraRouteMoveType.EDGE_EXCHANGE);
                    break;
                case "GreedyLS_GreedyStart_NodeExchange":
                    solution = solver.greedyLocalSearch(instance, StartingSolutionType.GREEDY, IntraRouteMoveType.NODE_EXCHANGE);
                    break;

                case "GreedyLS_GreedyStart_EdgeExchange":
                    solution = solver.greedyLocalSearch(instance, StartingSolutionType.GREEDY, IntraRouteMoveType.EDGE_EXCHANGE);
                    break;
                    //
                case "SteepestLS_RandomStart_NodeExchange":
                    solution = solver.steepestLocalSearch(instance, StartingSolutionType.RANDOM, IntraRouteMoveType.NODE_EXCHANGE);
                    break;
                case "SteepestLS_RandomStart_EdgeExchange":
                    solution = solver.steepestLocalSearch(instance, StartingSolutionType.RANDOM, IntraRouteMoveType.EDGE_EXCHANGE);
                    break;
                case "SteepestLS_GreedyStart_NodeExchange":
                    solution = solver.steepestLocalSearch(instance, StartingSolutionType.GREEDY, IntraRouteMoveType.NODE_EXCHANGE);
                    break;

                case "SteepestLS_GreedyStart_EdgeExchange":
                    solution = solver.steepestLocalSearch(instance, StartingSolutionType.GREEDY, IntraRouteMoveType.EDGE_EXCHANGE);
                    break;
            }


            if (solution != null) {
                solutions.add(solution);
            }
        }

        return experimentStatsCalculations(instance, methodName, solutions);
    }



}
