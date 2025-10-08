package Utilities;

public class Node {
    public int id;
    public int x;
    public int y;
    public int cost;
     static int IDS = 0;

    public Node(int x, int y, int cost) {
        this.id= IDS++;
        this.x = x;
        this.y = y;
        this.cost = cost;
    }

    @Override
    public String toString() {
        return "Utilities.Node{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", cost=" + cost +
                '}';
    }
}
