package uniolunisaar.adam.tests.synthesis.symbolic.bddapproach.distrenv.reach;

public class Vertex<S> {
	private final S state;

	Vertex(S state) {
		this.state = state;
	}

	public S getState() {
		return state;
	}

	@Override
	public String toString() {
		return "Place{" + state + '}';
	}
}
