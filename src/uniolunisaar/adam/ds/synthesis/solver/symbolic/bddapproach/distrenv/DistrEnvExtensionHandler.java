package uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv;

import uniol.apt.adt.pn.Flow;
import uniolunisaar.adam.util.AdamDistrEnvExtensions;
import uniolunisaar.adam.util.ExtensionManagement;

/**
 *
 * @author Manuel Gieseking
 */
public class DistrEnvExtensionHandler {

    // register the Extensions for this submodule
    static {
        ExtensionManagement.getInstance().registerExtensions(true, AdamDistrEnvExtensions.values());
    }

    public static void setMovesTokenBetweenEqualGamePlaces(Flow f) {
        ExtensionManagement.getInstance().putExtension(f, AdamDistrEnvExtensions.flowMovesTokenBetweenEqualGamePlaces, true);
    }

}
