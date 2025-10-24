package LocalSearch;

import GreedyHeuristics.GreedyHeuristicsSolver;
import Utilities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LocalSearchExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {

    public GreedyHeuristicsSolver solver;

    public LocalSearchExperimentRunner() {
        this.solver = new GreedyHeuristicsSolver();

    }

    @Override
    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {




        return null;
    }

    @Override
    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {
        return null;
    }



}
