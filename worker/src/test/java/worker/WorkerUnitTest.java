package worker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkerUnitTest {

    private Connection dbConn;

    @BeforeEach
    void setUp() throws Exception {
        dbConn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");

        PreparedStatement dropTable = dbConn.prepareStatement("DROP TABLE IF EXISTS votes");
        dropTable.executeUpdate();

        PreparedStatement createTable = dbConn.prepareStatement(
            "CREATE TABLE votes (id VARCHAR(255) NOT NULL UNIQUE, vote VARCHAR(255) NOT NULL)"
        );
        createTable.executeUpdate();
    }

    @Test
    void updateVoteInsertsNewVote() throws Exception {
        Worker.updateVote(dbConn, "voter-1", "cats");

        PreparedStatement query = dbConn.prepareStatement("SELECT vote FROM votes WHERE id = ?");
        query.setString(1, "voter-1");

        ResultSet result = query.executeQuery();

        assertEquals(true, result.next());
        assertEquals("cats", result.getString("vote"));
    }

    @Test
    void updateVoteUpdatesExistingVoteForSameVoter() throws Exception {
        Worker.updateVote(dbConn, "voter-1", "cats");
        Worker.updateVote(dbConn, "voter-1", "dogs");

        PreparedStatement query = dbConn.prepareStatement("SELECT vote FROM votes WHERE id = ?");
        query.setString(1, "voter-1");

        ResultSet result = query.executeQuery();

        assertEquals(true, result.next());
        assertEquals("dogs", result.getString("vote"));
    }

    @Test
    void updateVoteDoesNotCreateDuplicateRowsForSameVoter() throws Exception {
        Worker.updateVote(dbConn, "voter-1", "cats");
        Worker.updateVote(dbConn, "voter-1", "dogs");

        PreparedStatement query = dbConn.prepareStatement("SELECT COUNT(*) FROM votes WHERE id = ?");
        query.setString(1, "voter-1");

        ResultSet result = query.executeQuery();

        assertEquals(true, result.next());
        assertEquals(1, result.getInt(1));
    }

    @Test
    void updateVoteKeepsVotesForDifferentVotersSeparate() throws Exception {
        Worker.updateVote(dbConn, "voter-1", "cats");
        Worker.updateVote(dbConn, "voter-2", "dogs");

        PreparedStatement query = dbConn.prepareStatement("SELECT COUNT(*) FROM votes");
        ResultSet result = query.executeQuery();

        assertEquals(true, result.next());
        assertEquals(2, result.getInt(1));
    }
}
