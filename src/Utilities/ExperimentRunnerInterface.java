package Utilities;

import java.util.List;

public interface ExperimentRunnerInterface {
    List<ExperimentResult> runExperiments(Instance instance, int numIterations);
    ExperimentResult testMethod(Instance instance, String methodName, int numIterations);


}
