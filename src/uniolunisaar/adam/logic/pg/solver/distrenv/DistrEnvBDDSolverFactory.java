package uniolunisaar.adam.logic.pg.solver.distrenv;

import uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.exceptions.pg.NotSupportedGameException;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.objectives.Buchi;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.objectives.Reachability;
import uniolunisaar.adam.ds.objectives.Safety;
import uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;
import uniolunisaar.adam.exceptions.pg.InvalidPartitionException;
import uniolunisaar.adam.exceptions.pg.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.logic.pg.solver.LLSolverFactory;
import uniolunisaar.adam.logic.pg.solver.distrenv.safety.DistrEnvBDDSafetySolverFactory;

/**
 * A factory creating BDD solvers for the case of one system and an arbitrary
 * number of environment players with a universal safety objective without
 * transits.
 *
 * @author Manuel Gieseking
 */
public class DistrEnvBDDSolverFactory extends LLSolverFactory<DistrEnvBDDSolverOptions, DistrEnvBDDSolver<? extends Condition<?>>> {

    private static DistrEnvBDDSolverFactory instance = null;

    public static DistrEnvBDDSolverFactory getInstance() {
        if (instance == null) {
            instance = new DistrEnvBDDSolverFactory();
        }
        return instance;
    }

    private DistrEnvBDDSolverFactory() {

    }

    @Override
    protected <W extends Condition<W>> DistrEnvBDDSolvingObject<W> createSolvingObject(PetriGame game, W winCon) throws NotSupportedGameException {
        try {
            return new DistrEnvBDDSolvingObject<>(game, winCon);
        } catch (NetNotSafeException | NoSuitableDistributionFoundException | InvalidPartitionException ex) {
            throw new NotSupportedGameException("Could not create solving object.", ex);
        }
    }

    @Override
    protected DistrEnvBDDSolver<Safety> getESafetySolver(PetriGame game, Safety con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected DistrEnvBDDSolver<Safety> getASafetySolver(PetriGame game, Safety con, DistrEnvBDDSolverOptions options) throws SolvingException, NotSupportedGameException, NoSuitableDistributionFoundException, InvalidPartitionException {
        try {
            return DistrEnvBDDSafetySolverFactory.getInstance().createDistrEnvBDDASafetySolver(createSolvingObject(game, con), options);
        } catch (NetNotSafeException ex) {
            throw new NotSupportedGameException(ex);
        }
    }

    @Override
    protected DistrEnvBDDSolver<Reachability> getEReachabilitySolver(PetriGame game, Reachability con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected DistrEnvBDDSolver<Reachability> getAReachabilitySolver(PetriGame game, Reachability con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected DistrEnvBDDSolver<Buchi> getEBuchiSolver(PetriGame game, Buchi con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected DistrEnvBDDSolver<Buchi> getABuchiSolver(PetriGame game, Buchi con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
