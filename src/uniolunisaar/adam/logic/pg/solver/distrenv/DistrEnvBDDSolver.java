package uniolunisaar.adam.logic.pg.solver.distrenv;

import uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.exceptions.pg.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.pg.SolverDontFitPetriGameException;
import uniolunisaar.adam.exceptions.pg.NotSupportedGameException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.solver.Solver;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;

/**
 *
 * @author Manuel Gieseking
 * @param <W>
 */
public abstract class DistrEnvBDDSolver<W extends Condition<W>> extends Solver<PetriGame, W, DistrEnvBDDSolvingObject<W>, DistrEnvBDDSolverOptions> {

    /**
     * Creates a new solver for the given game.
     *
     * @throws SolverDontFitPetriGameException - thrown if the created solver
     * don't fit the given winning objective specified in the given game.
     */
    DistrEnvBDDSolver(DistrEnvBDDSolvingObject<W> solvingObject, DistrEnvBDDSolverOptions opts) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException {
        super(solvingObject, opts);
    }

}
