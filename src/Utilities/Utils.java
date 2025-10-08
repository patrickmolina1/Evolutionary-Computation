package Utilities;

import java.io.*;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.*;

public class Utils {
    public static List<Node> readCSV(String filePath) {
        List<Node> nodes = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Split line by comma
                String[] parts = line.split(";");
                if (parts.length != 3) continue; // skip malformed lines

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int cost = Integer.parseInt(parts[2].trim());

                nodes.add(new Node(x, y, cost));
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV: " + e.getMessage());
        }

        return nodes;
    }

    public static int calculateDistance(Node a, Node b) {
        double distance = Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
        return (int) Math.round(distance);
    }

    public static int[][] calculateDistanceMatrix(List<Node> nodes) {
        int n = nodes.size();
        int[][] distanceMatrix = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    distanceMatrix[i][j] = calculateDistance(nodes.get(i), nodes.get(j));
                } else {
                    distanceMatrix[i][j] = 0;
                }
            }
        }

        return distanceMatrix;
    }
}
