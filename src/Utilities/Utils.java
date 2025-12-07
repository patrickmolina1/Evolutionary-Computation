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

    public static Solution getBestSolution(String filePath, Instance instance) throws IOException{

        try (BufferedReader br = new BufferedReader(new FileReader(filePath+"best"+instance.name+".csv"))) {
            // Skip header
            String line = br.readLine();
            if (line == null) return null;

            // Read data row
            line = br.readLine();
            if (line == null) return null;

            // Handle quoted cycle list safely
            String[] parts = line.split(",");


            //0: Method,
            // 1: Instance,
            // 2: SolutionID,
            // 3: TotalCost,
            // 4: NumNodes,
            // 5: TotalDistance,
            // 6: ObjectiveFunction,
            // 7: TotalRunningTime,
            // 8: TotalIterations,
            // 9: Cycle

            //Solution(List<Node> selectedNodes, List<Integer> cycle, int totalCost, int totalDistance, int totalRunningTime)
            Solution s = new Solution(transformNodeIDsToNodes(parts[9],instance.nodes),
                    transformNodeIDsToCycle(parts[9]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[5]),Integer.parseInt(parts[7]));


            return s;
        }

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

    public static List<Node> transformNodeIDsToNodes(String nodeIDs, List<Node> allNodes){
        List<Node> nodes = new ArrayList<>();
        String[] ids = nodeIDs.replace("\"", "").split("-");
        for(String id : ids){
            nodes.add(allNodes.get(Integer.parseInt(id)));
        }
        return nodes;
    }

    public static List<Integer> transformNodeIDsToCycle(String nodeIDs){
        List<Integer> cycle = new ArrayList<>();
        String[] ids = nodeIDs.replace("\"", "").split("-");
        for(String id : ids){
            cycle.add(Integer.parseInt(id));
        }
        return cycle;
    }
}
