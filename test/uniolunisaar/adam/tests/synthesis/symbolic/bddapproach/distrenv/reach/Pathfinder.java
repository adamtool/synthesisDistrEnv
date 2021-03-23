package uniolunisaar.adam.tests.synthesis.symbolic.bddapproach.distrenv.reach;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import uniol.apt.util.Pair;

public class Pathfinder {
	public static <S, L> Optional<List<Edge<S, L>>> findPath(GenericTransitionSystem<S, L> graph, Vertex<S> from, L to) {
		Predicate<Edge<S, L>> goal = edge -> edge.getLabel().equals(to);
		return findPath(graph, from, goal);
	}

	public static <S, L> Optional<List<Edge<S, L>>> findPath(GenericTransitionSystem<S, L> graph, Vertex<S> from, Vertex<S> to) {
		Predicate<Edge<S, L>> goal = edge -> edge.getTo().equals(to);
		return findPath(graph, from, goal);
	}

	public static <S, L> Optional<List<Edge<S, L>>> findPath(GenericTransitionSystem<S, L> graph, Vertex<S> from, Predicate<Edge<S, L>> goal) {
		Set<Vertex<S>> visited = new HashSet<>();
		Queue<Pair<Vertex<S>, List<Edge<S, L>>>> q = new LinkedList<>();
		q.add(new Pair<>(from, new LinkedList<>()));
		while (!q.isEmpty()) {
			Pair<Vertex<S>, List<Edge<S, L>>> current = q.remove();
			Set<Edge<S, L>> outgoingEdges = graph.getOutgoingEdges(current.getFirst());
			for (Edge<S, L> outgoingEdge : outgoingEdges) {
				LinkedList<Edge<S, L>> path = new LinkedList<>(current.getSecond());
				path.add(outgoingEdge);
				Vertex<S> next = outgoingEdge.getTo();
				if (goal.test(outgoingEdge)) {
					return Optional.of(path);
				} else if (!visited.contains(next)) {
					q.add(new Pair<>(next, path));
					visited.add(next);
				}
			}
		}
		return Optional.empty();
	}
}
