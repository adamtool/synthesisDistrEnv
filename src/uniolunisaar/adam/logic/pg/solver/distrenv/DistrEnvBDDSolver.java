package uniolunisaar.adam.logic.pg.solver.distrenv;

import uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;
import uniolunisaar.adam.logic.pg.solver.symbolic.bddapproach.BDDSolver;

/**
 *
 * @author Manuel Gieseking
 * @param <W>
 */
public abstract class DistrEnvBDDSolver<W extends Condition<W>> extends BDDSolver<W, DistrEnvBDDSolvingObject<W>, DistrEnvBDDSolverOptions> {

    protected DistrEnvBDDSolver(DistrEnvBDDSolvingObject<W> solvingObject, DistrEnvBDDSolverOptions opts) {
        super(solvingObject, opts);
    }

}
