package GreedyRegretHeuristics;

import Utilities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GreedyRegretHeuristicsExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {

    public GreedyRegretHeuristicsSolver solver;

    public GreedyRegretHeuristicsExperimentRunner() {
        this.solver = new GreedyRegretHeuristicsSolver();

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
