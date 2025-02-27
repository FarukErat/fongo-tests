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

    @Test
    void aggregateMatch_whenDocsExist_returnsMatchedDocuments() {
        collection.insertMany(Arrays.asList(
            new Document("name", "Alice").append("age", 25),
            new Document("name", "Bob").append("age", 30),
            new Document("name", "Charlie").append("age", 20)
        ));

        List<Document> results = collection.aggregate(Arrays.asList(
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
        collection.insertOne(doc);

        Document update = new Document("$set", new Document("age", 26));
        collection.updateOne(new Document("name", "Alice"), update);

        Document updatedDoc = collection.find(new Document("name", "Alice")).first();
        assertNotNull(updatedDoc, "Expected to find updated document for 'Alice'");
        assertEquals(26, updatedDoc.getInteger("age"), "Age should be updated to 26");
    }

    @Test
    void delete_whenDocExists_removesDocument() {
        Document doc = new Document("name", "Alice").append("age", 25);
        collection.insertOne(doc);

        collection.deleteOne(new Document("name", "Alice"));

        Document deletedDoc = collection.find(new Document("name", "Alice")).first();
        assertNull(deletedDoc, "Expected document for 'Alice' to be deleted");
    }

    @Test
    void countDocuments_whenDocsInserted_returnsCorrectCount() {
        collection.insertMany(Arrays.asList(
            new Document("name", "Alice").append("age", 25),
            new Document("name", "Bob").append("age", 30))
        );

        long count = collection.count();
        assertEquals(2, count, "Expected count of documents to be 2");
    }
}
