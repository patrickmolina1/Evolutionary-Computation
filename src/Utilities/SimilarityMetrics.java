package Utilities;

import java.util.*;

public class SimilarityMetrics {

    public static double edgeBasedSimilarity(Solution sol1, Solution sol2) {
        if (sol1 == null || sol2 == null || sol1.cycle == null || sol2.cycle == null)
            return 0.0;

        Set<Edge> edges1 = extractEdges(sol1.cycle);
        Set<Edge> edges2 = extractEdges(sol2.cycle);

        if (edges1.isEmpty() && edges2.isEmpty()) return 1.0;
        if (edges1.isEmpty() || edges2.isEmpty()) return 0.0;

        Set<Edge> intersection = new HashSet<>(edges1);
        intersection.retainAll(edges2);

        Set<Edge> union = new HashSet<>(edges1);
        union.addAll(edges2);

        return (double) intersection.size() / union.size();
    }


    public static double nodeBasedSimilarity(Solution sol1, Solution sol2) {
        if (sol1 == null || sol2 == null || sol1.cycle == null || sol2.cycle == null)
            return 0.0;

        Set<Integer> nodes1 = new HashSet<>(sol1.cycle);
        Set<Integer> nodes2 = new HashSet<>(sol2.cycle);

        if (nodes1.isEmpty() && nodes2.isEmpty()) return 1.0;
        if (nodes1.isEmpty() || nodes2.isEmpty()) return 0.0;

        Set<Integer> intersection = new HashSet<>(nodes1);
        intersection.retainAll(nodes2);

        Set<Integer> union = new HashSet<>(nodes1);
        union.addAll(nodes2);

        return (double) intersection.size() / union.size();
    }


    private static Set<Edge> extractEdges(List<Integer> cycle) {
        Set<Edge> edges = new HashSet<>();
        if (cycle.size() < 2) return edges;

        for (int i = 0; i < cycle.size(); i++) {
            int a = cycle.get(i);
            int b = cycle.get((i + 1) % cycle.size());

            edges.add(new Edge(a, b));
        }
        return edges;
    }






}
