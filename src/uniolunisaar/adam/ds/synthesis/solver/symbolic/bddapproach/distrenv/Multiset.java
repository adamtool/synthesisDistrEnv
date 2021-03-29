package uniolunisaar.adam.ds.synthesis.solver.symbolic.bddapproach.distrenv;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Multiset<K> {
    private final Map<K, Integer> map;

    private Multiset(Map<K, Integer> map) {
        this.map = map;
    }

    public int count(K element) {
        return this.map.getOrDefault(element, 0);
    }

    public boolean contains(K element, int minMultiplicity) {
        return this.count(element) >= minMultiplicity;
    }

    public boolean contains(K element) {
        return this.contains(element, 1);
    }

    public boolean containsAll(Set<K> set) {
        return set.stream().allMatch(this::contains);
    }

    public static <K> Collector<K, ?, Multiset<K>> collect() {
        return Collectors.collectingAndThen(Collectors.toMap(Function.identity(), place -> 1, Integer::sum), Multiset::new);
    }

    public Stream<K> stream() {
        return this.map.entrySet().stream()
                .flatMap(entry -> IntStream.rangeClosed(1, entry.getValue()).mapToObj(i -> entry.getKey()));
    }

    public boolean equals(Set<K> set) {
        return this.map.keySet().equals(set) && Set.copyOf(this.map.values()).equals(Set.of(1));
    }

    public Set<K> elements() {
        return Collections.unmodifiableSet(this.map.keySet());
    }

    public String toString(final BiFunction<? super K, ? super Integer, String> f,
                           final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        return this.map.entrySet().stream()
                .map(e -> f.apply(e.getKey(), e.getValue()))
                .collect(Collectors.joining(delimiter, prefix, suffix));
    }

    public String toString(final BiFunction<? super K, ? super Integer, String> f) {
        return toString(f, ", ", "{", "}");
    }

    public String toString(final Function<? super K, String> f) {
        return toString((element, count) -> count + "x " + f.apply(element), ", ", "{", "}");
    }

    @Override
    public String toString() {
        return this.toString(Object::toString);
    }
}
