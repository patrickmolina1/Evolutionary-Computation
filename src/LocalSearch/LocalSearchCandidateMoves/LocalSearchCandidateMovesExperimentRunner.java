package LocalSearch.LocalSearchCandidateMoves;
import Utilities.*;
import java.util.List;

public class LocalSearchCandidateMovesExperimentRunner extends ExperimentRunner implements ExperimentRunnerInterface {
    public LocalSearchCandidateMovesSolver solver;

    public LocalSearchCandidateMovesExperimentRunner(){
        this.solver = new LocalSearchCandidateMovesSolver();
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