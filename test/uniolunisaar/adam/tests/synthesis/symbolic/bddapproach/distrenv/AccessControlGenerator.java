package uniolunisaar.adam.tests.synthesis.symbolic.bddapproach.distrenv;

import java.util.Map;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import static uniolunisaar.adam.ds.objectives.Condition.Objective.GLOBAL_SAFETY;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.logic.synthesis.pgwt.calculators.CalculatorIDs;
import uniolunisaar.adam.logic.synthesis.pgwt.calculators.ConcurrencyPreservingGamesCalculator;
import uniolunisaar.adam.logic.synthesis.pgwt.calculators.MaxTokenCountCalculator;
import uniolunisaar.adam.util.PGTools;

public class AccessControlGenerator {
    public static PetriGameWithTransits generate(int authenticators) {
        if (authenticators < 1) {
            throw new IllegalArgumentException("requirement: authenticators >= 1");
        }

        PetriGameWithTransits game = new PetriGameWithTransits("Access Control with " + authenticators + " authenticators");
        Place s_closed = game.createPlace("s_closed");
        Place s_open = game.createPlace("s_open");
        Transition open = game.createTransition("open");
        game.createFlow(s_closed, open);
        game.createFlow(open, s_open);

        s_closed.setInitialToken(1);

        game.setPartition(s_closed, 0);
        game.setPartition(s_open, 0);

        for (int i = 1; i <= authenticators; i++) {
            Transition share = addAuthenticator(game, i);
            game.createFlow(s_closed, share);
            game.createFlow(share, s_closed);
        }

        PGTools.setConditionAnnotation(game, GLOBAL_SAFETY);
        game.addExtensionCalculator(CalculatorIDs.MAX_TOKEN_COUNT.name(), new MaxTokenCountCalculator(), true);
        game.addExtensionCalculator(CalculatorIDs.CONCURRENCY_PRESERVING.name(), new ConcurrencyPreservingGamesCalculator(), true);

        return game;
    }

    private static Transition addAuthenticator(PetriGameWithTransits game, int id) {
        Place a = game.createEnvPlace("a" + id);
        Place a_auth = game.createEnvPlace("a" + id + "_auth");
        Place e = game.createEnvPlace("e" + id);
        Place e_attempt = game.createEnvPlace("e" + id + "_attempt");
        Place e_wait = game.createEnvPlace("e" + id + "_wait");

        Transition finish = game.createTransition("finish" + id);
        Transition auth = game.createTransition("auth" + id);
        Transition skip = game.createTransition("skip" + id);
        Transition share = game.createTransition("share" + id);

        game.createFlow(e, finish);
        game.createFlow(a_auth, finish);
        game.createFlow(finish, e_wait);
        game.createFlow(finish, a_auth);

        game.createFlow(a, auth);
        game.createFlow(e, auth);
        game.createFlow(auth, a_auth);
        game.createFlow(auth, e_attempt);

        game.createFlow(e, skip);
        game.createFlow(skip, e_attempt);

        game.createFlow(e_attempt, share);
        game.createFlow(share, e);

        a.setInitialToken(1);
        e.setInitialToken(1);

        game.setPartition(a, 2 * id - 1);
        game.setPartition(a_auth, 2 * id - 1);
        game.setPartition(e, 2 * id);
        game.setPartition(e_attempt, 2 * id);
        game.setPartition(e_wait, 2 * id);

        // final marking == bad marking
        game.addFinalMarking(new Marking(game, Map.of("s_open", 1, a.getId(), 1)));

        return share;
    }
}
