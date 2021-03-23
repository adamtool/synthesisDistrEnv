package uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.safety;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import org.apache.commons.collections4.CollectionUtils;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.objectives.global.GlobalSafety;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;
import uniolunisaar.adam.exceptions.pnwt.CalculationInterruptedException;
import uniolunisaar.adam.logic.synthesis.builder.symbolic.bddapproach.distrenv.DistrEnvBDDGlobalSafetyPetriGameStrategyBuilder;
import uniolunisaar.adam.logic.synthesis.builder.twoplayergame.symbolic.bddapproach.BDDGraphAndGStrategyBuilder;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.BDDSolver;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolver;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.benchmarks.synthesis.Benchmarks;
import uniolunisaar.adam.util.symbolic.bddapproach.BDDTools;

/**
 * {@link #existsWinningStrategy() Decide the existence of a wining stratey}
 * for a {@link PetriGameWithTransits petri game} with bad markings.
 * <p>
 * Construct a two player graph game
 * as described in
 * Synthesis in Distributed Environments
 * by Bernd Finkbeiner and Paul Gölz
 * in Figure 2: Graph'(G).
 * <p>
 * A vertex in the graph game is made up of
 * a marking in the petri game,
 * a subset of the system transitions known as the commitment set
 * and a subset of the marking known as the responsibility set.
 * <p>
 * An edge in the graph game is made up of two vertices.
 * It's predecessor vertex it the state enabling the transition
 * and it's successor vertex is the state of the game after taking the transition.
 * The taken transition is not encoded,
 * because it can be inferred from the change in the marking.
 * <p>
 * To encode the marking efficiently the set of places is partitioned.
 * All system places are in the same partition.
 * Two environment places cannot be in the same partition
 * if they can be marked at the same time.
 * Thus every transition can have at most one place per partition
 * in it's preset and it's postset.
 * {@link DistrEnvBDDSolvingObject#partitionPlaces(PetriGameWithTransits, boolean)}
 * can in most cases find a valid partitioning.
 * <p>
 * A petri game is concurrency preserving,
 * if the number of tokens is the same in every reachable marking
 * and no token ever changes teams.
 * Thus in concurrency preserving games there is one token in every partition
 * in every reachable marking.
 * For non concurrency preserving games
 * there is also the option to have no token,
 * so every partition has zero or one tokens in every reachable marking.
 * <p>
 * The system player may refuse to take a transition.
 * Whenever a system transition is taken
 * or a new system token is created
 * the graph game enters a Top state,
 * to allow the system player to choose a commitment set.
 * A system transition can only fire, if it's in the commitment set.
 * <p>
 * The responsibility set prevents the environment player
 * from giving the system player too much information.
 * Without this restriction the graph game strategy could contain information,
 * that cannot be translated to a petri game strategy.
 * <p>
 * The graph game can be extracted with
 * {@link BDDGraphAndGStrategyBuilder#builtGraph(BDDSolver) BDDGraphAndGStrategyBuilder.getInstance().builtGraph(solver)}.
 * The graph strategy can be extracted with
 * {@link BDDGraphAndGStrategyBuilder#builtGraphStrategy(BDDSolver, Map) BDDGraphAndGStrategyBuilder.getInstance().builtGraphStrategy(solver, null)}.
 * The Petri game strategy can be build with
 * {@link DistrEnvBDDGlobalSafetyPetriGameStrategyBuilder}.
 *
 * @implSpec The system must have partition 0.
 * @implNote When there is no commitment set in the graph game (Top vertex)
 * the implementation encodes the empty commitment set.
 */
public class DistrEnvBDDGlobalSafetySolver extends DistrEnvBDDSolver<GlobalSafety> {

    /*
     * Every token (player) has their own partition of places.
     * That token will only ever be in places from their partition.
     *
     * Partition 0 is for the system player.
     */
    protected static final int PARTITION_OF_SYSTEM_PLAYER = 0;

    /*
     * When the game is not concurrency preserving,
     * there can be no token in a partition.
     * This is marked by setting the partition's place to 0.
     *
     * For this to work the ids of the places must be
     * 1 <= id <= number of places in partition.
     * When the net is concurrency preserving,
     * there is no need for a partition without a token.
     * Then the id 0 must not have special semantics
     * and can be used a normal id.
     * Thus in the concurrency preserving case
     * 0 <= id < number of places in partition.
     */
    protected static final int NO_TOKEN_IN_PARTITION = 0;

    /*
     * The java source code variable (not bdd variable) 'pos' always means
     * 0 for the predecessor bdd variables and 1 for the successor bdd variables.
     */
    protected static final int PREDECESSOR = 0;
    protected static final int SUCCESSOR = 1;

    protected static final int TRUE = 1;
    protected static final int FALSE = 0;
    protected static final int UNKNOWN = -1;

    /* [pos] */
    protected BDDDomain[] TRANSITIONS;
    /* [pos] */
    protected BDDDomain[] TOP;
    /* [pos] */
    protected BDDDomain[] RESPONSIBILITY;

    /**
     * Stream collector for or-ing a stream of BDDs.
     */
    private Collector<BDD, BDD, BDD> or() {
        return Collector.of(
                this::getZero,
                BDD::orWith,
                BDD::orWith
        );
    }

    /**
     * Stream collector for or-ing a stream of BDDs.
     */
    private Collector<BDD, BDD, BDD> xor() {
        return Collector.of(
                this::getZero,
                BDD::xorWith,
                BDD::xorWith
        );
    }

    /**
     * Stream collector for and-ing a stream of BDDs.
     */
    private Collector<BDD, BDD, BDD> and() {
        return Collector.of(
                this::getOne,
                BDD::andWith,
                BDD::andWith
        );
    }

    DistrEnvBDDGlobalSafetySolver(DistrEnvBDDSolvingObject<GlobalSafety> obj, DistrEnvBDDSolverOptions opts) {
        super(obj, opts);
    }

    /* <variables> */

    @Override
    protected void createVariables() {
        int numberOfPartitions = this.getSolvingObject().getMaxTokenCountInt();
        PLACES = new BDDDomain[2][numberOfPartitions];
        TRANSITIONS = new BDDDomain[2];
        TOP = new BDDDomain[2];
        RESPONSIBILITY = new BDDDomain[2];
        for (int pos : List.of(PREDECESSOR, SUCCESSOR)) {
            for (int partition = 0; partition < numberOfPartitions; partition++) {
                PLACES[pos][partition] = this.getFactory().extDomain(
                        this.getSolvingObject().getDevidedPlaces()[partition].size()
                                + (this.getSolvingObject().isConcurrencyPreserving() ? 0 : 1));

                /* for every system transition the system player must choose whether or not to allow that transition. */
                TRANSITIONS[pos] = this.getFactory().extDomain(BigInteger.TWO.pow(this.getSolvingObject().getSystemTransitions().size()));
                TOP[pos] = this.getFactory().extDomain(2);
                RESPONSIBILITY[pos] = this.getFactory().extDomain(BigInteger.TWO.pow(numberOfPartitions));
            }
        }
        setDCSLength(getFactory().varNum() / 2); // TODO what is this?
    }

    @Override
    protected BDD getVariables(int pos) {
        return TOP[pos].set()
                .and(TRANSITIONS[pos].set())
                .and(RESPONSIBILITY[pos].set())
                .and(Arrays.stream(PLACES[pos])
                        .map(BDDDomain::set)
                        .collect(and()));
    }

    @Override
    protected BDD preBimpSucc() {
        return TOP[PREDECESSOR].buildEquals(TOP[SUCCESSOR])
                .and(TRANSITIONS[PREDECESSOR].buildEquals(TRANSITIONS[SUCCESSOR]))
                .and(RESPONSIBILITY[PREDECESSOR].buildEquals(RESPONSIBILITY[SUCCESSOR]))
                .and(this.streamPartitions()
                        .mapToObj(partition -> PLACES[PREDECESSOR][partition].buildEquals(PLACES[SUCCESSOR][partition]))
                        .collect(and()));
    }

    /* </variables> */

    private BDD codeSystemTransition(Transition transition, int pos) {
        int transitionIndex = this.getSolvingObject().getSystemTransitions().indexOf(transition);
        return this.getFactory().ithVar(TRANSITIONS[pos].vars()[transitionIndex]);
    }

    protected BDD codeResponsibility(int partition, int pos) {
        return this.getFactory().ithVar(RESPONSIBILITY[pos].vars()[partition]);
    }

    protected IntStream streamPartitions() {
        return streamPartitions(true);
    }

    protected IntStream streamPartitions(boolean includeSystem) {
        return IntStream.range(includeSystem ? 0 : 1, this.getSolvingObject().getMaxTokenCountInt());
    }

    protected BDD graphGame_initialVertex(int pos) {
        return codeMarking(this.getGame().getInitialMarking(), pos, true)
                /*
                 * the system must choose it's initial commitment set,
                 * only if there is an initial system player.
                 */
                .andWith(top(pos).biimpWith(tokenExists(PARTITION_OF_SYSTEM_PLAYER, pos)))
                .andWith(nothingChosen(pos))
                .andWith(onlySystemInResponsibilitySet(PREDECESSOR));
    }

    /**
     * Encode all edge coming out of a player 0 vertex in the graph game.
     * These only make player 0 choose a commitment set.
     * <p>
     * These are edges where the predecessor vertices are those marked with TOP.
     * <p>
     * In the paper these are the edges of type E1.
     */
    protected BDD graphGame_player0Edges() {
        BDD ret = getOne();
        ret.andWith(markingsEqual()).andWith(onlyExistingPlacesInMarking(PREDECESSOR));

        /* the purpose of this edge is to remove the top. */
        ret.andWith(top(PREDECESSOR)).andWith(notTop(SUCCESSOR));

        /*
         * only transitions in the postset of the current system place can be chosen.
         */
        ret.andWith(onlyChooseTransitionsInPostsetOfSystemPlace(SUCCESSOR));

        ret.andWith(onlySystemInResponsibilitySet(PREDECESSOR).andWith(onlySystemInResponsibilitySet(SUCCESSOR)));

        /*
         * OPTIONAL
         * if no purely environmental transition is enabled,
         * but some system transitions are,
         * the system must only choose currently enabled transitions,
         * because no other marking can be reached before a system transition fires.
         */
        ret.andWith(onlySystemTransitionsEnabled(SUCCESSOR).impWith(onlyChooseEnabledTransitions(SUCCESSOR)));

        /*
         * we don't specify the chosen commitment set,
         * but let the bdd solver find all suitable commitment sets.
         */
        return ret;
    }

    /**
     * Encode an edge coming out of a player 1 vertex in the graph game.
     */
    protected BDD graphGame_player1Edge(Transition transition) {
        if (this.getSolvingObject().getSystemTransitions().contains(transition)) {
            return this.graphGame_player1Edge_systemTransition(transition);
        } else {
            return this.graphGame_player1Edge_purelyEnvironmentalTransition(transition);
        }
    }

    /**
     * In the paper these are the edges of type E2.
     */
    protected BDD graphGame_player1Edge_purelyEnvironmentalTransition(Transition transition) {
        if (this.getSolvingObject().getSystemTransitions().contains(transition)) {
            throw new IllegalArgumentException(transition + " is a system transition");
        }

        boolean createsNewSystemToken = transition.getPostset().stream()
                .anyMatch(place -> this.getGame().isSystem(place));

        BDD ret = getOne();
        if (createsNewSystemToken) {
            ret.andWith(notTop(PREDECESSOR)).andWith(top(SUCCESSOR));
            ret.andWith(nothingChosen(SUCCESSOR));
        } else {
            ret.andWith(notTop(PREDECESSOR)).andWith(notTop(SUCCESSOR));
            ret.andWith(commitmentsEqual());
        }

        BDD add, remove = transition.getPreset().stream()
                .map(this.getGame()::getPartition)
                .map(partition -> codeResponsibility(partition, SUCCESSOR))
                .collect(and());
        if (createsNewSystemToken) {
            /*
             * if this transition creates a new system token,
             * choose that as the new entry to the responsibility set.
             * otherwise there is no way for the system token to add the system in the future,
             * meaning that no system transition can ever fire.
             */
            add = codeResponsibility(PARTITION_OF_SYSTEM_PLAYER, SUCCESSOR);
        } else {
            /*
             * if no system token is created, choose a random token.
             */
            add = transition.getPostset().stream()
                    .map(this.getGame()::getPartition)
                    .map(partition -> codeResponsibility(partition, SUCCESSOR))
                    .collect(xor());
        }
        ret.andWith(responsibilitiesEqual().exist(remove).andWith(remove.not()).exist(add).andWith(add));
        ret.andWith(responsibilitySubsetMarking(SUCCESSOR));

        ret.andWith(fire(transition));
        return ret;
    }

    /**
     * In the paper these are the edges of type E3.
     */
    protected BDD graphGame_player1Edge_systemTransition(Transition transition) {
        if (!this.getSolvingObject().getSystemTransitions().contains(transition)) {
            throw new IllegalArgumentException(transition + " is not a system transition");
        }
        BDD ret = getOne();
        ret.andWith(notTop(PREDECESSOR)).andWith(top(SUCCESSOR));
        ret.andWith(chosen(transition, PREDECESSOR));
        /*
         * no new commitment set is chosen yet.
         * that happens in the edge going out of the next player 0 vertex.
         * the old commitment set is no  longer relevant,
         * because it's replaced with TOP.
         * the commitment set must have a value.
         * the empty set is chosen arbitrarily.
         */
        ret.andWith(nothingChosen(SUCCESSOR));
        ret.andWith(fire(transition));

        Set<Integer> prePartitions = transition.getPreset().stream()
                .map(this.getGame()::getPartition)
                .collect(Collectors.toSet());
        BDD responsibilitySubsetPreset = this.streamPartitions(false)
                .filter(partition -> !prePartitions.contains(partition))
                .mapToObj(partition -> codeResponsibility(partition, PREDECESSOR).not())
                .collect(and());
        ret.andWith(responsibilitySubsetPreset);
        ret.andWith(onlySystemInResponsibilitySet(SUCCESSOR));

        return ret;
    }

    /**
     * Calculate a BBD representing all vertices which must be avoided.
     * <p>
     * In the paper these are the vertices in the set X.
     */
    private BDD graphGame_badVertices(int pos) {
        return this.graphGame_badMarking(pos).orWith(this.graphGame_nondeterministic(pos)).orWith(this.graphGame_deadlock(pos));
    }

    /**
     * Calculates a BBD representing all vertices in which a bad marking is reached.
     * <p>
     * In the paper these are the vertices of type X1.
     */
    protected BDD graphGame_badMarking(int pos) {
        return this.getSolvingObject().getBadMarkings().stream()
                .map(marking -> codeMarking(marking, pos, false))
                .collect(or());
    }

    /**
     * Calculates a BBD representing all vertices in which player 0 must make a choice, but is undecided.
     * <p>
     * A vertex is non deterministic if
     * 2 transitions sharing a system place
     * are firable in a reachable marking
     * and both transitions are chosen.
     * <p>
     * A player 0 vertex cannot cause a deadlock, because no commitment is chosen yet.
     * <p>
     * In the paper these are the vertices of type X2a (and X2b).
     */
    protected BDD graphGame_nondeterministic(int pos) {
        BDD ret = this.getZero();
        Set<Transition> trans = this.getGame().getTransitions();
        for (Transition t1 : trans) {
            for (Transition t2 : trans) {
                if (t1.equals(t2) || !this.getGame().eventuallyEnabled(t1, t2)) {
                    continue;
                }
                BDD sharedSystemPlacesEncoded = CollectionUtils.intersection(t1.getPreset(), t2.getPreset()).stream()
                        .filter(this.getGame()::isSystem)
                        .map(place -> codePlace(place, pos, this.getGame().getPartition(place)))
                        .collect(or());
                ret = ret.orWith(sharedSystemPlacesEncoded.andWith(this.chosen(t1, pos).andWith(this.chosen(t2, pos))));
            }
        }
        return notTop(pos).andWith(ret);
    }

    /**
     * Calculates a BBD representing all vertices in which some transitions are enabled, but none chosen.
     * <p>
     * A vertex has a deadlock if
     * no purely environmental transitions are enabled,
     * and some system transitions are enabled,
     * but player 0 has none in their commitment set (refuses all options).
     * <p>
     * A player 0 vertex cannot cause a deadlock, because no commitment is chosen yet.
     * <p>
     * In the paper these are the vertices of type X3.
     */
    private BDD graphGame_deadlock(int pos) {
        BDD noSystemChosen = this.getSolvingObject().getSystemTransitions().stream()
                .map(transition -> this.enabled(transition, pos).andWith(this.chosen(transition, pos)).not())
                .collect(and());
        return notTop(pos).andWith(onlySystemTransitionsEnabled(pos)).andWith(noSystemChosen);
    }

    protected BDD graphGame_player1Edges() {
        return this.getGame().getTransitions().stream()
                .map(this::graphGame_player1Edge)
                .collect(or());
    }

    protected BDD graphGame_Edges() {
        return graphGame_player0Edges().orWith(graphGame_player1Edges());
    }

    protected BDD graphGame_reachableVertices(BDD startVertex) throws CalculationInterruptedException {
        return graphGame_reachableVertices(startVertex, null);
    }

    protected BDD graphGame_reachableVertices(BDD startVertex, Map<Integer, BDD> steps) throws CalculationInterruptedException {
        BDD edges = graphGame_Edges();
        return fixpoint(getZero(), startVertex, Q -> Q.or(getSuccs(Q.and(edges))), steps);
    }

    protected BDD graphGame_reachableEdges(BDD startVertex) throws CalculationInterruptedException {
        return graphGame_reachableEdges(startVertex, null);
    }

    protected BDD graphGame_reachableEdges(BDD startVertex, Map<Integer, BDD> steps) throws CalculationInterruptedException {
        //BDD vertices = graphGame_reachableVertices(startVertex);
        //return vertices.and(shiftFirst2Second(vertices));

        BDD edges = graphGame_Edges();
        return fixpoint(getZero(), startVertex.and(edges), Q -> Q.or(getSuccs(Q).and(edges)), steps);
    }

    protected BDD graphGame_poisonedVertices(BDD reachableEdges, BDD badVertices) throws CalculationInterruptedException {
        return graphGame_poisonedVertices(reachableEdges, badVertices, null);
    }

    protected BDD graphGame_poisonedVertices(BDD reachableEdges, BDD badVertices, Map<Integer, BDD> steps) throws CalculationInterruptedException {
        BDD player0Edges = graphGame_player0Edges().and(reachableEdges);
        BDD player1Edges = graphGame_player1Edges().and(reachableEdges);

        return fixpoint(getZero(), badVertices, Q -> Q.or(transmitPoison(Q, player0Edges, player1Edges)), steps).or(badVertices).and(wellformed(PREDECESSOR));
    }

    protected BDD graphGame_winningVertices() throws CalculationInterruptedException {
        BDD reachableEdges = graphGame_reachableEdges(graphGame_initialVertex(PREDECESSOR));
        BDD poisonedVertices = graphGame_poisonedVertices(reachableEdges, graphGame_badVertices(PREDECESSOR));
        BDD reachableVertices = graphGame_reachableVertices(graphGame_initialVertex(PREDECESSOR));
        //BDD reachableVertices = getSuccs(reachableEdges);
        return reachableVertices.and(poisonedVertices.not().andWith(wellformed()));
    }

    protected BDD fire(Transition transition) {
        PetriGameWithTransits game = this.getSolvingObject().getGame();

        Set<Place> pre = transition.getPreset();
        Set<Place> post = transition.getPostset();

        Map<Integer, Place> prePartitionToPlace = pre.stream()
                .collect(Collectors.toMap(game::getPartition, Function.identity()));
        Map<Integer, Place> postPartitionToPlace = post.stream()
                .collect(Collectors.toMap(game::getPartition, Function.identity()));

        Set<Integer> prePartitions = pre.stream()
                .map(game::getPartition)
                .collect(Collectors.toSet());
        Set<Integer> postPartitions = post.stream()
                .map(game::getPartition)
                .collect(Collectors.toSet());

        /* partitions that had a token before the transition has fired, but don't have one afterwards */
        /* CP => nothing consumed */
        assert !getSolvingObject().isConcurrencyPreserving() || CollectionUtils.subtract(prePartitions, postPartitions).isEmpty();
        BDD consumed = CollectionUtils.subtract(prePartitions, postPartitions).stream()
                .map(partition -> codePlace(prePartitionToPlace.get(partition), PREDECESSOR, partition).andWith(notUsedToken(SUCCESSOR, partition)))
                .collect(and());
        /*
         * notUsedToken(SUCCESSOR, partition) is equal to codePlace(0, SUCCESSOR, partition).
         * This could be problematic, because in the concurrency preserving case
         * the place id 0 does refer to an actual place.
         * But in the concurrency preserving case
         * no token can ever be taken out of a partition,
         * but only be moved within the partition.
         * Thus there can never ba a partition without a token
         * and this code is never executed when it could be problematic.
         */

        /* partitions that had no token before the transition has fired, but have one afterwards */
        /* CP => nothing produced */
        assert !getSolvingObject().isConcurrencyPreserving() || CollectionUtils.subtract(postPartitions, prePartitions).isEmpty();
        BDD produced = CollectionUtils.subtract(postPartitions, prePartitions).stream()
                .map(partition -> notUsedToken(PREDECESSOR, partition).andWith(codePlace(postPartitionToPlace.get(partition), SUCCESSOR, partition)))
                .collect(and());

        /* partitions that have a token before and after the transition fires */
        BDD moved = CollectionUtils.intersection(postPartitions, prePartitions).stream()
                .map(partition -> codePlace(prePartitionToPlace.get(partition), PREDECESSOR, partition).andWith(codePlace(postPartitionToPlace.get(partition), SUCCESSOR, partition)))
                .collect(and());

        /* partitions that have no contact with the transition */
        BDD unaffected = this.streamPartitions()
                .filter(o -> !prePartitions.contains(o))
                .filter(o -> !postPartitions.contains(o))
                .mapToObj(this::markingEqual)
                .collect(and())
                .andWith(onlyExistingPlacesInMarking(PREDECESSOR))
                .andWith(onlyExistingPlacesInMarking(SUCCESSOR));

        BDD flows = consumed
                .andWith(produced)
                .andWith(moved)
                .andWith(unaffected);

        return enabled(transition, PREDECESSOR).andWith(flows);
    }

    protected BDD onlyChooseTransitionsInPostsetOfSystemPlace(Place systemPlace, int pos) {
        Collection<Transition> unrelatedTransitions = CollectionUtils.subtract(
                this.getSolvingObject().getSystemTransitions(),
                systemPlace.getPostset());
        return codePlace(systemPlace, pos, PARTITION_OF_SYSTEM_PLAYER)
                .impWith(unrelatedTransitions.stream()
                        .map(transition -> codeSystemTransition(transition, pos).not())
                        .collect(and()));
    }

    protected BDD onlyChooseTransitionsInPostsetOfSystemPlace(int pos) {
        BDD ret = this.getSolvingObject().getSystemPlaces().stream()
                .map(place -> onlyChooseTransitionsInPostsetOfSystemPlace(place, pos))
                .collect(and());
        if (!this.getSolvingObject().isConcurrencyPreserving()) {
            ret.andWith(notUsedToken(pos, PARTITION_OF_SYSTEM_PLAYER).impWith(nothingChosen(pos)));
        }
        return ret;
    }

    protected BDD someSystemTransitionEnabled(int pos) {
        return this.getSolvingObject().getSystemTransitions().stream()
                .map(transition -> this.enabled(transition, pos))
                .collect(or());
    }

    protected BDD noSystemTransitionEnabled(int pos) {
        return this.getSolvingObject().getSystemTransitions().stream()
                .map(transition -> this.enabled(transition, pos).not())
                .collect(and());
    }

    protected BDD somePurelyEnvironmentalTransitionEnabled(int pos) {
        return this.getSolvingObject().getPurelyEnvironmentalTransitions().stream()
                .map(transition -> this.enabled(transition, pos))
                .collect(or());
    }

    protected BDD noPurelyEnvironmentalTransitionEnabled(int pos) {
        return this.getSolvingObject().getPurelyEnvironmentalTransitions().stream()
                .map(transition -> this.enabled(transition, pos).not())
                .collect(and());
    }

    protected BDD onlySystemTransitionsEnabled(int pos) {
        return noPurelyEnvironmentalTransitionEnabled(pos).andWith(someSystemTransitionEnabled(pos));
    }

    protected BDD someTransitionEnabled(int pos) {
        return someSystemTransitionEnabled(pos).orWith(somePurelyEnvironmentalTransitionEnabled(pos));
    }

    protected BDD onlyChooseEnabledTransitions(int pos) {
        return this.getSolvingObject().getSystemTransitions().stream()
                .map(transition -> enabled(transition, pos).not().impWith(codeSystemTransition(transition, pos).not()))
                .collect(and());
    }

    protected BDD onlyExistingPlacesInMarking(int pos) {
        return this.streamPartitions()
                .mapToObj(partition -> this.getSolvingObject().getDevidedPlaces()[partition].stream()
                        .map(place -> codePlace(place, pos, partition))
                        .collect(or())
                        .orWith(notUsedToken(pos, partition)))
                /*
                 * notUsedToken(pos, partition) is equal to codePlace(0, pos, partition).
                 * This could be problematic, because in the not concurrency preserving case
                 * the place id 0 does refer to an actual place.
                 * But in the big or statement the place with id 0 is already encoded,
                 * because there cannot be a partition without any places.
                 * Thus on the concurrency preserving case
                 * or-ing with notUsedToken(pos, partition) does nothing.
                 */
                .collect(and());
    }

    protected BDD tokenExists(int partition, int pos) {
        if (this.getSolvingObject().isConcurrencyPreserving()) {
            return getOne();
        } else {
            return notUsedToken(pos, partition).not().and(onlyExistingPlacesInMarking(pos));
        }
    }

    protected BDD markingEqual(int partition) {
        return PLACES[PREDECESSOR][partition].buildEquals(PLACES[SUCCESSOR][partition]);
    }

    protected BDD markingsEqual() {
        return this.streamPartitions()
                .mapToObj(partition -> markingEqual(partition))
                .collect(and());
    }

    protected BDD commitmentsEqual() {
        return TRANSITIONS[PREDECESSOR].buildEquals(TRANSITIONS[SUCCESSOR]);
    }

    protected BDD nothingChosen(int pos) {
        return TRANSITIONS[pos].ithVar(0);
    }

    protected BDD somethingChosen(int pos) {
        return this.getSolvingObject().getSystemTransitions().stream()
                .map(transition -> codeSystemTransition(transition, pos))
                .collect(or());
    }

    protected BDD responsibilitiesEqual() {
        return RESPONSIBILITY[PREDECESSOR].buildEquals(RESPONSIBILITY[SUCCESSOR]);
    }

    protected BDD onlySystemInResponsibilitySet(int pos) {
        return codeResponsibility(PARTITION_OF_SYSTEM_PLAYER, pos).biimpWith(tokenExists(PARTITION_OF_SYSTEM_PLAYER, pos))
                .andWith(this.streamPartitions(false)
                        .mapToObj(partition -> codeResponsibility(partition, pos).not())
                        .collect(and()));
    }

    protected BDD responsibilityContainsSystem(int pos) {
        return codeResponsibility(PARTITION_OF_SYSTEM_PLAYER, pos).biimpWith(tokenExists(PARTITION_OF_SYSTEM_PLAYER, pos));
    }

    protected BDD responsibilitySubsetMarking(int pos) {
        return this.streamPartitions()
                .mapToObj(partition -> codeResponsibility(partition, pos).impWith(tokenExists(partition, pos)))
                .collect(and());
    }

    protected BDD transmitPoison(BDD poisoned, BDD allEdges, BDD existsEdges) {
        BDD poisonedAsSuccessor = shiftFirst2Second(poisoned);
        BDD successorVariables = getVariables(SUCCESSOR);
        BDD forall = allEdges.exist(successorVariables) /* there is an edge controlled by player 0 */
                .and((allEdges.imp(poisonedAsSuccessor)).forAll(successorVariables)); /* and every edge controlled by player 0 leads into a poisoned vertex */
        BDD exists = (existsEdges.and(poisonedAsSuccessor)).exist(successorVariables); /* there is an edge controlled by player 1 leading into a a poisoned vertex */
        return forall.or(exists).and(wellformed());
    }

    protected BDD fixpoint(BDD Q, BDD q, Function<BDD, BDD> iteratedFunction, Map<Integer, BDD> steps) throws CalculationInterruptedException {
        Logger.getInstance().addMessage("Calculating fixpoint ...");
        Benchmarks.getInstance().start(Benchmarks.Parts.FIXPOINT);
        int i = 0;
        while (!Q.equals(q)) {
            if (Thread.interrupted()) {
                CalculationInterruptedException e = new CalculationInterruptedException();
                Logger.getInstance().addError(e.getMessage(), e);
                throw e;
            }
            Q = q;
            if (steps != null) {
                steps.put(i++, Q);
            }
            q = iteratedFunction.apply(Q);
        }
        Benchmarks.getInstance().stop(Benchmarks.Parts.FIXPOINT);
        Logger.getInstance().addMessage("... calculation of fixpoint done.");
        return Q;
    }

    @Override
    protected BDD wellformed(int pos) {
        /* only places that exist may be encoded */
        BDD ret = getOne();
        ret.andWith(onlyExistingPlacesInMarking(pos));
        /*
         * only transitions in the postset of the system player may be in the commitment.
         * in top states there is no commitment set, thus there are no constraints on it.
         */
        ret.andWith(notTop(pos).impWith(onlyChooseTransitionsInPostsetOfSystemPlace(pos)));
        ret.andWith(responsibilitySubsetMarking(pos)).andWith(responsibilityContainsSystem(pos));

        return ret;
    }

    protected boolean hasTop(BDD state, int pos) {
        return !state.and(top(pos)).isZero();
    }

    protected BDD top(int pos) {
        return TOP[pos].ithVar(1);
    }

    protected BDD notTop(int pos) {
        return TOP[pos].ithVar(0);
    }

    /**
     * For the initial marking we want to encode the exact marking.
     * But for bad Markings we want to compare,
     * if a given marking contains the bad marking.
     * For the latter we are not interested in encoding
     * that a partition is without a marking,
     * because actually want to include all markings,
     * that are greater (>=) then the bad marking.
     */
    protected BDD codeMarking(Marking marking, int pos, boolean encodeMissingToken) {
        return this.streamPartitions()
                .mapToObj(partition -> {
                    Optional<Place> place = getPlaceOfPartitionInMarking(marking, partition);
                    if (place.isPresent()) {
                        return codePlace(place.get(), pos, partition);
                    } else if (encodeMissingToken) {
                        assert !this.getSolvingObject().isConcurrencyPreserving();
                        return notUsedToken(pos, partition);
                    } else {
                        return getOne();
                    }
                })
                .collect(and());
    }

    /**
     * Find for a given partition, that place, that has a token in the given marking.
     * <p>
     * Since there is exactly one token in every partition for concurrency preserving nets,
     * and at most one token for not concurrency preserving nets,
     * this place is unique, if it exists.
     */
    protected Optional<Place> getPlaceOfPartitionInMarking(Marking marking, int partition) {
        Set<Place> markedPlacesWithMatchingPartition = this.getGame().getPlaces().stream()
                .filter(place -> !marking.getToken(place).isOmega() && marking.getToken(place).getValue() > 0)
                .filter(place -> this.getGame().getPartition(place) == partition)
                .collect(Collectors.toSet());
        assert markedPlacesWithMatchingPartition.size() <= 1;
        return markedPlacesWithMatchingPartition.stream().findAny();
    }

    /**
     * Calculates a BDD where a token on the system place in the preset of the transition implies the transition is in the commitment set.
     */
    protected BDD chosen(Transition transition, int pos) {
        if (!this.getSolvingObject().getSystemTransitions().contains(transition)) {
            return getOne();
        }
        Place systemPlace = this.getSolvingObject().getSystemPlaceOfTransition(transition, false);
        return this.codePlace(systemPlace, pos, PARTITION_OF_SYSTEM_PLAYER)
                .impWith(codeSystemTransition(transition, pos));
    }

    /**
     * Calculates a BDD where all places in the preset of the transition are encoded.
     */
    @Override
    protected BDD enabled(Transition transition, int pos) {
        return transition.getPreset().stream()
                .map(place -> codePlace(place, pos, this.getGame().getPartition(place)))
                .collect(and());
    }

    /* <overwrites for inheritance> */

    @Override
    protected String decodeDCS(byte[] dcs, int pos) {
        return decodeVertex(dcs, pos);
    }

    @Override
    public String decode(byte[] dcs) {
        return decodeEdge(dcs);
    }

    @Override
    protected BDD calcSystemTransition(Transition transition) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected BDD calcSystemTransitions() {
        return graphGame_player0Edges();
    }

    @Override
    protected BDD calcEnvironmentTransition(Transition transition) {
        return graphGame_player1Edge(transition);
    }

    @Override
    protected BDD calcEnvironmentTransitions() {
        return graphGame_player1Edges();
    }

    @Override
    public boolean isEnvState(BDD state) {
        return !hasTop(state, PREDECESSOR);
    }

    @Override
    public boolean isBadState(BDD state) {
        return !this.graphGame_badVertices(PREDECESSOR).and(state).isZero();
    }

    @Override
    protected BDD calcBadDCSs() {
        return this.graphGame_badVertices(PREDECESSOR);
    }

    @Override
    public boolean isBufferState(BDD state) {
        return hasTop(state, PREDECESSOR);
    }

    /*
     * The global safety winning condition is a safety condition.
     * Thus only states with bad markings are special.
     */
    @Override
    public boolean isSpecialState(BDD state) {
        return false;
    }

    @Override
    protected BDD calcSpecialDCSs() {
        return this.getFactory().zero();
    }

    @Override
    protected BDD initial() {
        return graphGame_initialVertex(PREDECESSOR);
    }

    @Override
    protected BDD calcWinningDCSs(Map<Integer, BDD> distance) throws CalculationInterruptedException {
        return graphGame_winningVertices();
    }

    @Override
    protected BDD calcDCSs() throws CalculationInterruptedException {
        return this.graphGame_reachableVertices(this.graphGame_initialVertex(PREDECESSOR));
    }

    @Override
    public boolean hasFired(Transition t, BDD source, BDD target) {
        BDD edge = source.and(shiftFirst2Second(target));
        return !graphGame_player1Edge(t).and(edge).isZero();
    }

    /* </overwrites for inheritance> */

    /* <decode> */

    private String decodePlayer(byte[] dcs, int pos, int partition) {
        int id = decodeInteger(dcs, PLACES[pos][partition]);
        if (id == -1) {
            return "?";
        } else if (id == 0 && !this.getSolvingObject().isConcurrencyPreserving()) {
            return "-";
        } else {
            for (Place place : this.getSolvingObject().getDevidedPlaces()[partition]) {
                if (getGame().getID(place) == id) {
                    return place.getId();
                }
            }
            throw new IllegalStateException("place with id " + id + " in partition " + partition + " encountered");
            //return "!" + id;
        }
    }

    private Map<Integer, List<Transition>> decodeCommitment(byte[] dcs, int pos) {
        List<Transition> yes = new LinkedList<>();
        List<Transition> no = new LinkedList<>();
        List<Transition> undecided = new LinkedList<>();
        final List<Transition> transitions = this.getSolvingObject().getSystemTransitions();
        for (int transitionIndex = 0; transitionIndex < transitions.size(); transitionIndex++) {
            Transition transition = transitions.get(transitionIndex);
            switch (dcs[TRANSITIONS[pos].vars()[transitionIndex]]) {
                case TRUE -> yes.add(transition);
                case FALSE -> no.add(transition);
                case UNKNOWN -> undecided.add(transition);
            }
        }
        return Map.of(
                TRUE, Collections.unmodifiableList(yes),
                FALSE, Collections.unmodifiableList(no),
                UNKNOWN, Collections.unmodifiableList(undecided)
        );

    }

    private static String commitmentToVerboseString(Map<Integer, List<Transition>> commitment) {
        Function<List<Transition>, String> stringify = list -> "{" + list.stream().map(Transition::getId).collect(Collectors.joining(", ")) + "}";
        StringJoiner sj = new StringJoiner(" ");
        if (!commitment.get(TRUE).isEmpty()) {
            sj.add("+" + stringify.apply(commitment.get(TRUE)));
        }
        if (!commitment.get(UNKNOWN).isEmpty()) {
            sj.add("?" + stringify.apply(commitment.get(UNKNOWN)));
        }
        if (!commitment.get(FALSE).isEmpty()) {
            sj.add("-" + stringify.apply(commitment.get(FALSE)));
        }
        return sj.toString();
    }

    private static String commitmentToConciseString(Map<Integer, List<Transition>> commitment, Place systemPlace) {
        Function<List<Transition>, String> stringify = list -> "{" + list.stream()
                .filter(transition -> Optional.ofNullable(systemPlace).map(Place::getPostset).orElseGet(Collections::emptySet).contains(transition))
                .map(Transition::getId)
                .collect(Collectors.joining(", ")) + "}";
        StringJoiner sj = new StringJoiner(" ");
        if (!commitment.get(TRUE).isEmpty()) {
            sj.add("+" + stringify.apply(commitment.get(TRUE)));
        }
        if (!commitment.get(UNKNOWN).isEmpty()) {
            sj.add("?" + stringify.apply(commitment.get(UNKNOWN)));
        }
        return sj.toString();
    }

    private Map<Integer, Byte> decodeResponsibility(byte[] dcs, int pos) {
        return this.streamPartitions()
                .boxed()
                .collect(Collectors.toMap(Function.identity(), partition -> dcs[RESPONSIBILITY[pos].vars()[partition]]));
    }

    protected String decodeVertex(byte[] dcs, int pos) {
        boolean verbose = false;
        Map<Integer, Byte> responsibility = decodeResponsibility(dcs, pos);
        String systemPlayer = decodePlayer(dcs, pos, PARTITION_OF_SYSTEM_PLAYER);
        assert responsibility.get(PARTITION_OF_SYSTEM_PLAYER) == 1 ^ systemPlayer.equals("-") : "System player " + systemPlayer + " is not in responsibility set " + responsibility;
        String stringifiedEnvPlayerPlaces = IntStream.range(1, this.getSolvingObject().getMaxTokenCountInt())
                .mapToObj(partition -> {
                    StringBuilder sb = new StringBuilder();
                    String player = decodePlayer(dcs, pos, partition);
                    sb.append(player);
                    if (verbose || !player.equals("-")) {
                        sb.append(":").append(responsibility.get(partition));
                    }
                    return sb.toString();
                })
                .collect(Collectors.joining(", "));
        StringBuilder ret = new StringBuilder();
        ret.append("(s: ").append(systemPlayer);
        if (verbose) {
            ret.append(":").append(responsibility.get(PARTITION_OF_SYSTEM_PLAYER));
        }
        ret.append(" | e: ").append(stringifiedEnvPlayerPlaces).append(")");
        byte top = dcs[TOP[pos].vars()[0]];
        if (verbose) {
            ret.append("\nT:").append(top).append(" ").append(commitmentToVerboseString(decodeCommitment(dcs, pos)));
        } else {
            switch (top) {
                case TRUE -> ret.insert(0, "T ");
                case FALSE -> ret.append("\n").append(
                        systemPlayer.equals("?")
                                ? commitmentToVerboseString(decodeCommitment(dcs, pos))
                                : commitmentToConciseString(decodeCommitment(dcs, pos), systemPlayer.equals("-") ? null : getGame().getPlace(systemPlayer)));
                case UNKNOWN -> ret.append("\nT:? ").append(commitmentToVerboseString(decodeCommitment(dcs, pos)));
            }
        }
        return ret.toString();
    }

    protected String decodeEdge(byte[] dcs) {
        boolean hasPredecessor = !BDDTools.notUsedByBin(dcs, getDcs_length(), PREDECESSOR);
        boolean hasSuccessor = !BDDTools.notUsedByBin(dcs, getDcs_length(), SUCCESSOR);
        if (!hasPredecessor && !hasSuccessor) {
            return "(predecessor not defined) -> (successor not defined)";
        } else if (hasPredecessor && !hasSuccessor) {
            return decodeVertex(dcs, PREDECESSOR) + "\n-> (successor not defined)";
        } else if (!hasPredecessor) {
            return "(predecessor not defined) ->\n" + decodeVertex(dcs, SUCCESSOR);
        } else {
            return decodeVertex(dcs, PREDECESSOR) + "\n->\n" + decodeVertex(dcs, SUCCESSOR);
        }
    }

    protected int decodeInteger(byte[] dcs, BDDDomain domain) {
        int length = domain.vars().length;
        StringBuilder binary = new StringBuilder();
        for (int i = length - 1; i >= 0; i--) {
            byte bit = dcs[domain.vars()[i]];
            if (bit == UNKNOWN) {
                return -1;
            }
            binary.append(bit);
        }
        return Integer.parseInt(binary.toString(), 2);
    }

    /* </decode> */

}
