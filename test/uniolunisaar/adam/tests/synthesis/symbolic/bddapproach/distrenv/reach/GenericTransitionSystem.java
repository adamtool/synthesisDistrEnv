package uniolunisaar.adam.tests.synthesis.symbolic.bddapproach.distrenv.reach;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uniol.apt.adt.ts.TransitionSystem;

public class GenericTransitionSystem<S, L> {

	private final S initial;
	private final Map<S, Vertex<S>> vertices = new HashMap<>();
	private final Map<Vertex<S>, Set<Edge<S, L>>> preset = new HashMap<>();
	private final Map<Vertex<S>, Set<Edge<S, L>>> postset = new HashMap<>();

	public GenericTransitionSystem(S initial) {
		this.initial = initial;
	}

	public TransitionSystem toAptLts(Function<S, String> stateToString, Function<L, String> labelToString) {
		TransitionSystem ts = new TransitionSystem();
		for (Vertex<S> vertex : this.getVertices()) {
			ts.createState(stateToString.apply(vertex.getState()));
		}
		for (Edge<S, L> edge : this.getEdges()) {
			ts.createArc(
					stateToString.apply(edge.getFrom().getState()),
					stateToString.apply(edge.getTo().getState()),
					labelToString.apply(edge.getLabel())
			);
		}
		ts.setInitialState(stateToString.apply(this.initial));
		return ts;
	}

	public Vertex<S> createVertex(S state) {
		return this.vertices.computeIfAbsent(state, Vertex::new);
	}

	public Optional<Vertex<S>> getVertex(S state) {
		return Optional.ofNullable(this.vertices.get(state));
	}

	public Edge<S, L> createEdge(Vertex<S> from, Vertex<S> to, L label) {
		Set<Edge<S, L>> postset = this.postset.computeIfAbsent(from, key -> new HashSet<>());
		Set<Edge<S, L>> preset = this.preset.computeIfAbsent(to, key -> new HashSet<>());

		Edge<S, L> edge = Stream.concat(postset.stream(), preset.stream())
				.filter(e -> e.getFrom().equals(from) && e.getTo().equals(to) && e.getLabel().equals(label))
				.findAny()
				.orElseGet(() -> new Edge<>(from, to, label));
		postset.add(edge);
		preset.add(edge);
		return edge;
	}

	public Set<Vertex<S>> getVertices() {
		return Set.copyOf(this.vertices.values());
	}

	public Set<Edge<S, L>> getEdges() {
		return Stream.concat(this.preset.values().stream(), this.postset.values().stream())
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
	}

	public Set<Edge<S, L>> getOutgoingEdges(Vertex<S> vertex) {
		return Collections.unmodifiableSet(this.postset.getOrDefault(vertex, Collections.emptySet()));
	}

	public Set<Edge<S, L>> getIncomingEdges(Vertex<S> vertex) {
		return Collections.unmodifiableSet(this.preset.getOrDefault(vertex, Collections.emptySet()));
	}

	public Vertex<S> getInitial() {
		return this.getVertex(this.initial).orElseThrow(() -> new IllegalStateException("The initial vertex was never created."));
	}
}
