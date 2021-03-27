package uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.BDDSolvingObject;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.InvalidPartitionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.UnboundedPGException;
import uniolunisaar.adam.logic.synthesis.pgwt.calculators.CalculatorIDs;
import uniolunisaar.adam.logic.synthesis.pgwt.partitioning.Partitioner;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.PGTools;
import uniolunisaar.adam.util.PNTools;

/**
 * This class serves for storing the input of the solving algorithm, i.e., the
 * model (the Petri game) and the specification (the winning condition).
 *
 * @param <W>
 * @author Manuel Gieseking
 */
public class DistrEnvBDDSolvingObject<W extends Condition<W>> extends BDDSolvingObject<W> {

    private List<Transition> systemTransitions, envTransitions;
    private Map<Integer, Long> boundOfPartitions;

    public DistrEnvBDDSolvingObject(PetriGameWithTransits game, W winCon) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, InvalidPartitionException {
        this(game, winCon, false);
    }

    public DistrEnvBDDSolvingObject(PetriGameWithTransits game, W winCon, boolean skipChecks) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, InvalidPartitionException {
        super(game, winCon, skipChecks);
    }

    public DistrEnvBDDSolvingObject(DistrEnvBDDSolvingObject<W> obj) {
        super(obj);
    }

    @Override
    protected void checkPrecondition(PetriGameWithTransits game) throws NetNotSafeException, NotSupportedGameException {
        PNTools.addsBoundedness2Places(game);
        if (!game.getBounded().isBounded()) {
            throw new UnboundedPGException(game, game.getBounded().unboundedPlace);
        }
        PGTools.checkAtMostOneSysToken(game);
    }

    @Override
    protected void annotatePlacesWithPartitions(boolean skipChecks) throws InvalidPartitionException, NoSuitableDistributionFoundException {
        partitionPlaces(getGame(), skipChecks);
    }

    public static void partitionPlaces(PetriGameWithTransits game, boolean skipChecks) throws InvalidPartitionException, NoSuitableDistributionFoundException {
        if (game.getPlaces().stream().anyMatch(game::hasPartition)) {
            Logger.getInstance().addMessage("Using the annotated partition of places.");
        } else {
            Partitioner.doIt(game, false);
        }
        autoPartitionSystem(game);

        if (!skipChecks) {
            Logger.getInstance().addMessage("check partitioning ... ", true);
            PGTools.checkValidPartitioned(game);

            Map<Integer, Set<Place>> partitions = game.getPlaces().stream()
                    .collect(Collectors.groupingBy(game::getPartition, Collectors.toSet()));
            long tokenCount = game.getValue(CalculatorIDs.MAX_TOKEN_COUNT.name());
            if (tokenCount != partitions.size()) {
                throw new NoSuitableDistributionFoundException();
            }

            Set<Place> systemPlaces = game.getPlaces().stream()
                    .filter(game::isSystem)
                    .collect(Collectors.toSet());
            if (!partitions.get(0).equals(systemPlaces)) {
                boolean swapped = false;
                for (Map.Entry<Integer, Set<Place>> partition : partitions.entrySet()) {
                    if (partition.getValue().equals(systemPlaces)) {
                        swapPartitions(game, partition.getKey(), 0);
                        swapped = true;
                    }
                }
                if (!swapped) {
                    throw new NoSuitableDistributionFoundException("Partition 0 must be exactly the system places " + systemPlaces);
                }
            }
            Logger.getInstance().addMessage("... partitionning check done.");
        }
    }

    /**
     * if system places are not partitioned,
     * they automatically get assigned partition 0.
     * if partition 0 is already occupied by environment places,
     * they are first moved to another free index.
     */
    private static void autoPartitionSystem(PetriGameWithTransits game) throws InvalidPartitionException {
        Map<Integer, Set<Place>> partitions = game.getPlaces().stream()
                .collect(Collectors.groupingBy(place -> game.hasPartition(place) ? game.getPartition(place) : -1, Collectors.toSet()));
        if (!partitions.containsKey(-1) || partitions.get(-1).isEmpty()) {
            /* there are not un-partitioned places. nothing to do */
            return;
        }

        Set<Place> systemPlaces = game.getPlaces().stream()
                .filter(game::isSystem)
                .collect(Collectors.toSet());
        if (!systemPlaces.equals(partitions.get(-1))) {
            throw new InvalidPartitionException("there are un-partitioned environment places.");
        }

        if (partitions.containsKey(0) && !partitions.get(0).isEmpty()) {
            Logger.getInstance().addMessage("Moving partition of environment places at index 0 to another index.");
            int firstUnusedPartitionId = 1;
            while (partitions.containsKey(firstUnusedPartitionId)) {
                firstUnusedPartitionId++;
            }
            swapPartitions(game, 0, firstUnusedPartitionId);
            /* now there is space in partition 0 */
        }
        Logger.getInstance().addMessage("Annotating system places with partition 0.");
        partitions.get(-1).forEach(place -> game.setPartition(place, 0));
    }

    private static void swapPartitions(PetriGameWithTransits game, int a, int b) {
        for (Place place : game.getPlaces()) {
            if (game.hasPartition(place) && game.getPartition(place) == a) {
                game.setPartition(place, b);
            } else if (game.hasPartition(place) && game.getPartition(place) == b) {
                game.setPartition(place, a);
            }
        }
    }

    /**
     * All transitions in the petri game, which have no system place in their preset.
     */
    public List<Transition> getPurelyEnvironmentalTransitions() {
        if (this.envTransitions == null) {
            Set<Place> systemPlaces = this.getSystemPlaces();
            this.envTransitions = this.getGame().getTransitions().stream()
                    .filter(transition -> CollectionUtils.intersection(systemPlaces, transition.getPreset()).isEmpty())
                    .collect(Collectors.toUnmodifiableList());
        }
        return this.envTransitions;
    }

    /**
     * All transitions in the petri game, which have a system place in their preset.
     */
    public List<Transition> getSystemTransitions() {
        if (this.systemTransitions == null) {
            Set<Place> systemPlaces = this.getSystemPlaces();
            this.systemTransitions = this.getGame().getTransitions().stream()
                    .filter(transition -> !CollectionUtils.intersection(systemPlaces, transition.getPreset()).isEmpty())
                    .collect(Collectors.toUnmodifiableList());
        }
        return this.systemTransitions;
    }

    public Set<Place> getSystemPlaces() {
        return this.getDevidedPlaces()[0];
    }

    public Place getSystemPlaceOfTransition(Transition transition, boolean post) {
        if (!this.getSystemTransitions().contains(transition)) {
            throw new IllegalArgumentException(transition + " has no system place in it's preset. It's purely environmental. " + transition.getPreset());
        }
        Collection<Place> currentPlaceOfSystemPlayer =
                CollectionUtils.intersection(this.getSystemPlaces(), post ? transition.getPostset() : transition.getPreset());
        if (currentPlaceOfSystemPlayer.size() != 1) {
            throw new AssertionError("Every system transition must have exactly one system place in it's postset (for this solver)");
        }
        return currentPlaceOfSystemPlayer.iterator().next();
    }

    public long getBoundOfPartition(int partition) {
        if (this.boundOfPartitions == null) {
            this.boundOfPartitions = this.getGame().getPlaces().stream()
                    .collect(Collectors.toMap(this.getGame()::getPartition, PetriNetExtensionHandler::getBoundedness, BinaryOperator.maxBy(Long::compareTo)));
        }
        return this.boundOfPartitions.get(partition);
    }

    public Set<Marking> getBadMarkings() {
        return this.getGame().getFinalMarkings();
    }

    @Override
    public DistrEnvBDDSolvingObject<W> getCopy() {
        return new DistrEnvBDDSolvingObject<>(this);
    }
}
