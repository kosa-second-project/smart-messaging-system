import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckChannelStatId {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@100.68.224.11:1521/XEPDB1", "message", "0000");
            Statement stmt = conn.createStatement();
            
            rs = stmt.executeQuery("SELECT sequence_name FROM user_sequences WHERE sequence_name LIKE '%CHANNEL_STAT%'");
            while (rs.next()) {
                System.out.println("Sequence: " + rs.getString(1));
            }
            rs.close();
            
            ResultSet rs2 = stmt.executeQuery("SELECT column_name, data_default, identity_column FROM user_tab_columns WHERE table_name = 'CHANNEL_STAT' AND column_name = 'ID'");
            while (rs2.next()) {
                System.out.println("Column ID Default: " + rs2.getString(2) + " Identity: " + rs2.getString(3));
            }
            rs2.close();
            
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
