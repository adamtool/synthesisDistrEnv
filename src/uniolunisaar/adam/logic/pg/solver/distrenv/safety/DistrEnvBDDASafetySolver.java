package uniolunisaar.adam.logic.pg.solver.distrenv.safety;

import java.util.Map;
import net.sf.javabdd.BDD;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.objectives.Safety;
import uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;
import uniolunisaar.adam.exceptions.pg.CalculationInterruptedException;
import uniolunisaar.adam.exceptions.pg.InvalidPartitionException;
import uniolunisaar.adam.exceptions.pg.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.pg.NotSupportedGameException;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.logic.pg.solver.distrenv.DistrEnvBDDSolver;

public class DistrEnvBDDASafetySolver extends DistrEnvBDDSolver<Safety> {

    DistrEnvBDDASafetySolver(DistrEnvBDDSolvingObject<Safety> obj, DistrEnvBDDSolverOptions opts) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, InvalidPartitionException {
        super(obj, opts);
    }

    @Override
    protected BDD calcSystemTransition(Transition t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected BDD calcEnvironmentTransition(Transition t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEnvState(BDD state) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isBadState(BDD state) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isSpecialState(BDD state) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isBufferState(BDD state) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected BDD calcWinningDCSs(Map<Integer, BDD> distance) throws CalculationInterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected BDD calcBadDCSs() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected BDD calcSpecialDCSs() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
