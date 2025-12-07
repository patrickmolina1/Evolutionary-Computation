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


    public static double oldnodeBasedSimilarity(Solution sol1, Solution sol2) {
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

    public static double nodeBasedSimilarity(Solution sol1, Solution sol2) {
        if (sol1 == null || sol2 == null || sol1.cycle == null || sol2.cycle == null)
            return 0.0;

        List<Integer> cycle1 = sol1.cycle;
        List<Integer> cycle2 = sol2.cycle;

        if (cycle1.isEmpty() && cycle2.isEmpty()) return 1.0;
        if (cycle1.size() != cycle2.size()) return 0.0;

        // For TSP, node-based similarity should measure positional agreement
        // Count how many nodes appear at the same position in both cycles
        int matchingPositions = 0;
        for (int i = 0; i < cycle1.size(); i++) {
            if (cycle1.get(i).equals(cycle2.get(i))) {
                matchingPositions++;
            }
        }

        return (double) matchingPositions / cycle1.size();
    }

    public static double cyclicNodeBasedSimilarity(Solution sol1, Solution sol2) {
        if (sol1 == null || sol2 == null || sol1.cycle == null || sol2.cycle == null)
            return 0.0;

        List<Integer> c1 = sol1.cycle;
        List<Integer> c2 = sol2.cycle;

        int n = c1.size();
        if (n == 0 && c2.size() == 0) return 1.0;
        if (n != c2.size()) return 0.0;

        double best = 0.0;

        // try all rotations of c2
        for (int shift = 0; shift < n; shift++) {
            double match = 0;
            for (int i = 0; i < n; i++) {
                if (c1.get(i).equals(c2.get((i + shift) % n)))
                    match++;
            }
            best = Math.max(best, match / n);
        }

        // also try reversed c2 for direction-invariant cycles
        List<Integer> c2rev = new ArrayList<>(c2);
        Collections.reverse(c2rev);

        for (int shift = 0; shift < n; shift++) {
            double match = 0;
            for (int i = 0; i < n; i++) {
                if (c1.get(i).equals(c2rev.get((i + shift) % n)))
                    match++;
            }
            best = Math.max(best, match / n);
        }

        return best;
    }




}
