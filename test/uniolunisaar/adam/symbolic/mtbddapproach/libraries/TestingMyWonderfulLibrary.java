package uniolunisaar.adam.symbolic.mtbddapproach.libraries;

import java.io.File;
import org.testng.Assert;
import org.testng.annotations.Test;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.logic.pg.solver.distrenv.MTBDDSolver;
import uniolunisaar.adam.logic.pg.solver.distrenv.MTBDDSolverFactory;
import uniolunisaar.adam.logic.pg.solver.distrenv.MTBDDSolverOptions;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingMyWonderfulLibrary {

    private static final String inputDir = System.getProperty("examplesfolder") + "/safety/";
    private static final String outputDir = System.getProperty("testoutputfolder") + "/safety/";

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public static void testCall() throws Exception {
        final String path = inputDir + "firstExamplePaper" + File.separator;
        final String name = "firstExamplePaper";
        MTBDDSolver<? extends Condition<?>> solv = MTBDDSolverFactory.getInstance().getSolver(path + name + ".apt", new MTBDDSolverOptions());
        solv.existsWinningStrategy();
    }

    @Test
    public static void testCall2() throws Exception {
        final String path = inputDir + "firstExamplePaper" + File.separator;
        final String name = "firstExamplePaper";
        String output = "asdf";
        try {
            MTBDDSolver<? extends Condition<?>> solv = MTBDDSolverFactory.getInstance().getSolver(path + name + ".apt", new MTBDDSolverOptions());
            solv.getStrategy();
        } catch (UnsupportedOperationException e) {
            output = e.getMessage();
        }
        Assert.assertEquals(output, "Not supported yet.");
    }
}
