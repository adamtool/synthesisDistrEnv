package uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv;

import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.objectives.Buchi;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.objectives.Reachability;
import uniolunisaar.adam.ds.objectives.Safety;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;
import uniolunisaar.adam.exceptions.synthesis.pgwt.InvalidPartitionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.logic.synthesis.solver.LLSolverFactory;
import uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv.safety.DistrEnvBDDSafetySolverFactory;

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
    protected <W extends Condition<W>> DistrEnvBDDSolvingObject<W> createSolvingObject(PetriGameWithTransits game, W winCon) throws NotSupportedGameException {
        try {
            return new DistrEnvBDDSolvingObject<>(game, winCon);
        } catch (NetNotSafeException | NoSuitableDistributionFoundException | InvalidPartitionException ex) {
            throw new NotSupportedGameException("Could not create solving object.", ex);
        }
    }

    @Override
    protected DistrEnvBDDSolver<Safety> getESafetySolver(PetriGameWithTransits game, Safety con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected DistrEnvBDDSolver<Safety> getASafetySolver(PetriGameWithTransits game, Safety con, DistrEnvBDDSolverOptions options) throws SolvingException, NotSupportedGameException, NoSuitableDistributionFoundException, InvalidPartitionException {
        try {
            return DistrEnvBDDSafetySolverFactory.getInstance().createDistrEnvBDDASafetySolver(createSolvingObject(game, con), options);
        } catch (NetNotSafeException ex) {
            throw new NotSupportedGameException(ex);
        }
    }

    @Override
    protected DistrEnvBDDSolver<Reachability> getEReachabilitySolver(PetriGameWithTransits game, Reachability con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected DistrEnvBDDSolver<Reachability> getAReachabilitySolver(PetriGameWithTransits game, Reachability con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected DistrEnvBDDSolver<Buchi> getEBuchiSolver(PetriGameWithTransits game, Buchi con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected DistrEnvBDDSolver<Buchi> getABuchiSolver(PetriGameWithTransits game, Buchi con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
