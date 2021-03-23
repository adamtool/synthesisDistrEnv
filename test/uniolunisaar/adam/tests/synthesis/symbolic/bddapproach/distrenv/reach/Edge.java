package uniolunisaar.adam.tests.synthesis.symbolic.bddapproach.distrenv.reach;

public class Edge<S, L> {

	private final L label;
	private final Vertex<S> from, to;

	public Edge(Vertex<S> from, Vertex<S> to, L label) {
		this.from = from;
		this.to = to;
		this.label = label;
	}

	public L getLabel() {
		return label;
	}

	public Vertex<S> getFrom() {
		return from;
	}

	public Vertex<S> getTo() {
		return to;
	}

	@Override
	public String toString() {
		return "Edge{" +
				"label=" + label +
				", from=" + from +
				", to=" + to +
				'}';
	}
}
