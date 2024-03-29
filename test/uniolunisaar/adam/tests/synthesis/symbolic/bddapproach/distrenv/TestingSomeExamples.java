package uniolunisaar.adam.tests.synthesis.symbolic.bddapproach.distrenv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.analysis.coverability.CoverabilityGraph;
import uniol.apt.analysis.coverability.CoverabilityGraphEdge;
import uniol.apt.analysis.coverability.CoverabilityGraphNode;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.graph.synthesis.twoplayergame.symbolic.bddapproach.BDDGraph;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;
import uniolunisaar.adam.exceptions.synthesis.pgwt.CouldNotCalculateException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.InvalidPartitionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NoStrategyExistentException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;
import uniolunisaar.adam.logic.synthesis.builder.symbolic.bddapproach.distrenv.DistrEnvBDDGlobalSafetyPetriGameStrategyBuilder;
import uniolunisaar.adam.logic.synthesis.builder.twoplayergame.symbolic.bddapproach.BDDGraphAndGStrategyBuilder;
import uniolunisaar.adam.logic.synthesis.pgwt.calculators.CalculatorIDs;
import uniolunisaar.adam.logic.synthesis.pgwt.calculators.ConcurrencyPreservingGamesCalculator;
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

    private final static Object[] paul = { "forallsafety/2env/paul.apt", SYS, PARTITIONED_IN_FILE };
    private final static Object[] causalmemory = { "forallsafety/boundedunfolding/causalmemory.apt", ENV, AUTO_PARTITION };
    private final static Object[] finiteWithBad = { "forallsafety/boundedunfolding/finiteWithBad.apt", SYS, AUTO_PARTITION };
    private final static Object[] firstTry = { "forallsafety/boundedunfolding/firstTry.apt", SYS, AUTO_PARTITION };
    private final static Object[] secondTry = { "forallsafety/boundedunfolding/secondTry.apt", SYS, AUTO_PARTITION };
    private final static Object[] thirdTry = { "forallsafety/boundedunfolding/thirdTry.apt", SYS, AUTO_PARTITION };
    private final static Object[] unreachableEnvTransition = { "forallsafety/cornercases/unreachableEnvTransition.apt", SYS, List.of(Set.of("S"), Set.of("E1", "E3"), Set.of("E2", "E4")) }; // no bad marking => system wins, automatic partitioning does not work for this one (2021-03-06)
    private final static Object[] unreachableEnvTransition2 = { "forallsafety/cornercases/unreachableEnvTransition2.apt", SYS, AUTO_PARTITION };
    private final static Object[] nondetDeadlock = { "forallsafety/deadlock/nondetDeadlock.apt", SYS, AUTO_PARTITION };
    private final static Object[] nondetDeadlock0 = { "forallsafety/deadlock/nondetDeadlock0.apt", SYS, AUTO_PARTITION };
    private final static Object[] deadlockAvoidance1 = { "forallsafety/deadlockAvoidance/deadlockAvoidance1.apt", true, AUTO_PARTITION };
    private final static Object[] deadlockAvoidance2 = { "forallsafety/deadlockAvoidance/deadlockAvoidance2.apt", true, AUTO_PARTITION };
    private final static Object[] firstExamplePaper = { "forallsafety/firstExamplePaper/firstExamplePaper.apt", SYS, AUTO_PARTITION };
    private final static Object[] firstExamplePaper_extended = { "forallsafety/firstExamplePaper/firstExamplePaper_extended.apt", ENV, AUTO_PARTITION };
    private final static Object[] myexample000 = { "forallsafety/jhh/myexample000.apt", ENV, AUTO_PARTITION }; // cp and no system token in reachable marking
    private final static Object[] myexample2 = { "forallsafety/jhh/myexample2.apt", SYS, AUTO_PARTITION }; // no bad marking => system wins
    private final static Object[] myexample3 = { "forallsafety/jhh/myexample3.apt", SYS, AUTO_PARTITION }; // no system token in reachable marking
    private final static Object[] myexample4 = { "forallsafety/jhh/myexample4.apt", ENV, AUTO_PARTITION };
    private final static Object[] myexample7 = { "forallsafety/jhh/myexample7.apt", PROBABLY_SYS, AUTO_PARTITION }; // to big to understand
    private final static Object[] robots_false = { "forallsafety/jhh/robots_false.apt", PROBABLY_ENV, AUTO_PARTITION }; // to big to understand
    private final static Object[] robots_true = { "forallsafety/jhh/robots_true.apt", PROBABLY_SYS, AUTO_PARTITION }; // to big to understand
    private final static Object[] vsp_1 = { "forallsafety/ma_vsp/vsp_1.apt", SYS, AUTO_PARTITION }; // no bad places -> cannot loose
    private final static Object[] vsp_1_withBadPlaces = { "forallsafety/ma_vsp/vsp_1_withBadPlaces.apt", PROBABLY_SYS, AUTO_PARTITION };
    private final static Object[] independentNets = { "forallsafety/nm/independentNets.apt", SYS, AUTO_PARTITION }; // no system token in reachable marking
    private final static Object[] minimal = { "forallsafety/nm/minimal.apt", ENV, AUTO_PARTITION }; // no system token in reachable marking
    private final static Object[] minimalNotFinishingEnv = { "forallsafety/nm/minimalNotFinishingEnv.apt", ENV, AUTO_PARTITION }; // no system token in reachable marking
    private final static Object[] lateSameDecision = { "forallsafety/noStrategy/lateSameDecision.apt", ENV, AUTO_PARTITION };
    private final static Object[] separateEnvSys = { "forallsafety/separateEnvSys.apt", true, AUTO_PARTITION };
    private final static Object[] envSkipsSys = { "forallsafety/testingNets/envSkipsSys.apt", ENV, AUTO_PARTITION };
    private final static Object[] infiniteSystemTrysToAvoidEnvUseBadPlace = { "forallsafety/testingNets/infiniteSystemTrysToAvoidEnvUseBadPlace.apt", ENV, AUTO_PARTITION };
    private final static Object[] testNotStartingMcut = { "forallsafety/tests/testNotStartingMcut.apt", SYS, AUTO_PARTITION };
    private final static Object[] accessor = { "forallsafety/lukas-panneke/accessor.apt", SYS, PARTITIONED_IN_FILE };
    private final static Object[] chasing = { "forallsafety/lukas-panneke/chasing.apt", SYS, PARTITIONED_IN_FILE };
    private final static Object[] decision = { "forallsafety/lukas-panneke/decision.apt", PROBABLY_SYS, PARTITIONED_IN_FILE };
    private final static Object[] different_choice = { "forallsafety/lukas-panneke/different_choice.apt", ENV, PARTITIONED_IN_FILE };
    private final static Object[] systemDisappears = { "forallsafety/lukas-panneke/systemDisappears.apt", SYS, PARTITIONED_IN_FILE };
    private final static Object[] systemCanWaitForever = { "forallsafety/lukas-panneke/systemCanWaitForever.apt", SYS, AUTO_PARTITION };
    private final static Object[] forkJoinInterrupt = { "forallsafety/lukas-panneke/forkJoinInterrupt.apt", ENV, PARTITIONED_IN_FILE };
    private final static Object[] assassin = { "forallsafety/lukas-panneke/assassin.apt", ENV, PARTITIONED_IN_FILE };
    private final static Object[] isolatedEnvironmentsSingleUse = { "forallsafety/lukas-panneke/isolatedEnvironmentsSingleUse.apt", SYS, AUTO_PARTITION };
    private final static Object[] twobounded = { "forallsafety/lukas-panneke/twobounded.apt", ENV, AUTO_PARTITION };
    private final static Object[] unboundedBlowsUpPetriStrategy1 = { "forallsafety/lukas-panneke/unboundedBlowsUpPetriStrategy-1.apt", SYS, AUTO_PARTITION };
    private final static Object[] unboundedBlowsUpPetriStrategy2 = { "forallsafety/lukas-panneke/unboundedBlowsUpPetriStrategy-2.apt", SYS, PARTITIONED_IN_FILE };

    @DataProvider
    private static Object[][] concurrencyPreservingGames() {
        return new Object[][] {
                unreachableEnvTransition2,
                nondetDeadlock,
                deadlockAvoidance1,
                deadlockAvoidance2,
                firstExamplePaper,
                firstExamplePaper_extended,
                myexample4,
                myexample7,
                vsp_1,
                vsp_1_withBadPlaces,
                lateSameDecision,
                envSkipsSys,
                infiniteSystemTrysToAvoidEnvUseBadPlace,
                separateEnvSys,
                accessor,
                chasing,
                decision,
                different_choice,
                systemCanWaitForever,
                isolatedEnvironmentsSingleUse,
        };
    }

    @DataProvider
    private static Object[][] notConcurrencyPreservingGames() {
        return new Object[][] {
                causalmemory, // the underlying net is cp
                paul,
                finiteWithBad,
                firstTry,
                secondTry,
                thirdTry,
                unreachableEnvTransition, // the underlying net is cp
                nondetDeadlock0,
                myexample000, // the underlying net is cp
                myexample2,
                myexample3,
                robots_false,
                robots_true,
                independentNets,
                minimal, // the underlying net is cp
                minimalNotFinishingEnv,
                testNotStartingMcut,
                systemDisappears,
                forkJoinInterrupt,
                assassin,
                twobounded,
                unboundedBlowsUpPetriStrategy1,
                unboundedBlowsUpPetriStrategy2,
        };
    }


    @DataProvider
    private static Object[][] all() {
        return new Object[][] {
                paul,
                causalmemory,
                finiteWithBad,
                firstTry,
                secondTry,
                thirdTry,
                unreachableEnvTransition,
                unreachableEnvTransition2,
                nondetDeadlock,
                nondetDeadlock0,
                deadlockAvoidance1,
                deadlockAvoidance2,
                firstExamplePaper,
                firstExamplePaper_extended,
                myexample000,
                myexample2,
                myexample3,
                myexample4,
                myexample7,
                robots_false,
                robots_true,
                vsp_1,
                vsp_1_withBadPlaces,
                independentNets,
                minimal,
                minimalNotFinishingEnv,
                lateSameDecision,
                separateEnvSys,
                envSkipsSys,
                infiniteSystemTrysToAvoidEnvUseBadPlace,
                testNotStartingMcut,
                accessor,
                chasing,
                decision,
                different_choice,
                systemDisappears,
                systemCanWaitForever,
                forkJoinInterrupt,
                assassin,
                isolatedEnvironmentsSingleUse,
                twobounded,
                unboundedBlowsUpPetriStrategy1,
                unboundedBlowsUpPetriStrategy2,
        };
    }

    @DataProvider
    private static Object[][] slow() {
        return new Object[][] {
                finiteWithBad,
                firstTry,
                secondTry,
                thirdTry,
                myexample7,
                robots_false,
                robots_true,
                accessor,
                decision,
        };
    }

    @DataProvider
    private static Object[][] specific() {
        return varargs(
                unboundedBlowsUpPetriStrategy1,
                unboundedBlowsUpPetriStrategy2
        );
    }

    private static Object[][] varargs(Object[]... args) {
        List<Object[]> ret = new ArrayList<>();
        for (Object[] arg : args) {
            if (arg instanceof Object[][]) {
                ret.addAll(Arrays.asList((Object[][]) arg));
            } else {
                ret.add(arg);
            }
        }
        return ret.toArray(Object[][]::new);
    }

    private static Object[][] intersect(Object[][]... values) {
        if (values.length == 0) {
            return new Object[0][];
        } else if (values.length == 1) {
            return values[0];
        } else {
            List<Object[]> r = new ArrayList<>(List.of(values[0]));
            for (int i = 1; i < values.length; i++) {
                r.retainAll(List.of(values[i]));
            }
            return r.toArray(Object[][]::new);
        }
    }

    private static Object[][] minus(Object[][] set, Object[][]... subtract) {
        if (subtract.length == 0) {
            return set;
        } else {
            List<Object[]> r = new ArrayList<>(List.of(set));
            for (Object[][] objects : subtract) {
                r.removeAll(List.of(objects));
            }
            return r.toArray(Object[][]::new);
        }
    }

    private static Object[][] filter(Object[][] set, Predicate<Object[]> filter) {
        return List.of(set).stream()
                .filter(filter)
                .toArray(Object[][]::new);
    }

    static {
        Logger.getInstance().setVerbose(true);
    }

    private static final String inputDir = System.getProperty("examplesfolder") + "/";
    private static final String outputDir = System.getProperty("testoutputfolder") + "/";

    @Test(dataProvider = "all")
    public static void existsWinningStrategyTest(String fileName, boolean existsWinningStrategy, List<Set<String>> partition) throws Exception {
        assertEquals(solver(fileName, partition).existsWinningStrategy(), existsWinningStrategy);
    }

    public static void renderPartitions(String fileName, List<Set<String>> partition) throws Exception {
        PGTools.savePG2DotAndPDF(outputDir + fileName, solver(fileName, partition).getGame(), false, true).join();
    }

    @Test(dataProvider = "specific")
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

    @Test
    public static void checkAllPartitionedByConcurrencyPreserving() {
        List<Object[]> tru = List.of(concurrencyPreservingGames());
        List<Object[]> fals = List.of(notConcurrencyPreservingGames());
        for (Object[] args : all()) {
            if (tru.contains(args) == fals.contains(args)) {
                PetriGameWithTransits game = null;
                try {
                    //noinspection unchecked
                    game = game((String) args[0], (List) args[2]);
                } catch (NotSupportedGameException | CouldNotCalculateException | ParseException | IOException e) {
                    fail(Arrays.toString(args) + " is not partitioned regarding cp.");
                }
                Object value = game.getValue(CalculatorIDs.CONCURRENCY_PRESERVING.name());
                if (!(value instanceof Boolean)) {
                    fail(Arrays.toString(args) + " is not partitioned regarding cp.");
                }
                fail(Arrays.toString(args) + " is not partitioned regarding cp. It " + ((Boolean) value ? "is" : "is not") + " cp.");
            }
        }
    }

    @Test(dataProvider = "concurrencyPreservingGames")
    public static void isConcurrencyPreserving(String fileName, boolean hasStrategy, List<Set<String>> partition) throws NotSupportedGameException, CouldNotCalculateException, ParseException, IOException {
        Object value = game(fileName, partition).getValue(CalculatorIDs.CONCURRENCY_PRESERVING.name());
        assertEquals(value.getClass(), Boolean.class);
        assertTrue(((Boolean) value), fileName + " is not concurrency preserving.");
    }

    @Test(dataProvider = "notConcurrencyPreservingGames")
    public static void isNotConcurrencyPreserving(String fileName, boolean hasStrategy, List<Set<String>> partition) throws NotSupportedGameException, CouldNotCalculateException, ParseException, IOException {
        Object value = game(fileName, partition).getValue(CalculatorIDs.CONCURRENCY_PRESERVING.name());
        assertEquals(value.getClass(), Boolean.class);
        assertFalse(((Boolean) value), fileName + " is concurrency preserving.");
    }

    @Test(dataProvider = "all")
    public static void hasTwoTeams(String fileName, boolean unused, List<Set<String>> partition) throws NotSupportedGameException, CouldNotCalculateException, ParseException, IOException {
        PetriGameWithTransits game = game(fileName, partition);
        boolean hasSys = game.getPlaces().stream().anyMatch(game::isSystem);
        boolean hasEnv = game.getPlaces().stream().anyMatch(game::isEnvironment);
        assertTrue(hasEnv && hasSys, fileName + " doesn't have two teams, it's not a game");
    }

    @Test(dataProvider = "all")
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
        BDDTools.saveGraph2PDF(outputDir + justFileName(fileName) + "_graph", bddGraph, solver);
        Thread.sleep(100);
    }

    @Test(dataProvider = "specific")
    public static void renderGraphStrategy(String fileName, boolean existsWinningStrategy, List<Set<String>> partition) throws Exception {
        PetriGameWithTransits game = game(fileName, partition);
        DistrEnvBDDGlobalSafetySolver solver = (DistrEnvBDDGlobalSafetySolver) DistrEnvBDDSolverFactory.getInstance()
                .getSolver(game, new DistrEnvBDDSolverOptions(false, false));
        solver.initialize();
        try {
            BDDGraph bddGraph = BDDGraphAndGStrategyBuilder.getInstance().builtGraphStrategy(solver, null);
            BDDTools.saveGraph2PDF(outputDir + justFileName(fileName) + "_graph_strategy", bddGraph, solver);
            Thread.sleep(100);
        } catch (NoStrategyExistentException e) {
            if (existsWinningStrategy) {
                fail();
            }
            return;
        }
        if (!existsWinningStrategy) {
            fail();
        }
    }

    @Test(dataProvider = "specific")
    public static void renderPetriStrategy(String fileName, boolean existsWinningStrategy, List<Set<String>> partition) throws Exception {
        PetriGameWithTransits game = game(fileName, partition);
        DistrEnvBDDGlobalSafetySolver solver = (DistrEnvBDDGlobalSafetySolver) DistrEnvBDDSolverFactory.getInstance()
                .getSolver(game, new DistrEnvBDDSolverOptions(false, false));
        solver.initialize();
        try {
            PetriGameWithTransits strategy = solver.getStrategy();
            PGTools.savePG2PDF(outputDir + justFileName(fileName) + "_petri_strategy", strategy, false);
            Thread.sleep(100);
        } catch (NoStrategyExistentException e) {
            if (existsWinningStrategy) {
                fail();
            }
            return;
        }
        if (!existsWinningStrategy) {
            fail();
        }
    }

    @Test(dataProvider = "specific")
    public static void renderGraphGameAndStrategies(String fileName, boolean existsWinningStrategy, List<Set<String>> partition) throws Exception {
        renderGraph(fileName, existsWinningStrategy, partition);
        renderGraphStrategy(fileName, existsWinningStrategy, partition);
        renderPetriStrategy(fileName, existsWinningStrategy, partition);
    }

    @Test(dataProvider = "specific")
    public static void strategyRespectsFlowsOfGame(String fileName, boolean existsWinningStrategy, List<Set<String>> partition) throws Exception {
        if (!existsWinningStrategy) {
            return;
        }
        PetriGameWithTransits game = game(fileName, partition);
        DistrEnvBDDGlobalSafetySolver solver = (DistrEnvBDDGlobalSafetySolver) DistrEnvBDDSolverFactory.getInstance()
                .getSolver(game, new DistrEnvBDDSolverOptions(false, false));
        solver.initialize();

        DistrEnvBDDGlobalSafetyPetriGameStrategyBuilder strategyBuilder = new DistrEnvBDDGlobalSafetyPetriGameStrategyBuilder(solver, solver.getGraphStrategy());
        Set<Marking> gameMarkings = StreamSupport.stream(CoverabilityGraph.getReachabilityGraph(game).getNodes().spliterator(), false).map(CoverabilityGraphNode::getMarking).collect(Collectors.toSet());
        iterateBreadthFirstWithoutRepeat(CoverabilityGraph.getReachabilityGraph(strategyBuilder.build()), edge -> {
            Marking predecessor_s = edge.getSource().getMarking();
            Marking successor_s = edge.getTarget().getMarking();
            Transition transition_s = edge.getTransition();
            Marking predecessor_g = strategyBuilder.lambda(predecessor_s);
            Marking successor_g = strategyBuilder.lambda(successor_s);
            Transition transition_g = strategyBuilder.lambda(transition_s);

            assertTrue(gameMarkings.contains(predecessor_g));
            assertEquals(predecessor_g.fireTransitions(transition_g), successor_g);
        });
    }

    private static void iterateBreadthFirstWithoutRepeat(CoverabilityGraph graph, Consumer<CoverabilityGraphEdge> forEachEdge) {
        Set<CoverabilityGraphNode> visited = new HashSet<>();
        Queue<CoverabilityGraphNode> q = new LinkedList<>();
        q.add(graph.getInitialNode());
        while (!q.isEmpty()) {
            CoverabilityGraphNode current = q.poll();
            Set<CoverabilityGraphEdge> outgoingEdges = current.getPostsetEdges();
            for (CoverabilityGraphEdge outgoingEdge : outgoingEdges) {
                CoverabilityGraphNode next = outgoingEdge.getTarget();
                forEachEdge.accept(outgoingEdge);
                if (!visited.contains(next)) {
                    q.add(next);
                    visited.add(next);
                }
            }
        }
    }

    @Test(dataProvider = "all")
    public static void petriStrategySafe(String fileName, boolean existsWinningStrategy, List<Set<String>> partition) throws Exception {
        if (existsWinningStrategy) {
            assertTrue(solver(fileName, partition).getStrategy().getBounded().isSafe(), "Petri game strategy is not safe");
        }
    }

    private static DistrEnvBDDGlobalSafetySolver solver(String fileName, List<Set<String>> partition) throws SolvingException, CouldNotFindSuitableConditionException, IOException, ParseException {
        return (DistrEnvBDDGlobalSafetySolver) DistrEnvBDDSolverFactory.getInstance()
                .getSolver(game(fileName, partition), new DistrEnvBDDSolverOptions(false, false));
    }

    private static PetriGameWithTransits game(String fileName, List<Set<String>> partition) throws NotSupportedGameException, CouldNotCalculateException, ParseException, IOException {
        PetriGameWithTransits game = PGTools.getPetriGame(path(fileName), false, false);
        game.addExtensionCalculator(CalculatorIDs.CONCURRENCY_PRESERVING.name(), new ConcurrencyPreservingGamesCalculator());
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
