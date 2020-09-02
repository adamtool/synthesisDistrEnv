package uniolunisaar.adam.logic.pg.solver.distrenv;

import uniolunisaar.adam.ds.solver.LLSolverOptions;

/**
 *
 * @author Manuel Gieseking
 */
public class DistrEnvBDDSolverOptions extends LLSolverOptions {

    private boolean gg = false;
    private boolean ggs = false;
    private boolean pgs = true;

    public DistrEnvBDDSolverOptions() {
        super("distrEnv");
    }

    public DistrEnvBDDSolverOptions(boolean skip) {
        super(skip, "distrEnv");
    }

    public DistrEnvBDDSolverOptions(String libraryName, int maxIncrease, int initNodeNb, int cacheSize) {
        super("distrEnv");
    }

    public DistrEnvBDDSolverOptions(boolean gg, boolean ggs, boolean pgs) {
        super("distrEnv");
        this.gg = gg;
        this.ggs = ggs;
        this.pgs = pgs;
    }

    public boolean isGg() {
        return gg;
    }

    public void setGg(boolean gg) {
        this.gg = gg;
    }

    public boolean isGgs() {
        return ggs;
    }

    public void setGgs(boolean ggs) {
        this.ggs = ggs;
    }

    public boolean isPgs() {
        return pgs;
    }

    public void setPgs(boolean pgs) {
        this.pgs = pgs;
    }
}
