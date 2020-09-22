package uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.safety;

import java.util.Map;
import net.sf.javabdd.BDD;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.objectives.global.GlobalSafety;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;
import uniolunisaar.adam.exceptions.pnwt.CalculationInterruptedException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.InvalidPartitionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolver;

public class DistrEnvBDDGlobalSafetySolver extends DistrEnvBDDSolver<GlobalSafety> {

    DistrEnvBDDGlobalSafetySolver(DistrEnvBDDSolvingObject<GlobalSafety> obj, DistrEnvBDDSolverOptions opts) throws NotSupportedGameException, NetNotSafeException, NoSuitableDistributionFoundException, InvalidPartitionException {
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
    protected BDD calcBadDCSs() {
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

    /*
     * The global safety winning condition is a safety condition.
     * Thus only states with bad markings are special.
     *
     * TODO in all other safety solvers this this will return false/zero. Explain why.
     */
    @Override
    public boolean isSpecialState(BDD state) {
        return false;
    }

    @Override
    protected BDD calcSpecialDCSs() {
        return getFactory().zero();
    }

}
