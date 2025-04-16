import java.sql.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Database credentials
        String url = "jdbc:mysql://localhost:3306/pathfinder_db";
        String user = "root";
        String password = "Ramraya2308$";

        Graph graph = new Graph();

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            graph.loadFromDB(conn);

            while (true) {
                System.out.println("\n--- Pathfinder Menu ---");
                System.out.println("1. Find Safest Path");
                System.out.println("2. Emergency Button (Nearest Police Stations)");
                System.out.println("3. Rate a Location's Safety");
                System.out.println("4. View Top 5 Safest Locations");
                System.out.println("5. Exit");
                System.out.print("Enter your choice: ");
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        System.out.print("Enter Source Location Name: ");
                        String source = scanner.nextLine();
                        System.out.print("Enter Destination Location Name: ");
                        String destination = scanner.nextLine();
                        Result result = graph.dijkstra(source, destination);
                        System.out.println("Safest Path:");
                        System.out.println(result);
                        break;
                    case 2:
                        System.out.print("Enter your Current Location Name: ");
                        String currentLocation = scanner.nextLine();
                        fetchEmergencyContacts(conn, currentLocation);
                        break;
                    case 3:
                        rateLocation(scanner, conn);
                        break;
                    case 4:
                        displayTopSafestLocations(conn);
                        break;
                    case 5:
                        System.out.println("Exiting... Stay safe!");
                        return;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void fetchEmergencyContacts(Connection conn, String locationName) {
        String query = "SELECT ec.contact_name, ec.phone_number " +
                       "FROM emergencycontacts ec " +
                       "JOIN nodes n ON ec.node_id = n.NodeID " +
                       "WHERE n.NodeName = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, locationName);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n--- Emergency Contacts Near " + locationName + " ---");
            boolean found = false;
            while (rs.next()) {
                String name = rs.getString("contact_name");
                String phone = rs.getString("phone_number");
                System.out.println("Name: " + name + " | Phone: " + phone);
                found = true;
            }

            if (!found) {
                System.out.println("No emergency contacts found for this location.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void rateLocation(Scanner scanner, Connection conn) {
        try {
            System.out.print("Enter your user ID: ");
            String userId = scanner.nextLine();

            System.out.print("Enter the location name you want to rate: ");
            String location = scanner.nextLine();

            System.out.print("Rate the location (Safe / Neutral / Unsafe): ");
            String rating = scanner.nextLine().trim();

            if (!rating.equalsIgnoreCase("Safe") &&
                !rating.equalsIgnoreCase("Neutral") &&
                !rating.equalsIgnoreCase("Unsafe")) {
                System.out.println("Invalid rating. Must be Safe, Neutral, or Unsafe.");
                return;
            }

            // Get NodeID from name
            String nodeQuery = "SELECT NodeID FROM nodes WHERE NodeName = ?";
            try (PreparedStatement nodeStmt = conn.prepareStatement(nodeQuery)) {
                nodeStmt.setString(1, location);
                ResultSet rs = nodeStmt.executeQuery();

                if (rs.next()) {
                    int nodeId = rs.getInt("NodeID");

                    String insertQuery = "INSERT INTO userratings (node_id, user_id, safety_rating) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                        insertStmt.setInt(1, nodeId);
                        insertStmt.setString(2, userId);
                        insertStmt.setString(3, rating);
                        insertStmt.executeUpdate();
                        System.out.println("✅ Rating submitted successfully.");
                    }
                } else {
                    System.out.println("❌ Location not found.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void displayTopSafestLocations(Connection conn) {
        String query = "SELECT n.NodeName, " +
                       "AVG(CASE safety_rating " +
                       "     WHEN 'Safe' THEN 3 " +
                       "     WHEN 'Neutral' THEN 2 " +
                       "     WHEN 'Unsafe' THEN 1 END) AS avg_rating " +
                       "FROM userratings ur " +
                       "JOIN nodes n ON ur.node_id = n.NodeID " +
                       "GROUP BY n.NodeName " +
                       "ORDER BY avg_rating DESC " +
                       "LIMIT 5";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            System.out.println("\n--- Top 5 Safest Locations Based on User Ratings ---");
            while (rs.next()) {
                String name = rs.getString("NodeName");
                double avg = rs.getDouble("avg_rating");
                System.out.printf("%s (Avg Rating: %.2f)\n", name, avg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
