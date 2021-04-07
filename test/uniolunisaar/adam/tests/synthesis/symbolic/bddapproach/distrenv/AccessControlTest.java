package uniolunisaar.adam.tests.synthesis.symbolic.bddapproach.distrenv;

import java.io.IOException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.analysis.isomorphism.IsomorphismLogic;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.exceptions.CalculationInterruptedException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.CouldNotCalculateException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolver;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverFactory;
import uniolunisaar.adam.util.PGTools;

@Test
public class AccessControlTest {
    @Test
    private void is_generator_correct_for_two_authenticators() throws NotSupportedGameException, CouldNotCalculateException, ParseException, IOException, UnboundedException, RenderException {
        PetriGameWithTransits generated = AccessControlGenerator.generate(2);
        PetriGameWithTransits handmade = PGTools.getPetriGame(System.getProperty("examplesfolder") + "/forallsafety/lukas-panneke/accessor.apt", false, false);
        IsomorphismLogic isomorphism = new IsomorphismLogic(generated.getReachabilityGraph().toReachabilityLTS(), handmade.getReachabilityGraph().toReachabilityLTS(), false);
        assertTrue(isomorphism.isIsomorphic());
    }

    private long test_speed_for_fixed_authenticators(int authenticators) throws CouldNotFindSuitableConditionException, SolvingException, CalculationInterruptedException {
        PetriGameWithTransits game = AccessControlGenerator.generate(authenticators);
        long start = System.currentTimeMillis();
        DistrEnvBDDSolver<? extends Condition<?>> solver = DistrEnvBDDSolverFactory.getInstance()
                .getSolver(game, new DistrEnvBDDSolverOptions(false, false));
        assertTrue(solver.existsWinningStrategy());
        long end = System.currentTimeMillis();
        return end - start;
    }

    @Test(timeOut = 180_000)  // 3 minutes
    private void test_speed_for_increasing_authenticators() throws CouldNotFindSuitableConditionException, CalculationInterruptedException, SolvingException {
        long duration_millis = 0;
        for (int i = 1; duration_millis < 60_000; i++) {
            duration_millis = test_speed_for_fixed_authenticators(i);
            System.out.println("Checking AccessControl with " + i + " accessors took " + duration_millis + " ms");
        }

    }


}
