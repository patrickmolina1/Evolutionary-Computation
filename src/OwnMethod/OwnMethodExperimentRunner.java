package OwnMethod;

  import Utilities.*;
  import java.io.FileWriter;
  import java.io.IOException;
  import java.util.ArrayList;
  import java.util.List;

  public class OwnMethodExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {
      public OwnMethodSolver solver;
      private String baseOutputDir;
      private int timeLimitPerRun = 55000; // Increased to 100 seconds for better convergence

      public OwnMethodExperimentRunner() {
          this.solver = new OwnMethodSolver();
      }

      public void setBaseOutputDir(String baseOutputDir) {
          this.baseOutputDir = baseOutputDir;
      }

      public void setTimeLimitPerRun(int timeLimitMs) {
          this.timeLimitPerRun = timeLimitMs;
      }

      @Override
      public List<ExperimentResult> runExperiments(Instance instance, int numIterations) {
          List<ExperimentResult> results = new ArrayList<>();
          results.add(testMethod(instance, "OwnMethod", numIterations));
          return results;
      }

      @Override
      public ExperimentResult testMethod(Instance instance, String methodName, int numRuns) {
          List<Solution> solutions = new ArrayList<>();

          System.out.println("\n=== Starting " + numRuns + " runs with time limit " + timeLimitPerRun + "ms per run ===\n");

          for (int i = 0; i < numRuns; i++) {
              System.out.println("=== Run " + (i + 1) + "/" + numRuns + " ===");

              // Reset solver state between runs to ensure independence
              this.solver = new OwnMethodSolver();

              long runStart = System.currentTimeMillis();
              Solution solution = solver.solve(instance, timeLimitPerRun);
              long runEnd = System.currentTimeMillis();

              System.out.println("Run " + (i + 1) + " Result:");
              System.out.println("  Cost: " + solution.totalCost);
              System.out.println("  Distance: " + solution.totalDistance);
              System.out.println("  Nodes: " + solution.selectedNodes.size());
              System.out.println("  Actual Time: " + (runEnd - runStart) + "ms");
              System.out.println();

              solutions.add(solution);
          }

          // Print summary statistics
          ExperimentResult result = experimentStatsCalculations(instance, methodName, solutions);
          System.out.println("\n=== Summary Statistics ===");
          System.out.println("Min Cost: " + result.minCost);
          System.out.println("Max Cost: " + result.maxCost);
          System.out.println("Avg Cost: " + String.format("%.2f", result.avgCost));
          System.out.println("Best Solution Nodes: " + result.bestSolutionId);

          return result;
      }

      public List<ExperimentResult> runExperimentsWithSeparateRuns(Instance instance, int numRuns) {
          List<ExperimentResult> results = new ArrayList<>();
          List<Solution> allSolutions = new ArrayList<>();

          System.out.println("\n=== Starting Separate Run Experiments ===");
          System.out.println("Instance: " + instance.name);
          System.out.println("Number of runs: " + numRuns);
          System.out.println("Time limit per run: " + timeLimitPerRun + "ms\n");

          for (int run = 0; run < numRuns; run++) {
              System.out.println("=== Run " + (run + 1) + "/" + numRuns + " ===");

              // Reset solver for independent runs
              this.solver = new OwnMethodSolver();

              long runStartTime = System.currentTimeMillis();
              Solution solution = solver.solve(instance, timeLimitPerRun);
              long runEndTime = System.currentTimeMillis();

              List<Solution> singleRunSolution = new ArrayList<>();
              singleRunSolution.add(solution);

              ExperimentResult result = experimentStatsCalculations(instance, "OwnMethod_Run_" + (run + 1), singleRunSolution);
              result.runTotalTime = runEndTime - runStartTime;

              System.out.println("Run " + (run + 1) + " completed:");
              System.out.println("  Cost: " + solution.totalCost);
              System.out.println("  Time: " + result.runTotalTime + "ms");
              System.out.println();

              results.add(result);
              allSolutions.add(solution);

              // Save individual run results
              if (baseOutputDir != null) {
                  try {
                      String runDir = baseOutputDir + "/run_" + (run + 1);
                      java.io.File dir = new java.io.File(runDir);
                      if (!dir.exists()) {
                          dir.mkdirs();
                      }

                      List<ExperimentResult> singleRunResult = new ArrayList<>();
                      singleRunResult.add(result);
                      exportResults(singleRunResult, runDir);
                  } catch (IOException e) {
                      System.err.println("Error saving results for run " + (run + 1) + ": " + e.getMessage());
                  }
              }
          }

          // Create overall summary
          if (baseOutputDir != null) {
              try {
                  createOverallSummary(instance, results, allSolutions, baseOutputDir);
                  System.out.println("Overall summary saved to: " + baseOutputDir + "/overall_summary.csv");
              } catch (IOException e) {
                  System.err.println("Error creating overall summary: " + e.getMessage());
              }
          }

          return results;
      }

      private void createOverallSummary(Instance instance, List<ExperimentResult> results,
                                       List<Solution> allSolutions, String outputDir) throws IOException {
          ExperimentResult overallResult = experimentStatsCalculations(instance, "OwnMethod_Overall", allSolutions);

          double avgRunTime = results.stream().mapToLong(r -> r.runTotalTime).average().orElse(0.0);
          long minRunTime = results.stream().mapToLong(r -> r.runTotalTime).min().orElse(0);
          long maxRunTime = results.stream().mapToLong(r -> r.runTotalTime).max().orElse(0);

          // Summary statistics file
          String summaryFile = outputDir + "/overall_summary.csv";
          try (FileWriter writer = new FileWriter(summaryFile)) {
              writer.append("Run,MinCost,MaxCost,AvgCost,MinTime,MaxTime,AvgTime,NumNodes,TotalRunTime\n");

              for (int i = 0; i < results.size(); i++) {
                  ExperimentResult result = results.get(i);
                  writer.append(String.format("%d,%d,%d,%.2f,%d,%d,%.2f,%d,%d\n",
                          i + 1,
                          result.minCost,
                          result.maxCost,
                          result.avgCost,
                          result.minRunningTime,
                          result.maxRunningTime,
                          result.avgRunningTime,
                          allSolutions.get(i).selectedNodes.size(),
                          result.runTotalTime));
              }

              writer.append(String.format("\nOVERALL,%d,%d,%.2f,%d,%d,%.2f,%d,%.2f\n",
                      overallResult.minCost,
                      overallResult.maxCost,
                      overallResult.avgCost,
                      overallResult.minRunningTime,
                      overallResult.maxRunningTime,
                      overallResult.avgRunningTime,
                      overallResult.numSolutions,
                      avgRunTime));

              // Add standard deviation
              double stdDev = calculateStdDev(allSolutions);
              writer.append(String.format("\nSTD_DEV,%.2f\n", stdDev));
          }

          // Detailed solutions file
          String allSolutionsFile = outputDir + "/all_solutions.csv";
          try (FileWriter writer = new FileWriter(allSolutionsFile)) {
              writer.append("Run,TotalCost,NumNodes,TotalDistance,TotalRunningTime,Cycle\n");

              for (int run = 0; run < allSolutions.size(); run++) {
                  Solution sol = allSolutions.get(run);
                  String cycleStr = sol.cycle.toString()
                          .replace("[", "")
                          .replace("]", "")
                          .replace(", ", "-");

                  writer.append(String.format("%d,%d,%d,%d,%d,\"%s\"\n",
                          run + 1,
                          sol.totalCost,
                          sol.selectedNodes.size(),
                          sol.totalDistance,
                          sol.totalRunningTime,
                          cycleStr));
              }
          }

          // Best solution details
          Solution bestSolution = allSolutions.stream()
                  .min((s1, s2) -> Integer.compare(s1.totalCost, s2.totalCost))
                  .orElse(null);

          if (bestSolution != null) {
              String bestSolutionFile = outputDir + "/best_solution.csv";
              try (FileWriter writer = new FileWriter(bestSolutionFile)) {
                  writer.append("NodeID,X,Y,Cost\n");
                  for (Node node : bestSolution.selectedNodes) {
                      writer.append(String.format("%d,%d,%d,%d\n",
                              node.id, node.x, node.y, node.cost));
                  }
              }
          }
      }

      private double calculateStdDev(List<Solution> solutions) {
          double mean = solutions.stream().mapToInt(s -> s.totalCost).average().orElse(0.0);
          double variance = solutions.stream()
                  .mapToDouble(s -> Math.pow(s.totalCost - mean, 2))
                  .average()
                  .orElse(0.0);
          return Math.sqrt(variance);
      }
  }