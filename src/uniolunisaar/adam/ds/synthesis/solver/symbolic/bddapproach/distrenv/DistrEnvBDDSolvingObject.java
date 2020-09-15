package uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv;

import uniol.apt.adt.pn.Place;
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

    @Override
    public DistrEnvBDDSolvingObject<W> getCopy() {
        return new DistrEnvBDDSolvingObject<>(this);
    }

}
