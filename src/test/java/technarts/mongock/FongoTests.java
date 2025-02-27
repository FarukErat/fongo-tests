package technarts.mongock;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class FongoTests {

    private Fongo fongo;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    @BeforeEach
    void setUp() {
        fongo = new Fongo("mockDB");
        mongoClient = fongo.getMongo();
        database = mongoClient.getDatabase("testDB");
        collection = database.getCollection("users");
    }

    @AfterEach
    void tearDown() {
        database.drop();
        mongoClient.close();
    }

    @Test
    void find_whenDocExists_returnsDocument() {
        Document doc = new Document("name", "Alice").append("age", 25);

        collection.insertOne(doc);
        Document found = collection.find(new Document("name", "Alice")).first();

        assertNotNull(found, "Expected to find a document with name 'Alice'");
        assertEquals("Alice", found.getString("name"), "Name should be 'Alice'");
        assertEquals(25, found.getInteger("age"), "Age should be 25");
    }

    @Test
    void find_whenDocDoesNotExist_returnsNull() {
        Document found = collection.find(new Document("name", "Bob")).first();

        assertNull(found, "Expected no document for name 'Bob'");
    }
}
