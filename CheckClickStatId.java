import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckClickStatId {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@100.68.224.11:1521/XEPDB1", "message", "0000");
            Statement stmt = conn.createStatement();
            
            // Check triggers for click_stat
            ResultSet rs = stmt.executeQuery("SELECT trigger_name, trigger_body FROM user_triggers WHERE table_name = 'CLICK_STAT'");
            while (rs.next()) {
                System.out.println("Trigger: " + rs.getString(1) + " - " + rs.getString(2));
            }
            rs.close();
            
            // Check sequences
            rs = stmt.executeQuery("SELECT sequence_name FROM user_sequences WHERE sequence_name LIKE '%CLICK_STAT%'");
            while (rs.next()) {
                System.out.println("Sequence: " + rs.getString(1));
            }
            rs.close();
            
            // Check table columns
            rs = stmt.executeQuery("SELECT column_name, data_default, identity_column FROM user_tab_columns WHERE table_name = 'CLICK_STAT' AND column_name = 'ID'");
            while (rs.next()) {
                System.out.println("Column ID Default: " + rs.getString(2) + " Identity: " + rs.getString(3));
            }
            rs.close();
            
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
