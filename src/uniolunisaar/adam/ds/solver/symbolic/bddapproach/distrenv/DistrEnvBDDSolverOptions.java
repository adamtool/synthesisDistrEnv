package uniolunisaar.adam.ds.solver.symbolic.bddapproach.distrenv;

import uniolunisaar.adam.ds.solver.symbolic.bddapproach.BDDSolverOptions;

/**
 * This class is used to store solver specific options for the solver for one
 * system player and an arbitrary number of environment players and a universal
 * safety conditions without transits.
 *
 * @author Manuel Gieseking
 */
public class DistrEnvBDDSolverOptions extends BDDSolverOptions {

    public DistrEnvBDDSolverOptions() {
        super(true, false);
    }

    public DistrEnvBDDSolverOptions(boolean skip) {
        super(skip, false);
    }

    public DistrEnvBDDSolverOptions(boolean skipTests, boolean withAutomaticTransitAnnotation) {
        super(skipTests, withAutomaticTransitAnnotation);
    }

}
