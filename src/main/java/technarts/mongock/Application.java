package technarts.mongock;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Application {
    private static long timeOperation(Runnable operation) {
        long startTime = System.nanoTime();
        operation.run();
        return (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
    }

    private static void insertDocuments(MongoCollection<Document> collection, int numDocs) {
        for (int i = 0; i < numDocs; i++) {
            collection.insertOne(new Document("_id", i).append("value", "data_" + i));
        }
    }

    private static void updateDocuments(MongoCollection<Document> collection, int numDocs) {
        for (int i = 0; i < numDocs; i++) {
            collection.updateOne(new Document("_id", i), new Document("$set", new Document("updated", true)));
        }
    }

    private static void queryDocuments(MongoCollection<Document> collection, int numDocs) {
        for (int i = 0; i < numDocs; i++) {
            collection.find(new Document("_id", i)).first();
        }
    }

    private static void deleteDocuments(MongoCollection<Document> collection, int numDocs) {
        for (int i = 0; i < numDocs; i++) {
            collection.deleteOne(new Document("_id", i));
        }
    }

    public static void main(String[] args) {
        Fongo fongo = new Fongo("mockDB");
        MongoClient mongoClient = fongo.getMongo();
        MongoDatabase database = mongoClient.getDatabase("testDB");
        MongoCollection<Document> collection = database.getCollection("users");
        int numDocs = 10_000;

        System.out.println("Testing with " + numDocs + " documents...");

        long insertTime = timeOperation(() -> insertDocuments(collection, numDocs));
        System.out.println("Insert time: " + insertTime + " ms");

        long updateTime = timeOperation(() -> updateDocuments(collection, numDocs));
        System.out.println("Update time: " + updateTime + " ms");

        long queryTime = timeOperation(() -> queryDocuments(collection, numDocs));
        System.out.println("Query time: " + queryTime + " ms");

        long deleteTime = timeOperation(() -> deleteDocuments(collection, numDocs));
        System.out.println("Delete time: " + deleteTime + " ms");
    }
}
