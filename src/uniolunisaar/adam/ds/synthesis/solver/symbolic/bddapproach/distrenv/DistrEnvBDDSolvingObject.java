package uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.BDDSolvingObject;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.InvalidPartitionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
import uniolunisaar.adam.logic.synthesis.pgwt.calculators.CalculatorIDs;
import uniolunisaar.adam.logic.synthesis.pgwt.partitioning.Partitioner;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.PGTools;

/**
 * This class serves for storing the input of the solving algorithm, i.e., the
 * model (the Petri game) and the specification (the winning condition).
 *
 * @param <W>
 * @author Manuel Gieseking
 */
public class DistrEnvBDDSolvingObject<W extends Condition<W>> extends BDDSolvingObject<W> {

    private List<Transition> systemTransitions, envTransitions;

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
        if (!game.getBounded().isSafe()) {
            throw new NetNotSafeException(game.getBounded().unboundedPlace.toString(), game.getBounded().sequence.toString());
        }
        PGTools.checkOnlyOneSysToken(game);
    }

    @Override
    protected void annotatePlacesWithPartitions(boolean skipChecks) throws InvalidPartitionException, NoSuitableDistributionFoundException {
        if (Partitioner.hasDistribution(getGame(), false)) {
            Logger.getInstance().addMessage("Using the annotated partition of places.");
        } else {
            // todo: could add here algorithms to automatically partition the places
            long tokencount = getGame().getValue(CalculatorIDs.MAX_TOKEN_COUNT.name());
            throw new NoSuitableDistributionFoundException(tokencount);
        }
        // to simplify the annotations for the user (system player automatically gets 0)
        for (Place p : getGame().getPlaces()) {
            if (getGame().isSystem(p)) {
                getGame().setPartition(p, 0);
            }
        }
        if (!skipChecks) {
            Logger.getInstance().addMessage("check partitioning ... ", true);
            PGTools.checkValidPartitioned(getGame());
            Logger.getInstance().addMessage("... partitionning check done.");
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

    public Set<Transition> getTransitionsEnabledByPlace(Place place) {
        return this.getGame().getTransitions().stream()
                .filter(transition -> transition.getPreset().contains(place))
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<Marking> getBadMarkings() {
        return this.getGame().getFinalMarkings();
    }

    @Override
    public DistrEnvBDDSolvingObject<W> getCopy() {
        return new DistrEnvBDDSolvingObject<>(this);
    }
}
