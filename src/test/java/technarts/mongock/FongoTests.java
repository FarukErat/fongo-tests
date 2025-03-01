package technarts.mongock;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FongoTests {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> usersCollection;

    @BeforeAll
    static void setUp() {
        Fongo fongo = new Fongo("mockDB");
        mongoClient = fongo.getMongo();
        database = mongoClient.getDatabase("testDB");
        usersCollection = database.getCollection("users");
    }

    @AfterEach
    void tearDown() {
        database.drop();
    }

    @Test
    void find_whenDocExists_returnsDocument() {
        Document doc = new Document("name", "Alice").append("age", 25);

        usersCollection.insertOne(doc);
        Document found = usersCollection.find(new Document("name", "Alice")).first();

        assertNotNull(found, "Expected to find a document with name 'Alice'");
        assertEquals("Alice", found.getString("name"), "Name should be 'Alice'");
        assertEquals(25, found.getInteger("age"), "Age should be 25");
    }

    @Test
    void find_whenDocDoesNotExist_returnsNull() {
        Document found = usersCollection.find(new Document("name", "Bob")).first();

        assertNull(found, "Expected no document for name 'Bob'");
    }

    @Test
    void aggregateMatch_whenDocsExist_returnsMatchedDocuments() {
        usersCollection.insertMany(Arrays.asList(
            new Document("name", "Alice").append("age", 25),
            new Document("name", "Bob").append("age", 30),
            new Document("name", "Charlie").append("age", 20)
        ));

        List<Document> results = usersCollection.aggregate(List.of(
                new Document("$match", new Document("age", new Document("$gt", 20)))
        )).into(new ArrayList<>());

        assertEquals(2, results.size(), "Expected two documents with age > 20");
        for (Document doc : results) {
            assertTrue(doc.getInteger("age") > 20, "Each document should have age greater than 20");
        }
    }

    @Test
    void update_whenDocExists_updatesDocument() {
        Document doc = new Document("name", "Alice").append("age", 25);
        usersCollection.insertOne(doc);

        Document update = new Document("$set", new Document("age", 26));
        usersCollection.updateOne(new Document("name", "Alice"), update);

        Document updatedDoc = usersCollection.find(new Document("name", "Alice")).first();
        assertNotNull(updatedDoc, "Expected to find updated document for 'Alice'");
        assertEquals(26, updatedDoc.getInteger("age"), "Age should be updated to 26");
    }

    @Test
    void delete_whenDocExists_removesDocument() {
        Document doc = new Document("name", "Alice").append("age", 25);
        usersCollection.insertOne(doc);

        usersCollection.deleteOne(new Document("name", "Alice"));

        Document deletedDoc = usersCollection.find(new Document("name", "Alice")).first();
        assertNull(deletedDoc, "Expected document for 'Alice' to be deleted");
    }

    @Test
    void countDocuments_whenDocsInserted_returnsCorrectCount() {
        usersCollection.insertMany(Arrays.asList(
            new Document("name", "Alice").append("age", 25),
            new Document("name", "Bob").append("age", 30))
        );

        long count = usersCollection.count();
        assertEquals(2, count, "Expected count of documents to be 2");
    }

    @Test
    void aggregateWithGroupAndCount_whenDocsExist_returnsAggregatedResults() {
        usersCollection.insertMany(Arrays.asList(
                new Document("name", "Alice").append("age", 25),
                new Document("name", "Alice").append("age", 30),
                new Document("name", "Bob").append("age", 35),
                new Document("name", "Bob").append("age", 40),
                new Document("name", "Charlie").append("age", 20)
        ));

        List<Document> results = usersCollection.aggregate(List.of(
                new Document("$group", new Document("_id", "$name")
                        .append("totalAge", new Document("$sum", "$age"))
                        .append("userCount", new Document("$sum", 1)))
        )).into(new ArrayList<>());

        assertEquals(3, results.size(), "Expected three groups: Alice, Bob, and Charlie");

        for (Document result : results) {
            String name = result.getString("_id");
            int totalAge = result.getInteger("totalAge");
            int userCount = result.getInteger("userCount");

            if ("Alice".equals(name)) {
                assertEquals(55, totalAge, "Expected total age for Alice to be 55");
                assertEquals(2, userCount, "Expected user count for Alice to be 2");
            } else if ("Bob".equals(name)) {
                assertEquals(75, totalAge, "Expected total age for Bob to be 75");
                assertEquals(2, userCount, "Expected user count for Bob to be 2");
            } else if ("Charlie".equals(name)) {
                assertEquals(20, totalAge, "Expected total age for Charlie to be 20");
                assertEquals(1, userCount, "Expected user count for Charlie to be 1");
            }
        }
    }

    @Test
    void aggregateComplexGroupSortProject_returnsExpectedResults() {
        usersCollection.insertMany(Arrays.asList(
                new Document("name", "Alice").append("age", 35).append("department", "Engineering"),
                new Document("name", "Bob").append("age", 25).append("department", "Engineering"),
                new Document("name", "Charlie").append("age", 24).append("department", "HR"),
                new Document("name", "Dave").append("age", 26).append("department", "HR"),
                new Document("name", "Eve").append("age", 28).append("department", "Sales")
        ));

        List<Document> pipeline = Arrays.asList(
                new Document("$group", new Document("_id", "$department")
                        .append("averageAge", new Document("$avg", "$age"))
                        .append("count", new Document("$sum", 1))),
                new Document("$sort", new Document("averageAge", -1)),
                new Document("$project", new Document("department", "$_id")
                        .append("averageAge", 1)
                        .append("count", 1)
                        .append("_id", 0))
        );

        List<Document> results = usersCollection.aggregate(pipeline).into(new ArrayList<>());

        assertEquals(3, results.size(), "Expected results for 3 departments");

        Document engineeringDept = results.get(0);
        assertEquals("Engineering", engineeringDept.getString("department"));
        assertEquals(30.0, engineeringDept.getDouble("averageAge"), 0.01, "Engineering average age mismatch");
        assertEquals(2, engineeringDept.getInteger("count"), "Engineering employee count mismatch");

        Document salesDept = results.get(1);
        assertEquals("Sales", salesDept.getString("department"));
        assertEquals(28.0, salesDept.getDouble("averageAge"), 0.01, "Sales average age mismatch");
        assertEquals(1, salesDept.getInteger("count"), "Sales employee count mismatch");

        Document hrDept = results.get(2);
        assertEquals("HR", hrDept.getString("department"));
        assertEquals(25.0, hrDept.getDouble("averageAge"), 0.01, "HR average age mismatch");
        assertEquals(2, hrDept.getInteger("count"), "HR employee count mismatch");
    }
}
