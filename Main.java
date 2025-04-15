import java.sql.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Database credentials
        String url = "jdbc:mysql://localhost:3306/pathfinder_db"; // Update with your DB URL
        String user = "root"; // Update with your DB username
        String password = "Ramraya2308$"; // Update with your DB password

        // Initialize the graph
        Graph graph = new Graph();

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // Load graph data from the database
            graph.loadFromDB(conn);

            // Taking user input for source and destination
            System.out.print("Enter Source Location Name: ");
            String source = scanner.nextLine();
            System.out.print("Enter Destination Location Name: ");
            String destination = scanner.nextLine();

            // Find the safest path
            Result result = graph.dijkstra(source, destination);

            // Output the result
            System.out.println("Safest Path Risk Score: " + result.totalRiskScore);
            System.out.println("Safest Path:");
            System.out.println(result);
        } catch (SQLException e) {
            e.printStackTrace();  // Print the stack trace for any SQL exceptions
        }
    }
}
