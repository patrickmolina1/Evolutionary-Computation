
import Utilities.*;
import GreedyHeuristics.*;

public class Main {
    public static void main(String[] args) {
        Instance instance = new Instance("src/TSPA.csv");

        Solver solver = new Solver();
        Solution randomSolution = solver.randomSolution(instance);


    }
}
