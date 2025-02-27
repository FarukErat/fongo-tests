package technarts.mongock;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Application {
    public static void main(String[] args) {
        Fongo fongo = new Fongo("mockDB");

        MongoClient mongoClient = fongo.getMongo();

        MongoDatabase database = mongoClient.getDatabase("testDB");

        MongoCollection<Document> collection = database.getCollection("users");

        Document doc = new Document("name", "Alice").append("age", 25);
        collection.insertOne(doc);

        Document found = collection.find(new Document("name", "Alice")).first();
        System.out.println(found);
    }
}
