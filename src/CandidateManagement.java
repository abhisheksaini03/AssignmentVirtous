import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

class Candidate {
    String studentName;
    String collegeName;
    float round1Marks;
    float round2Marks;
    float round3Marks;
    float technicalRoundMarks;
    float totalMarks;
    String result;
    int rank;

    public Candidate(String studentName, String collegeName, float round1Marks, float round2Marks, float round3Marks, float technicalRoundMarks) {
        this.studentName = studentName;
        this.collegeName = collegeName;
        this.round1Marks = round1Marks;
        this.round2Marks = round2Marks;
        this.round3Marks = round3Marks;
        this.technicalRoundMarks = technicalRoundMarks;
        this.totalMarks = round1Marks + round2Marks + round3Marks + technicalRoundMarks;
        this.result = (totalMarks >= 35) ? "Selected" : "Rejected";
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "studentName='" + studentName + '\'' +
                ", collegeName='" + collegeName + '\'' +
                ", round1Marks=" + round1Marks +
                ", round2Marks=" + round2Marks +
                ", round3Marks=" + round3Marks +
                ", technicalRoundMarks=" + technicalRoundMarks +
                ", totalMarks=" + totalMarks +
                ", result='" + result + '\'' +
                ", rank=" + rank +
                '}';
    }
}

public class CandidateManagement {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/CandidateDB";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "abhishek123";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Enter candidate details:");
            System.out.print("Student Name: ");
            String studentName = scanner.nextLine();
            System.out.print("College Name: ");
            String collegeName = scanner.nextLine();
            System.out.print("Round 1 Marks (0 to 10): ");
            float round1Marks = scanner.nextFloat();
            System.out.print("Round 2 Marks (0 to 10): ");
            float round2Marks = scanner.nextFloat();
            System.out.print("Round 3 Marks (0 to 10): ");
            float round3Marks = scanner.nextFloat();
            System.out.print("Technical Round Marks (0 to 20): ");
            float technicalRoundMarks = scanner.nextFloat();
            scanner.nextLine(); // consume newline

            if (isValidMarks(round1Marks, 10) && isValidMarks(round2Marks, 10) && isValidMarks(round3Marks, 10) && isValidMarks(technicalRoundMarks, 20)) {
                Candidate candidate = new Candidate(studentName, collegeName, round1Marks, round2Marks, round3Marks, technicalRoundMarks);
                saveCandidateToDatabase(candidate);
            } else {
                System.out.println("Invalid marks entered. Please try again.");
            }

            System.out.print("Do you want to add another candidate? (yes/no): ");
            String continueInput = scanner.nextLine();
            if (!continueInput.equalsIgnoreCase("yes")) {
                break;
            }
        }

        List<Candidate> candidates = getAllCandidatesFromDatabase();
        assignRanks(candidates);
        updateCandidateRanksInDatabase(candidates);
        candidates.sort(Comparator.comparingInt(c -> c.rank));

        System.out.println("List of candidates sorted by rank:");
        for (Candidate candidate : candidates) {
            System.out.println(candidate);
        }
    }

    private static boolean isValidMarks(float marks, float maxMarks) {
        return marks >= 0 && marks <= maxMarks;
    }
    private static void saveCandidateToDatabase(Candidate candidate) {
        String sql = "INSERT INTO Candidate (studentName, collegeName, round1Marks, round2Marks, round3Marks, technicalRoundMarks) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, candidate.studentName);
            stmt.setString(2, candidate.collegeName);
            stmt.setFloat(3, candidate.round1Marks);
            stmt.setFloat(4, candidate.round2Marks);
            stmt.setFloat(5, candidate.round3Marks);
            stmt.setFloat(6, candidate.technicalRoundMarks);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    

    private static List<Candidate> getAllCandidatesFromDatabase() {
        List<Candidate> candidates = new ArrayList<>();
        String sql = "SELECT studentName, collegeName, round1Marks, round2Marks, round3Marks, technicalRoundMarks, totalMarks, result FROM Candidate";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Candidate candidate = new Candidate(
                    rs.getString("studentName"),
                    rs.getString("collegeName"),
                    rs.getFloat("round1Marks"),
                    rs.getFloat("round2Marks"),
                    rs.getFloat("round3Marks"),
                    rs.getFloat("technicalRoundMarks")
                );
                candidate.totalMarks = rs.getFloat("totalMarks");
                candidate.result = rs.getString("result");
                candidates.add(candidate);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return candidates;
    }
    

    private static void assignRanks(List<Candidate> candidates) {
        candidates.sort(Comparator.comparingDouble(c -> -c.totalMarks));
        int rank = 1;
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0 && candidates.get(i).totalMarks == candidates.get(i - 1).totalMarks) {
                candidates.get(i).rank = candidates.get(i - 1).rank;
            } else {
                candidates.get(i).rank = rank;
            }
            rank++;
        }
    }

    private static void updateCandidateRanksInDatabase(List<Candidate> candidates) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            for (Candidate candidate : candidates) {
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE Candidate SET `rank` = ? WHERE studentName = ? AND collegeName = ?")) {
                    stmt.setInt(1, candidate.rank);
                    stmt.setString(2, candidate.studentName);
                    stmt.setString(3, candidate.collegeName);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
}
