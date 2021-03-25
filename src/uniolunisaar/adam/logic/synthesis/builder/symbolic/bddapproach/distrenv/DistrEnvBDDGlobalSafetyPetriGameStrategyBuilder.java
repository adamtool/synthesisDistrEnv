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
import java.util.stream.Collectors;
import uniol.apt.adt.Node;
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
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.safety.DistrEnvBDDGlobalSafetySolver;
import uniolunisaar.adam.util.symbolic.bddapproach.BDDTools;

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
                        if (postsetInGame.contains(lambda(postPlaceInStrategy))) {
                            this.petriStrategy.createFlow(transitionInStrategy, postPlaceInStrategy);
                        } else if (!preCut.contains(postPlaceInStrategy) && lambda(preCut).contains(lambda(postPlaceInStrategy))) {
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
                                    //System.out.println(t.getId() + " was created for " + bddStateToString(predecessor) + " -> " + bddStateToString(successor) + ", " + transitionInGame + ", " + cut + ", " + subCut);
                                    for (Place prePlaceInStrategy : subCut) {
                                        this.petriStrategy.createFlow(prePlaceInStrategy, t);
                                    }
                                    for (Place postPlaceInGame : transitionInGame.getPostset()) {
                                        this.petriStrategy.createFlow(t, newStrategyPlace(postPlaceInGame));
                                    }
                                    return t;
                                });
                        Set<Place> successorCut = markingToSet(setToMarking(this.petriStrategy, cut).fireTransitions(transitionInStrategy));
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

    public Set<Place> lambda(Collection<Place> placesInStrategy) {
        return placesInStrategy.stream()
                .map(this::lambda)
                .collect(Collectors.toSet());
    }

    public Transition lambda(Transition placeInStrategy) {
        return this.lambdaTransitions.get(placeInStrategy);
    }

    public Marking lambda(Marking marking) {
        return setToMarking(this.game, lambda(markingToSet(marking)));
    }

    private Set<Place> initialCut() {
        Marking initialMarking = this.game.getInitialMarking();
        return markingToSet(initialMarking).stream()
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

    public Set<Place> markingToSet(Marking marking) {
        return marking.getNet().getPlaces().stream()
                .filter(place -> marking.getToken(place).getValue() >= 1)
                .collect(Collectors.toSet());
    }

    public Marking setToMarking(PetriNet net, Set<Place> places) {
        return new Marking(net, places.stream().collect(Collectors.toMap(Node::getId, place -> 1)));
    }

    private String cutsToString() {
        return this.cuts.entrySet().stream()
                .map(entry -> bddStateToString(entry.getKey()) + " -> " + entry.getValue()).collect(Collectors.joining(", ", "{", "}"));
    }

    private String bddStateToString(BDDState state) {
        return BDDTools.getDecodedDecisionSets(state.getState(), this.solver).replace("-> (successor not defined)", "").replace("\n", "");
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
