package uniolunisaar.adam.logic.synthesis.builder.symbolic.bddapproach.distrenv;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.PowerSet;
import uniolunisaar.adam.ds.graph.synthesis.twoplayergame.symbolic.bddapproach.BDDGraph;
import uniolunisaar.adam.ds.graph.synthesis.twoplayergame.symbolic.bddapproach.BDDState;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.Multiset;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.safety.DistrEnvBDDGlobalSafetySolver;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.symbolic.bddapproach.BDDTools;

/**
 * Creates a petri game strategy from a graph game strategy
 * for one system and many environment players.
 * <p>
 * A petri game strategy is a subnet of the unfolding of the original game.
 * Often they are infinite, meaning they cannot be expressed graphically.
 * The construction of the possibly infinite petri game strategy
 * is described in
 * Synthesis in Distributed Environments
 * by Bernd Finkbeiner and Paul Gölz
 * on pages 9 and 10 right after theorem 2 is stated.
 * <p>
 * To create a finite game with the same behaviour as the strategy
 * we let some transitions lead back to earlier places.
 * More precisely if a vertex P in the graph game strategy
 * leeds back to a vertex S that was already visited,
 * a set of new petri game strategy transition is created;
 * one for ever pair of cuts p and s associated with P and S
 * unless there exists already another transition with the desired behaviour.
 * Each such transition is enabled by p and when fired reaches s.
 * This way the petri game strategy is again in a marking
 * for which the successor behavior is well defined.
 * <p>
 * After calling {@link #build()} one call the homomorphism lambda
 * ({@link #lambda(Place)},
 * {@link #lambda(Transition)},
 * {@link #lambda(Marking)},
 * {@link #lambda(Collection)})
 * to find out what places and transition of the petri game strategy
 * correspond to what places and transitions of the petri game.
 * This information is also encoded in the
 * {@link PetriNetExtensionHandler#getOrigID(uniol.apt.adt.pn.Node) origID}
 * extension of the nodes.
 *
 * @author Lukas Panneke
 */
public class DistrEnvBDDGlobalSafetyPetriGameStrategyBuilder {

    private static final String DELIMITER = "_";

    private final DistrEnvBDDGlobalSafetySolver solver;
    private final PetriGameWithTransits petriStrategy;
    private final PetriGameWithTransits game;
    private final BDDGraph graphStrategy;

    /* from place in game to the first unused cardinal for places in the strategy */
    private final Map<Place, Integer> nextPlaceNumber = new HashMap<>();
    /* from place in strategy to place in game */
    private final Map<Place, Place> lambdaPlaces = new LinkedHashMap<>();
    /* from transition in game to the first unused cardinal for transitions in the strategy */
    private final Map<Transition, Integer> nextTransitionNumber = new HashMap<>();
    /* from transition in strategy to transition in game */
    private final Map<Transition, Transition> lambdaTransitions = new LinkedHashMap<>();
    /* from vertex in graph strategy to set of cuts in petri strategy */
    private final Map<BDDState, Set<Set<Place>>> cuts = new LinkedHashMap<>();

    public DistrEnvBDDGlobalSafetyPetriGameStrategyBuilder(DistrEnvBDDGlobalSafetySolver solver, BDDGraph graphStrategy) {
        this.solver = solver;
        this.graphStrategy = graphStrategy;
        this.game = this.solver.getGame();
        this.petriStrategy = new PetriGameWithTransits("Winning strategy of the system players of the game '" + solver.getGame().getName() + "'.");

        if (!solver.getGame().getBounded().isSafe()) {
            Logger.getInstance().addWarning("The petri game strategy for a unsafe petri game may be incorrect!");
        }
    }

    public PetriGameWithTransits build() {
        Set<BDDState> visited = new LinkedHashSet<>();
        visited.add(this.graphStrategy.getInitial());
        Queue<Edge> todo = new LinkedList<>();
        queueSuccessors(todo, this.graphStrategy.getInitial());

        this.cuts.put(this.graphStrategy.getInitial(), new HashSet<>(Set.of(this.initialCut())));

        while (!todo.isEmpty()) {
            Edge edge = todo.poll();
            if (!visited.contains(edge.getSuccessor())) {
                onNewVertex(edge.getPredecessor(), edge.getSuccessor());
                visited.add(edge.getSuccessor());
                queueSuccessors(todo, edge.getSuccessor());
            } else {
                onVisitedVertex(edge.getPredecessor(), edge.getSuccessor());
            }
        }
        return this.petriStrategy;
    }

    private void onVisitedVertex(BDDState predecessor, BDDState successor) {
        for (Transition transitionInGame : this.game.getTransitions()) {
            if (!this.solver.hasFired(transitionInGame, predecessor.getState(), successor.getState())) {
                continue;
            }
            Set<Place> presetInGame = transitionInGame.getPreset();
            Set<Place> postsetInGame = transitionInGame.getPostset();
            for (Set<Place> preCut : this.cuts.get(predecessor)) {
                for (Set<Place> postCut : this.cuts.get(successor)) {
                    if (findExistingStrategyTransition(transitionInGame, preCut, postCut).isPresent()) {
                        continue;
                    }
                    Transition transitionInStrategy = newStrategyTransition(transitionInGame);
                    for (Place prePlaceInStrategy : preCut) {
                        if (presetInGame.contains(lambda(prePlaceInStrategy))) {
                            this.petriStrategy.createFlow(prePlaceInStrategy, transitionInStrategy);
                        } else if (!postCut.contains(prePlaceInStrategy) && lambda(postCut).contains(lambda(prePlaceInStrategy))) {
                            Flow flow = this.petriStrategy.createFlow(prePlaceInStrategy, transitionInStrategy);
                            this.petriStrategy.setSpecial(flow);
                        }
                    }
                    for (Place postPlaceInStrategy : postCut) {
                        if (postsetInGame.contains(lambda(postPlaceInStrategy))
                                || (!preCut.contains(postPlaceInStrategy) && lambda(preCut).contains(lambda(postPlaceInStrategy)))) {
                            Flow flow = this.petriStrategy.createFlow(transitionInStrategy, postPlaceInStrategy);
                            this.petriStrategy.setSpecial(flow);
                        }
                    }
                    this.cuts.computeIfAbsent(successor, state -> new HashSet<>()).add(postCut);
                }
            }
        }
    }

    private Optional<Transition> findExistingStrategyTransition(Transition transitionInGame, Set<Place> fromInStrategy, Set<Place> toInStrategy) {
        Set<Place> fromReducedToTransition = new HashSet<>(),
                fromUntouchedByTransition = new HashSet<>(),
                toReducedToTransition = new HashSet<>(),
                toUntouchedByTransition = new HashSet<>();
        Set<Place> presetInGame = transitionInGame.getPreset();
        for (Place placeInStrategy : fromInStrategy) {
            if (presetInGame.contains(lambda(placeInStrategy))) {
                fromReducedToTransition.add(placeInStrategy);
            } else {
                fromUntouchedByTransition.add(placeInStrategy);
            }
        }
        Set<Place> postsetInGame = transitionInGame.getPostset();
        for (Place placeInStrategy : toInStrategy) {
            if (postsetInGame.contains(lambda(placeInStrategy))) {
                toReducedToTransition.add(placeInStrategy);
            } else {
                toUntouchedByTransition.add(placeInStrategy);
            }
        }
        return this.petriStrategy.getTransitions()
                .stream()
                .filter(t -> fromUntouchedByTransition.equals(toUntouchedByTransition))
                .filter(t -> t.getPreset().equals(fromReducedToTransition))
                .filter(t -> t.getPostset().equals(toReducedToTransition))
                .findAny();
    }

    private void onNewVertex(BDDState predecessor, BDDState successor) {
        if (this.solver.isBufferState(predecessor.getState())) {
            /*
             * (E’1): Do not modify the petri game strategy
             * and map the new node to the same cuts as its parent.
             */
            this.cuts.put(successor, this.cuts.get(predecessor));
        } else {
            for (Transition transitionInGame : this.game.getTransitions()) {
                if (!this.solver.hasFired(transitionInGame, predecessor.getState(), successor.getState())) {
                    continue;
                }
                for (Set<Place> cut : this.cuts.get(predecessor)) {
                    boolean foundSuitableSubCut = false;
                    for (Collection<Place> subCutList : PowerSet.powerSet(cut)) {
                        /*
                         * The power set iterator uses array lists.
                         * Lists may never equal sets...
                         */
                        Set<Place> subCut = Set.copyOf(subCutList);
                        if (!lambda(subCut).equals(transitionInGame.getPreset())) {
                            continue;
                        }
                        foundSuitableSubCut = true;
                        Transition transitionInStrategy = this.petriStrategy.getTransitions().stream()
                                .filter(t -> subCut.equals(t.getPreset()) && lambda(t).equals(transitionInGame))
                                .findAny()
                                .orElseGet(() -> {
                                    Transition t = newStrategyTransition(transitionInGame);
                                    //System.out.println(t.getId() + " was created for " + bddStateToString(predecessor) + " -> " + bddStateToString(successor) + ", " + transitionInGame.getId() + ", " + nodesToString(cut) + ", " + nodesToString(subCut));
                                    for (Place prePlaceInStrategy : subCut) {
                                        this.petriStrategy.createFlow(prePlaceInStrategy, t);
                                    }
                                    for (Place postPlaceInGame : transitionInGame.getPostset()) {
                                        this.petriStrategy.createFlow(t, newStrategyPlace(postPlaceInGame));
                                    }
                                    return t;
                                });
                        Set<Place> successorCut = fire(cut, transitionInStrategy);
                        this.cuts.computeIfAbsent(successor, state -> new HashSet<>()).add(successorCut);
                    }
                    /*
                     * such a sub cut always exists,
                     * because the transition is enabled in lambda(cut)
                     */
                    assert foundSuitableSubCut;
                }
            }
        }
    }

    public Place lambda(Place placeInStrategy) {
        return this.lambdaPlaces.get(placeInStrategy);
    }

    public Multiset<Place> lambda(Collection<Place> placesInStrategy) {
        return placesInStrategy.stream()
                .map(this::lambda)
                .collect(Multiset.collect());
    }

    public Transition lambda(Transition placeInStrategy) {
        return this.lambdaTransitions.get(placeInStrategy);
    }

    public Marking lambda(Marking marking) {
        return streamMarking(marking)
                .map(this::lambda)
                .collect(collectMarking(this.game));
    }

    private Set<Place> initialCut() {
        Marking initialMarking = this.game.getInitialMarking();
        return streamMarking(initialMarking)
                .map(this::newStrategyPlace)
                .peek(placeInStrategy -> placeInStrategy.setInitialToken(1))
                .collect(Collectors.toSet());
    }

    private void queueSuccessors(Queue<Edge> queue, BDDState vertex) {
        this.graphStrategy.getPostset(vertex.getId()).stream()
                .map(successor -> new Edge(vertex, successor))
                .forEach(queue::add);
    }

    private Place newStrategyPlace(Place placeInGame) {
        Integer newNumber = this.nextPlaceNumber.compute(placeInGame, (place, integer) -> integer == null ? 1 : integer + 1);
        Place newPlaceInStrategy = this.petriStrategy.createPlace(placeInGame.getId() + DELIMITER + newNumber);
        this.lambdaPlaces.put(newPlaceInStrategy, placeInGame);
        this.petriStrategy.setOrigID(newPlaceInStrategy, placeInGame.getId());
        newPlaceInStrategy.copyExtensions(placeInGame);
        PetriNetExtensionHandler.setLabel(newPlaceInStrategy, placeInGame.getId());
        if (this.game.isSystem(placeInGame)) {
            this.petriStrategy.setSystem(newPlaceInStrategy);
        }
        PetriNetExtensionHandler.setOrigID(newPlaceInStrategy, placeInGame.getId());
        PetriNetExtensionHandler.clearCoords(newPlaceInStrategy);
        return newPlaceInStrategy;
    }

    private Transition newStrategyTransition(Transition transitionInGame) {
        Integer newNumber = this.nextTransitionNumber.compute(transitionInGame, (place, integer) -> integer == null ? 1 : integer + 1);
        Transition newTransitionInStrategy = this.petriStrategy.createTransition(transitionInGame.getId() + DELIMITER + newNumber);
        this.lambdaTransitions.put(newTransitionInStrategy, transitionInGame);
        newTransitionInStrategy.copyExtensions(transitionInGame);
        newTransitionInStrategy.setLabel(transitionInGame.getLabel());
        PetriNetExtensionHandler.setOrigID(newTransitionInStrategy, transitionInGame.getId());
        return newTransitionInStrategy;
    }

    private Set<Place> fire(Set<Place> preCutInStrategy, Transition transitionInStrategy) {
        Marking preMarking = preCutInStrategy.stream().collect(collectMarking(this.petriStrategy));
        Marking postMarking = preMarking.fireTransitions(transitionInStrategy);
        return streamMarking(postMarking).collect(Collectors.toSet());
    }

    private static Stream<Place> streamMarking(Marking marking) {
        return marking.getNet().getPlaces().stream()
                .peek(place -> {
                    assert !marking.getToken(place).isOmega();
                })
                .flatMap(place -> LongStream.rangeClosed(1, marking.getToken(place).getValue()).mapToObj(i -> place));
    }

    private static Collector<Place, ?, Marking> collectMarking(PetriNet net) {
        return Collectors.collectingAndThen(
                Collectors.toMap(Node::getId, (Place place) -> 1, Integer::sum),
                (Map<String, Integer> map) -> new Marking(net, map));
    }

    private String cutsToString() {
        return this.cuts.entrySet().stream()
                .map(entry -> bddStateToString(entry.getKey()) + " -> " + entry.getValue()).collect(Collectors.joining(", ", "{", "}"));
    }

    private String bddStateToString(BDDState state) {
        return BDDTools.getDecodedDecisionSets(state.getState(), this.solver).replace("-> (successor not defined)", "").replace("\n", "");
    }

    private String nodesToString(Collection<? extends Node> nodes) {
        return nodes.stream().map(Node::getId).collect(Collectors.joining(", ", "{", "}"));
    }

    private class Edge {

        private final BDDState predecessor;
        private final BDDState successor;

        public Edge(BDDState predecessor, BDDState successor) {
            this.predecessor = predecessor;
            this.successor = successor;
        }

        public BDDState getPredecessor() {
            return this.predecessor;
        }

        public BDDState getSuccessor() {
            return this.successor;
        }

        @Override
        public String toString() {
            return "Edge{" + "\n"
                    + "    " + bddStateToString(this.predecessor) + "\n"
                    + " -> " + bddStateToString(this.successor) + "\n"
                    + '}';
        }
    }
}
