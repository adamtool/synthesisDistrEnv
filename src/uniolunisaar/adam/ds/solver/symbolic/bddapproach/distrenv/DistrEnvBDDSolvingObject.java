package uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv;

import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.exceptions.pg.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.pg.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.solver.SolvingObject;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.tools.Logger;

/**
 * This class serves for storing the input of the solving algorithm, i.e., the
 * model (the Petri game) and the specification (the winning condition).
 *
 * @author Manuel Gieseking
 * @param <W>
 */
public class DistrEnvBDDSolvingObject<W extends Condition<W>> extends SolvingObject<PetriGame, W, DistrEnvBDDSolvingObject<W>> {

    public DistrEnvBDDSolvingObject(PetriGame game, W winCon) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException {
        this(game, winCon, false);
    }

    public DistrEnvBDDSolvingObject(PetriGame game, W winCon, boolean skipChecks) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException {
        super(game, winCon);

        if (!skipChecks) {
            checkPrecondition(game);
        } else {
            Logger.getInstance().addMessage("Attention: You decided to skip the tests. We cannot ensure that the net"
                    + " belongs to the class of solvable Petri games!", false);
        }
    }

    public DistrEnvBDDSolvingObject(DistrEnvBDDSolvingObject<W> obj) {
        super(new PetriGame(obj.getGame()), obj.getWinCon().getCopy());
    }

    private void checkPrecondition(PetriGame game) throws NetNotSafeException {
        if (!game.getBounded().isSafe()) {
            throw new NetNotSafeException(game.getBounded().unboundedPlace.toString(), game.getBounded().sequence.toString());
        }
    }

    @Override
    public DistrEnvBDDSolvingObject<W> getCopy() {
        return new DistrEnvBDDSolvingObject<>(this);
    }
}
