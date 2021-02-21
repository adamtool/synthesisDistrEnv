package uniolunisaar.adam.logic.synthesis.solver.symbolic.bddapproach.distrenv;

import java.util.Map;
import uniol.apt.adt.pn.Marking;
import uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.objectives.local.Buchi;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.objectives.global.GlobalSafety;
import uniolunisaar.adam.ds.objectives.local.Reachability;
import uniolunisaar.adam.ds.objectives.local.Safety;
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
    public DistrEnvBDDSolver<? extends Condition<?>> getSolver(PetriGameWithTransits game, Condition.Objective winCon, DistrEnvBDDSolverOptions options) throws SolvingException {
        switch (winCon) {
            case E_SAFETY: {
                return getESafetySolver(game, new Safety(true), options);
            }
            case A_SAFETY: {
                return getASafetySolver(game, new Safety(false), options);
            }
            case E_REACHABILITY: {
//                SolvingObject<G, Reachability, ? extends SolvingObject<G, Reachability, ?>> obj = createSolvingObject(game, new Reachability(true));
//                return getEReachabilitySolver(obj, options);
                return getEReachabilitySolver(game, new Reachability(true), options);
            }
            case A_REACHABILITY: {
//                SolvingObject<G, Reachability, ? extends SolvingObject<G, Reachability, ?>> obj = createSolvingObject(game, new Reachability(false));
//                return getAReachabilitySolver(obj, options);
                return getAReachabilitySolver(game, new Reachability(false), options);
            }
            case E_BUCHI: {
//                SolvingObject<G, Buchi, ? extends SolvingObject<G, Buchi, ?>> obj = createSolvingObject(game, new Buchi(true));
//                return getEBuchiSolver(obj, options);
                return getEBuchiSolver(game, new Buchi(true), options);
            }
            case A_BUCHI: {
//                SolvingObject<G, Buchi, ? extends SolvingObject<G, Buchi, ?>> obj = createSolvingObject(game, new Buchi(false));
//                return getABuchiSolver(obj, options);
                return getABuchiSolver(game, new Buchi(false), options);
            }
            case GLOBAL_SAFETY: {
                return getGlobalSafetySolver(game, new GlobalSafety(), options);
            }
            case LTL:
                break;
        }
        return null;
    }

    protected DistrEnvBDDSolver<GlobalSafety> getGlobalSafetySolver(PetriGameWithTransits game, GlobalSafety con, DistrEnvBDDSolverOptions options) throws SolvingException {
        try {
            return DistrEnvBDDSafetySolverFactory.getInstance().createDistrEnvBDDGlobalSafetySolver(createSolvingObject(game, con), options);
        } catch (NetNotSafeException ex) {
            throw new NotSupportedGameException(ex);
        }
    }

    @Override
    protected DistrEnvBDDSolver<Safety> getESafetySolver(PetriGameWithTransits game, Safety con, DistrEnvBDDSolverOptions options) throws SolvingException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected DistrEnvBDDSolver<GlobalSafety> getASafetySolver(PetriGameWithTransits game, Safety con, DistrEnvBDDSolverOptions options) throws SolvingException, NotSupportedGameException, NoSuitableDistributionFoundException, InvalidPartitionException {
        con.getBadPlaces().stream()
                .map(place -> new Marking(game, Map.of(place.getId(), 1)))
                .forEach(game::addFinalMarking);
        try {
            return DistrEnvBDDSafetySolverFactory.getInstance().createDistrEnvBDDGlobalSafetySolver(createSolvingObject(game, new GlobalSafety()), options);
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
