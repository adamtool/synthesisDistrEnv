package uniolunisaar.adam.tests.synthesis.symbolic.bddapproach.distrenv;

import java.io.File;
import org.testng.annotations.Test;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolver;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverFactory;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingSomeExamples {

    private static final String inputDir = System.getProperty("examplesfolder") + "/forallsafety/";
    private static final String outputDir = System.getProperty("testoutputfolder") + "/safety/";

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public static void someTest() throws Exception {
        final String path = inputDir + "2env" + File.separator;
        final String name = "paul";
        Logger.getInstance().addMessage("Testing file: " + inputDir + name, false);
        DistrEnvBDDSolverOptions opts = new DistrEnvBDDSolverOptions(false, false);
        DistrEnvBDDSolver<? extends Condition> solv = DistrEnvBDDSolverFactory.getInstance().getSolver(path + name + ".apt", opts);
        solv.existsWinningStrategy();
    }
}
