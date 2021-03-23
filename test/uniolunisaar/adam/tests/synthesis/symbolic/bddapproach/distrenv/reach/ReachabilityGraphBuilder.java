package uniolunisaar.adam.tests.synthesis.symbolic.bddapproach.distrenv.reach;

import java.util.HashSet;
import java.util.Set;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Transition;

public class ReachabilityGraphBuilder {

    public GenericTransitionSystem<Marking, Transition> build(PetriNet net) {
        return new Builder(net, null).build();
    }

    public GenericTransitionSystem<Marking, Transition> build(PetriNet net, Marking maxMarking) {
        return new Builder(net, maxMarking).build();
    }

    private static class Builder {
        private final PetriNet net;
        private final Marking maxMarking;
        private final GenericTransitionSystem<Marking, Transition> ts;
        private final Set<Transition> transitions;
        private final Set<Marking> completed = new HashSet<>();

        public Builder(PetriNet net, Marking maxMarking) {
            this.net = net;
            this.maxMarking = maxMarking;
            this.ts = new GenericTransitionSystem<>(this.net.getInitialMarking());
            this.transitions = this.net.getTransitions();
        }

        public GenericTransitionSystem<Marking, Transition> build() {
            Marking initialMarking = this.net.getInitialMarking();
            rec(initialMarking);
            return this.ts;
        }

        private void rec(Marking marking) {
            if (!this.completed.add(marking)) {
                return;
            }
            for (Transition transition : transitions) {
                if (!transition.isFireable(marking)) {
                    continue;
                }
                Marking nextMarking = transition.fire(marking);
                if (this.maxMarking != null && !Comparison.markingLessEqual(nextMarking, this.maxMarking)) {
                    continue;
                }
                ts.createEdge(ts.createVertex(marking), ts.createVertex(nextMarking), transition);
                rec(nextMarking);
            }
        }
    }
}
