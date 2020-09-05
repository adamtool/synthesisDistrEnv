package uniolunisaar.adam.logic.distrsynt.solver.distrenv.safety;

import uniolunisaar.adam.ds.objectives.Safety;
import uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolverOptions;
import uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv.DistrEnvBDDSolvingObject;
import uniolunisaar.adam.exceptions.synthesis.pgwt.InvalidPartitionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NoSuitableDistributionFoundException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.NotSupportedGameException;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;

/**
 * This is just a helper class since JAVA does not allow restricting the
 * visibility to subpackages but the constructor of the single solver should not
 * be visible to the outside but have to be called in 'DistrSysBDDSolverFactor'.
 *
 * Do not use the factory to create the solver. Use 'DistrSysBDDSolverFactor'.
 *
 * @author thewn
 */
public class DistrEnvBDDSafetySolverFactory {

    private static DistrEnvBDDSafetySolverFactory instance = null;

    public static DistrEnvBDDSafetySolverFactory getInstance() {
        if (instance == null) {
            instance = new DistrEnvBDDSafetySolverFactory();
        }
        return instance;
    }

    private DistrEnvBDDSafetySolverFactory() {

    }

    public DistrEnvBDDASafetySolver createDistrEnvBDDASafetySolver(DistrEnvBDDSolvingObject<Safety> obj, DistrEnvBDDSolverOptions opts) throws NotSupportedGameException, NoSuitableDistributionFoundException, InvalidPartitionException, NetNotSafeException {
        return new DistrEnvBDDASafetySolver(obj, opts);
    }

}
