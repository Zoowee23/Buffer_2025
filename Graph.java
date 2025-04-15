import java.sql.*;
import java.util.*;

class Edge {
    String to;
    double riskScore;

    Edge(String to, double riskScore) {
        this.to = to;
        this.riskScore = riskScore;
    }
}

class Graph {
    Map<String, List<Edge>> adjList = new HashMap<>();

    // Adds an edge between two nodes with a risk score
    public void addEdge(String from, String to, double riskScore) {
        adjList.computeIfAbsent(from, k -> new ArrayList<>()).add(new Edge(to, riskScore));
        adjList.computeIfAbsent(to, k -> new ArrayList<>()).add(new Edge(from, riskScore));  // Assuming undirected graph
    }

    // Load graph data from the MySQL database
    public void loadFromDB(Connection conn) {
        String query = "SELECT n1.NodeName AS source, n2.NodeName AS destination, " +
                       "(e.CSS + e.SLF + e.PPI + e.PPS) AS risk_score " +
                       "FROM edges e " +
                       "JOIN nodes n1 ON e.FromNode = n1.NodeID " +
                       "JOIN nodes n2 ON e.ToNode = n2.NodeID";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String from = rs.getString("source");
                String to = rs.getString("destination");
                double riskScore = rs.getDouble("risk_score");
                addEdge(from, to, riskScore);  // Add edge to graph
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Dijkstra's algorithm to find the safest path
    public Result dijkstra(String source, String destination) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        for (String node : adjList.keySet()) {
            dist.put(node, Double.MAX_VALUE);  // Store distances as Double
        }
        dist.put(source, 0.0);
        pq.add(source);

        while (!pq.isEmpty()) {
            String current = pq.poll();

            if (current.equals(destination)) break;

            for (Edge edge : adjList.getOrDefault(current, new ArrayList<>())) {
                double newDist = dist.get(current) + edge.riskScore;

                if (newDist < dist.getOrDefault(edge.to, Double.MAX_VALUE)) {
                    dist.put(edge.to, newDist);
                    prev.put(edge.to, current);
                    pq.add(edge.to);
                }
            }
        }

        List<String> path = new ArrayList<>();
        String current = destination;
        while (current != null) {
            path.add(0, current);
            current = prev.get(current);
        }

        // Cast the totalRiskScore to int
        int totalRiskScore = (int) Math.round(dist.getOrDefault(destination, Double.MAX_VALUE));  // Round and cast to int
        return new Result(path, totalRiskScore);
    }
}

class Result {
    List<String> path;
    int riskscoreInt;

    Result(List<String> path, int riskscoreInt) {
        this.path = path;
        this.riskscoreInt = riskscoreInt;
    }

    @Override
    public String toString() {
        return "Safest Path: " + String.join(" -> ", path) + " -> END\nTotal Risk Score: " + riskscoreInt;
    }
}



