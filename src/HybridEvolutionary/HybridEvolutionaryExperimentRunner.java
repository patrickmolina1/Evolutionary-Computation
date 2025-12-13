package HybridEvolutionary;

import Utilities.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HybridEvolutionaryExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {

    public HybridEvolutionarySolver solver;

    public HybridEvolutionaryExperimentRunner() {
        this.solver = new HybridEvolutionarySolver();
    }

    public List<ExperimentResult> runExperiments(Instance instance, int numIterations, long timeLimitMs) {
        List<ExperimentResult> results = new ArrayList<>();

        results.add(testMethod(instance, "HEA_Operator1_WithLS", numIterations, timeLimitMs, RecombinationOperator.OPERATOR_1, true));
        results.add(testMethod(instance, "HEA_Operator2_WithLS", numIterations, timeLimitMs, RecombinationOperator.OPERATOR_2, true));
        results.add(testMethod(instance, "HEA_Operator2_WithoutLS", numIterations, timeLimitMs, RecombinationOperator.OPERATOR_2, false));

        return results;
    }

    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations, long timeLimitMs,
                                       RecombinationOperator operator, boolean useLS) {
        List<Solution> solutions = new ArrayList<>();

        for (int i = 0; i < numIterations; i++) {
            Solution solution = solver.hybridEvolutionary(instance, timeLimitMs, operator, useLS);
            if (solution != null) {
                solutions.add(solution);
            }
        }

        return experimentStatsCalculations(instance, methodName, solutions);
    }

    @Override
    public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
        return runExperiments(instance, numIterations, 5000);
    }

    @Override
    public ExperimentResult testMethod(Instance instance, String methodName, int numIterations) {
        return null;
    }
}
