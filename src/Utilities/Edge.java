package Utilities;

import java.util.Objects;

public class Edge {
    int u;
    int v;

    Edge(int a, int b) {
        // Undirected: store smaller first
        if (a < b) {
            this.u = a;
            this.v = b;
        } else {
            this.u = b;
            this.v = a;
        }
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Edge edge)) return false;

        return u == edge.u && v == edge.v;
    }

    @Override
    public int hashCode() {
        return Objects.hash(u, v);
    }
}
