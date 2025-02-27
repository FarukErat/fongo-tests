docker run --rm \
  -v .:/app \
  -w /app \
  maven:3.9.9-eclipse-temurin-23 \
  mvn test
