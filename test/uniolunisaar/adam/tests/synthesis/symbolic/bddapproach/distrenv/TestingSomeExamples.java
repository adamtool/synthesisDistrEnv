package uniolunisaar.adam.tests.synthesis.symbolic.bddapproach.distrenv;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.graph.synthesis.twoplayergame.symbolic.bddapproach.BDDGraph;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;
import uniolunisaar.adam.exceptions.synthesis.pgwt.CouldNotCalculateException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.InvalidPartitionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;
import uniolunisaar.adam.logic.synthesis.builder.twoplayergame.symbolic.bddapproach.BDDGraphAndGStrategyBuilder;
import uniolunisaar.adam.logic.synthesis.pgwt.calculators.CalculatorIDs;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolver;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverFactory;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.safety.DistrEnvBDDGlobalSafetySolver;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.AdamExtensions;
import uniolunisaar.adam.util.PGTools;
import uniolunisaar.adam.util.symbolic.bddapproach.BDDTools;

/**
 * @author Lukas Panneke
 */
@Test
public class TestingSomeExamples {

    private static final boolean ENV = false;
    private static final boolean SYS = true;
    private static final boolean PROBABLY_ENV = ENV;
    private static final boolean PROBABLY_SYS = SYS;

    private static final List<Set<String>> AUTO_PARTITION = Collections.emptyList();
    private static final List<Set<String>> PARTITIONED_IN_FILE = null;
    private static final List<Set<String>> PARTITION_TODO = AUTO_PARTITION;
    private static final List<Set<String>> PARTITION_IMPOSSIBLE = null;

    @DataProvider
    private static Object[][] examples() {
        return new Object[][] {
//              { "existssafety/escape/escape11.apt", ENV, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/infflowchains/infflowchains_env_0.apt", ENV, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/decision1.apt", ENV, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/decision2.apt", SYS, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/oneTransitionEnv1.apt", ENV, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/oneTransitionEnv2.apt", SYS, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/oneTransitionEnv3.apt", ENV, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/oneTransitionSys1.apt", ENV, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/oneTransitionSys2.apt", SYS, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/oneTransitionSys3.apt", SYS, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/safeEnvChain.apt", SYS, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/twoDecisions1.apt", ENV, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/twoDecisions2.apt", SYS, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/unfair1.apt", SYS, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/unfair3.apt", SYS, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/unfair7.apt", ENV, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/unfair8.apt", SYS, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/unfair9.apt", ENV, PARTITION_TODO }, // wrong winning condition
//              { "existssafety/toyexamples/unfair10.apt", SYS, PARTITION_TODO }, // wrong winning condition
//              { "forallsafety/2env/paul.apt", SYS, PARTITIONED_IN_FILE }, // not concurrency preserving (fixed in chasing.apt)
//              { "forallsafety/boundedunfolding/causalmemory.apt", ENV, PARTITION_IMPOSSIBLE }, // possible partitions are {{ s0, s1, s2, s5 }, { s3, s4 }} and {{ s0, s5 }, { s1, s2 }, { s3, s4 }}. both are not valid.
//              { "forallsafety/boundedunfolding/finiteWithBad.apt", SYS, AUTO_PARTITION }, // not concurrency preserving
//              { "forallsafety/boundedunfolding/firstTry.apt", SYS, AUTO_PARTITION }, // not concurrency preserving
//              { "forallsafety/boundedunfolding/secondTry.apt", SYS, AUTO_PARTITION }, // not concurrency preserving
//              { "forallsafety/boundedunfolding/thirdTry.apt", SYS, AUTO_PARTITION }, // not concurrency preserving
//              { "forallsafety/boundedunfolding/txt.apt", UNKNOWN, AUTO_PARTITION }, // no system token in reachable marking
//              { "forallsafety/boundedunfolding/txt2.apt", UNKNOWN, AUTO_PARTITION }, // no system token in reachable marking
                { "forallsafety/constructedExample/constructedExample.apt", SYS, AUTO_PARTITION },
                { "forallsafety/constructedExampleWithoutLoop/constructedExampleWithoutLoop.apt", SYS, AUTO_PARTITION },
//              { "forallsafety/cornercases/unreachableEnvTransition.apt", UNKNOWN, AUTO_PARTITION }, // no system token in reachable marking
                { "forallsafety/cornercases/unreachableEnvTransition2.apt", SYS, List.of(Set.of("S1", "S2"), Set.of("E0", "E2", "E3"), Set.of("E1", "E4")) },
                { "forallsafety/deadlock/nondetDeadlock.apt", SYS, List.of(Set.of("s0", "s1", "s2"), Set.of("E", "r1", "EL", "r2")) },
//              { "forallsafety/deadlock/nondetDeadlock0.apt", SYS, PARTITION_TODO }, // not concurrency preserving
                { "forallsafety/firstExamplePaper/firstExamplePaper.apt", SYS, AUTO_PARTITION },
                { "forallsafety/firstExamplePaper/firstExamplePaper_extended.apt", ENV, AUTO_PARTITION },
//              { "forallsafety/jhh/myexample0.apt", ENV, PARTITION_TODO }, // no system
//              { "forallsafety/jhh/myexample00.apt", ENV, PARTITION_TODO }, // no system
//              { "forallsafety/jhh/myexample000.apt", ENV, AUTO_PARTITION }, // no system token in reachable marking
//              { "forallsafety/jhh/myexample2.apt", UNKNOWN, AUTO_PARTITION }, // no system token in reachable marking
//              { "forallsafety/jhh/myexample3.apt", SYS, AUTO_PARTITION }, // no system token in reachable marking
                { "forallsafety/jhh/myexample4.apt", ENV, AUTO_PARTITION },
                { "forallsafety/jhh/myexample7.apt", PROBABLY_SYS, AUTO_PARTITION }, // to big to understand
//              { "forallsafety/jhh/robots_false.apt", UNKNOWN, AUTO_PARTITION }, // not concurrency preserving
//              { "forallsafety/jhh/robots_true.apt", UNKNOWN, AUTO_PARTITION }, // not concurrency preserving
                { "forallsafety/ma_vsp/vsp_1.apt", SYS, AUTO_PARTITION }, // no bad places -> cannot loose
                { "forallsafety/ma_vsp/vsp_1_withBadPlaces.apt", PROBABLY_SYS, AUTO_PARTITION },
//              { "forallsafety/nm/independentNets.apt", SYS, AUTO_PARTITION }, // no system token in reachable marking
//              { "forallsafety/nm/minimal.apt", ENV, AUTO_PARTITION }, // no system token in reachable marking
//              { "forallsafety/nm/minimalNotFinishingEnv.apt", ENV, AUTO_PARTITION }, // no system token in reachable marking
//              { "forallsafety/nm/minimalOnlySys.apt", ENV, PARTITION_TODO }, // no environment
//              { "forallsafety/nm/trueconcurrent.apt", ENV, PARTITION_TODO }, // no system
                { "forallsafety/noStrategy/lateSameDecision.apt", ENV, AUTO_PARTITION },
                { "forallsafety/testingNets/envSkipsSys.apt", ENV, AUTO_PARTITION },
                { "forallsafety/testingNets/infiniteSystemTrysToAvoidEnvUseBadPlace.apt", ENV, AUTO_PARTITION },
//              { "forallsafety/tests/testNotStartingMcut.apt", UNKNOWN, AUTO_PARTITION }, // not concurrency preserving
                { "forallsafety/type2/separateEnvSys.apt", SYS, AUTO_PARTITION },
                { "~/work/nets/accessor.apt", SYS, PARTITIONED_IN_FILE },
                { "~/work/nets/chasing.apt", SYS, PARTITIONED_IN_FILE },
                { "~/work/nets/decision.apt", PROBABLY_SYS, PARTITIONED_IN_FILE },
                { "~/work/nets/different_choice.apt", ENV, PARTITIONED_IN_FILE }
        };
    }

    @DataProvider
    private static Object[][] specific() {
        return new Object[][] {
                { "~/work/nets/decision.apt", ENV, PARTITIONED_IN_FILE }
        };
    }

    static {
        Logger.getInstance().setVerbose(true);
    }

    private static final String inputDir = "/home/lukas/work/adam/github/synthesisDistrEnv/dependencies/examples/synthesis/";
    private static final String outputDir = "/home/lukas/tmp/work/";

    @Test(dataProvider = "examples")
    public static void existsWinningStrategyTest(String fileName, boolean existsWinningStrategy, List<Set<String>> partition) throws Exception {
        assertEquals(solver(fileName, partition).existsWinningStrategy(), existsWinningStrategy);
    }

    public static void renderPartitions(String fileName, List<Set<String>> partition) throws Exception {
        PGTools.savePG2DotAndPDF(outputDir + fileName, solver(fileName, partition).getGame(), false, true).join();
    }

    @Test(dataProvider = "examples")
    public static void checkPartition(String fileName, boolean existsWinningStrategy, List<Set<String>> partition) throws Exception {
        PetriGameWithTransits game = game(fileName, partition);
        try {
            DistrEnvBDDSolvingObject.partitionPlaces(game, false);
            System.out.println(game.getPlaces().stream()
                    .collect(Collectors.groupingBy(game::getPartition)));
        } catch (InvalidPartitionException | NoSuitableDistributionFoundException e) {
            System.out.println(game.getPlaces().stream()
                    .collect(Collectors.groupingBy(place -> game.hasPartition(place) ? game.getPartition(place) : -1)));
            e.printStackTrace();
            fail();
        }
    }

    @Test(dataProvider = "examples")
    public static void shouldCreateSolverTest(String fileName, boolean unused, List<Set<String>> partition) throws NotSupportedGameException, ParseException, CouldNotCalculateException, IOException {
        isSingleEnv(fileName, unused, partition);
        isConcurrencyPreserving(fileName, unused, partition);
    }

    @Test(dataProvider = "examples")
    public static void isSingleEnv(String fileName, boolean unused, List<Set<String>> partition) throws NotSupportedGameException, ParseException, IOException, CouldNotCalculateException {
        PGTools.checkExactlyOneSysToken(game(fileName, partition));
    }

    @Test(dataProvider = "examples")
    public static void isConcurrencyPreserving(String fileName, boolean unused, List<Set<String>> partition) throws NotSupportedGameException, CouldNotCalculateException, ParseException, IOException {
        Object value = game(fileName, partition).getValue(CalculatorIDs.CONCURRENCY_PRESERVING.name());
        assertEquals(value.getClass(), Boolean.class);
        assertTrue(((Boolean) value), fileName + " is not concurrency preserving.");
    }

    @Test(dataProvider = "examples")
    public static void canCreateSolverTest(String fileName, boolean unused, List<Set<String>> partition) throws IOException, ParseException {
        try {
            solver(fileName, partition);
        } catch (SolvingException e) {
            fail("Could not create solver", e);
        } catch (CouldNotFindSuitableConditionException e) {
            fail("Unsupported winning condition", e);
        }
    }

    @Test(dataProvider = "specific")
    public static void renderGraph(String fileName, boolean existsWinningStrategy, List<Set<String>> partition) throws Exception {
        PetriGameWithTransits game = game(fileName, partition);
        DistrEnvBDDGlobalSafetySolver solver = (DistrEnvBDDGlobalSafetySolver) DistrEnvBDDSolverFactory.getInstance()
                .getSolver(game, new DistrEnvBDDSolverOptions(false, false));
        solver.initialize();
        BDDGraph bddGraph = BDDGraphAndGStrategyBuilder.getInstance().builtGraph(solver);
        BDDTools.saveGraph2PDF(outputDir + justFileName(fileName), bddGraph, solver);
        Thread.sleep(1000);
    }

    @Test(dataProvider = "specific")
    public static void renderGraphStrategy(String fileName, boolean existsWinningStrategy, List<Set<String>> partition) throws Exception {
        PetriGameWithTransits game = game(fileName, partition);
        DistrEnvBDDGlobalSafetySolver solver = (DistrEnvBDDGlobalSafetySolver) DistrEnvBDDSolverFactory.getInstance()
                .getSolver(game, new DistrEnvBDDSolverOptions(false, false));
        solver.initialize();
        BDDGraph bddGraph = BDDGraphAndGStrategyBuilder.getInstance().builtGraphStrategy(solver, null);
        BDDTools.saveGraph2PDF(outputDir + justFileName(fileName), bddGraph, solver);
        Thread.sleep(1000);
    }

    private static DistrEnvBDDSolver<? extends Condition<?>> solver(String fileName, List<Set<String>> partition) throws SolvingException, CouldNotFindSuitableConditionException, IOException, ParseException {
        return DistrEnvBDDSolverFactory.getInstance()
                .getSolver(game(fileName, partition), new DistrEnvBDDSolverOptions(false, false));
    }

    private static PetriGameWithTransits game(String fileName, List<Set<String>> partition) throws NotSupportedGameException, CouldNotCalculateException, ParseException, IOException {
        PetriGameWithTransits game = PGTools.getPetriGame(path(fileName), false, false);
        if (partition != PARTITIONED_IN_FILE) {
            for (Place place : game.getPlaces()) {
                place.removeExtension(AdamExtensions.token.name());
            }
            IntStream.range(0, partition.size())
                    .forEach(i -> partition.get(i).stream()
                            .map(game::getPlace)
                            .forEach(place -> game.setPartition(place, i)));
        }
        return game;
    }

    private static String path(String relativePath) {
        if (relativePath.startsWith("/")) {
            return relativePath;
        } else if (relativePath.startsWith("~/")) {
            return relativePath.replaceFirst("~", System.getProperty("user.home"));
        } else {
            return inputDir + relativePath;
        }
    }

    private static String justFileName(String relativePath) {
        String nameWithExtension = relativePath;
        int i = relativePath.lastIndexOf("/");
        if (i != -1) {
            nameWithExtension = relativePath.substring(i);
        }
        int j = nameWithExtension.indexOf(".");
        if (j != -1) {
            return nameWithExtension.substring(0, j);
        } else return nameWithExtension;
    }
}
