import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckStatId {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@100.68.224.11:1521/XEPDB1", "message", "0000");
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("SELECT sequence_name FROM user_sequences");
            while (rs.next()) {
                System.out.println("Sequence: " + rs.getString(1));
            }
            rs.close();
            
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
