package Utilities;

import java.util.List;

public class ExperimentResult {
    public String instanceName;
    public String methodName;
    public int minCost;
    public int maxCost;
    public double avgCost;
    public int minRunningTime;
    public int maxRunningTime;
    public double avgRunningTime;
    public int numSolutions;
    public int bestSolutionId;
    public List<Solution> solutions;

    public ExperimentResult(String instanceName, String methodName,
                            int minCost, int maxCost, double avgCost, int minRunningTime, int maxRunningTime, double avgRunningTime, int numSolutions,
                            int bestSolutionId, List<Solution> solutions) {
        this.instanceName = instanceName;
        this.methodName = methodName;
        this.minCost = minCost;
        this.maxCost = maxCost;
        this.avgCost = avgCost;
        this.numSolutions = numSolutions;
        this.bestSolutionId = bestSolutionId;
        this.solutions = solutions;
        this.minRunningTime = minRunningTime;
        this.maxRunningTime = maxRunningTime;
        this.avgRunningTime = avgRunningTime;
    }
}
