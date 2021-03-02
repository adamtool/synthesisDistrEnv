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
import uniol.apt.adt.Node;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.objectives.global.GlobalSafety;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;
import uniolunisaar.adam.exceptions.pnwt.CalculationInterruptedException;
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
 * in Figure 2: Graph(G).
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
        for (int pos : List.of(PREDECESSOR, SUCCESSOR)) {
            for (int partition = 0; partition < numberOfPartitions; partition++) {
                PLACES[pos][partition] = this.getFactory().extDomain(this.getSolvingObject().getDevidedPlaces()[partition].size());

                /* for every system transition the system player must choose whether or not to allow that transition. */
                TRANSITIONS[pos] = this.getFactory().extDomain(BigInteger.TWO.pow(this.getSolvingObject().getSystemTransitions().size()));
                TOP[pos] = this.getFactory().extDomain(2);
            }
        }
        setDCSLength(getFactory().varNum() / 2); // TODO what is this?
    }

    @Override
    protected BDD getVariables(int pos) {
        return TOP[pos].set()
                .and(TRANSITIONS[pos].set())
                .and(Arrays.stream(PLACES[pos])
                        .map(BDDDomain::set)
                        .collect(and()));
    }

    @Override
    protected BDD preBimpSucc() {
        return TOP[PREDECESSOR].buildEquals(TOP[SUCCESSOR])
                .and(TRANSITIONS[PREDECESSOR].buildEquals(TRANSITIONS[SUCCESSOR]))
                .and(this.streamPartitions()
                        .mapToObj(partition -> PLACES[PREDECESSOR][partition].buildEquals(PLACES[SUCCESSOR][partition]))
                        .collect(and()));
    }

    /* </variables> */

    private BDD codeSystemTransition(Transition transition, int pos) {
        int transitionIndex = this.getSolvingObject().getSystemTransitions().indexOf(transition);
        return this.getFactory().ithVar(TRANSITIONS[pos].vars()[transitionIndex]);
    }

    protected IntStream streamPartitions() {
        return IntStream.range(0, this.getSolvingObject().getMaxTokenCountInt());
    }

    protected BDD graphGame_initialVertex(int pos) {
        return codeMarking(this.getGame().getInitialMarking(), pos)
                /* the system must choose it's initial commitment set. */
                .andWith(top(pos))
                .andWith(nothingChosen(pos));
    }

    /**
     * Encode an edge coming out of a player 0 vertex in the graph game.
     * That edge corresponds to a petri game transition
     * with the system player in it's preset.
     * It is not responsible for firing the transition,
     * so the marking remains unchanged.
     * The only thing happening is player 0 choosing a commitment set.
     * <p>
     * These are edges where the successor vertices are those marked with TOP.
     * <p>
     * In the paper these are the edges of type E1.
     */
    protected BDD graphGame_player0Edge(Place enteredSystemPlace) {
        BDD ret = getOne();

        /*
         * this only encodes the edge coming from the player 0 vertex.
         * the transition is fired by player 1,
         * who then sets TOP to inform the player
         * that they have to choose a new commitment set.
         * that means the marking changes in player 1's vertex, not here.
         */
        ret.andWith(markingsEqual()).andWith(onlyExistingPlacesInMarking(PREDECESSOR));

        /* the purpose of this transition is to remove the top. */
        ret.andWith(top(PREDECESSOR)).andWith(notTop(SUCCESSOR));

        /*
         * only transitions in the postset of the current system place can be chosen.
         */
        ret.andWith(onlyChooseTransitionsInPostsetOfSystemPlace(SUCCESSOR));

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
        BDD ret = getOne();
        ret.andWith(notTop(PREDECESSOR)).andWith(notTop(SUCCESSOR));
        ret.andWith(commitmentsEqual());
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
                .map(marking -> codeMarking(marking, pos))
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
        BDD noEnvironmentEnabled = this.getSolvingObject().getPurelyEnvironmentalTransitions().stream()
                .map(transition -> this.enabled(transition, pos).not())
                .collect(and());
        BDD someSystemEnabled = this.getSolvingObject().getSystemTransitions().stream()
                .map(transition -> this.enabled(transition, pos))
                .collect(or());
        BDD noSystemChosen = this.getSolvingObject().getSystemTransitions().stream()
                .map(transition -> this.enabled(transition, pos).andWith(this.chosen(transition, pos)).not())
                .collect(and());
        return notTop(pos).andWith(noEnvironmentEnabled).andWith(someSystemEnabled).andWith(noSystemChosen);
    }

    protected BDD graphGame_player0Edges() {
        return this.getSolvingObject().getSystemPlaces().stream()
                .map(this::graphGame_player0Edge)
                .collect(or());
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
        BDD edges = graphGame_Edges();
        return fixpoint(getZero(), startVertex, Q -> Q.or(getSuccs(Q.and(edges))), null);
    }

    protected BDD graphGame_reachableEdges(BDD startVertex) throws CalculationInterruptedException {
        //BDD vertices = graphGame_reachableVertices(startVertex);
        //return vertices.and(shiftFirst2Second(vertices));

        BDD edges = graphGame_Edges();
        return fixpoint(getZero(), startVertex.and(edges), Q -> Q.or(getSuccs(Q).and(edges)), null);
    }

    protected BDD graphGame_poisonedVertices(BDD reachableEdges, BDD badVertices) throws CalculationInterruptedException {
        BDD player0Edges = graphGame_player0Edges().and(reachableEdges);
        BDD player1Edges = graphGame_player1Edges().and(reachableEdges);

        return fixpoint(getZero(), badVertices, Q -> Q.or(transmitPoison(Q, player0Edges, player1Edges)), null).and(wellformed());
    }

    protected BDD graphGame_winningVertices() throws CalculationInterruptedException {
        BDD reachableEdges = graphGame_reachableEdges(graphGame_initialVertex(PREDECESSOR));
        BDD poisonedVertices = graphGame_poisonedVertices(reachableEdges, graphGame_badVertices(PREDECESSOR));
        BDD reachableVertices = graphGame_reachableVertices(graphGame_initialVertex(PREDECESSOR));
        //BDD reachableVertices = getSuccs(reachableEdges);
        return reachableVertices.and(poisonedVertices.not().andWith(wellformed()));
    }

    protected BDD fire(Transition transition) {
        Set<Place> pre = transition.getPreset();
        return enabled(transition, PREDECESSOR).andWith(this.streamPartitions()
                .mapToObj(partition -> this.getSolvingObject().getDevidedPlaces()[partition].stream()
                        .map(place -> {
                            Place successorPlace = pre.contains(place) ? getSuitableSuccessor(place, transition) : place;
                            return codePlace(place, PREDECESSOR, partition)
                                    .andWith(codePlace(successorPlace, SUCCESSOR, partition));
                        })
                        .collect(or()))
                .collect(and()));
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
        return this.getSolvingObject().getSystemPlaces().stream()
                .map(place -> onlyChooseTransitionsInPostsetOfSystemPlace(place, pos))
                .collect(and());
    }

    protected BDD onlyExistingPlacesInMarking(int pos) {
        return this.streamPartitions()
                .mapToObj(partition -> this.getSolvingObject().getDevidedPlaces()[partition].stream()
                        .map(place -> codePlace(place, pos, partition))
                        .collect(or()))
                .collect(and());
    }

    protected BDD markingsEqual() {
        return this.streamPartitions()
                .mapToObj(partition -> PLACES[PREDECESSOR][partition].buildEquals(PLACES[SUCCESSOR][partition]))
                .collect(and());
    }

    protected BDD commitmentsEqual() {
        return TRANSITIONS[PREDECESSOR].buildEquals(TRANSITIONS[SUCCESSOR]);
    }

    protected BDD nothingChosen(int pos) {
        return TRANSITIONS[pos].ithVar(0);
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

    protected BDD codeMarking(Marking marking, int pos) {
        return this.getGame().getPlaces().stream()
                .filter(place -> marking.getToken(place).getValue() > 0)
                .map(place -> codePlace(place, pos, this.getGame().getPartition(place)))
                .collect(and());
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

    private Optional<Place> decodePlayer(byte[] dcs, int pos, int partition) {
        int id = decodeInteger(dcs, PLACES[pos][partition]);
        if (id == -1) {
            return Optional.empty();
        }
        for (Place place : this.getSolvingObject().getDevidedPlaces()[partition]) {
            if (getGame().getID(place) == id) {
                return Optional.of(place);
            }
        }
        throw new IllegalStateException("place with id " + id + " in partition " + partition + " encountered");
        //return "!" + id;
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
                .filter(transition -> systemPlace.getPostset().contains(transition))
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

    protected String decodeVertex(byte[] dcs, int pos) {
        String stringifiedEnvPlayerPlaces = IntStream.range(1, this.getSolvingObject().getMaxTokenCountInt())
                .mapToObj(partition -> decodePlayer(dcs, pos, partition).map(Node::getId).orElse("?"))
                .collect(Collectors.joining(", "));
        Place systemPlayer = decodePlayer(dcs, pos, PARTITION_OF_SYSTEM_PLAYER)
                .orElseThrow(() -> new IllegalStateException("no system player"));
        StringBuilder ret = new StringBuilder();
        ret.append("(s: ").append(systemPlayer.getId());
        ret.append(" | e: ").append(stringifiedEnvPlayerPlaces).append(")");
        byte top = dcs[TOP[pos].vars()[0]];
        switch (top) {
            case TRUE -> ret.insert(0, "T ");
            case FALSE -> ret.append("\n").append(commitmentToConciseString(decodeCommitment(dcs, pos), systemPlayer));
            case UNKNOWN -> ret.append("\nT:? ").append(commitmentToVerboseString(decodeCommitment(dcs, pos)));
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
