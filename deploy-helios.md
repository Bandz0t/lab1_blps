# Deployment on Helios

1. **Build the JAR**
   Execute the following command in the project root to create an executable JAR file:
   ```bash
   ./gradlew bootJar
   ```
   The built JAR will be located in `build/libs/`.

2. **Configure Database Connection**
   Make sure you modify `application.properties` (or run with environment variables) to point to the Helios PostgreSQL database:
   ```properties
   spring.datasource.url=jdbc:postgresql://pg:5432/studs
   spring.datasource.username=sXXXXXX
   spring.datasource.password=your_password
   ```

3. **Transfer the JAR**
   Use `scp` to transfer the JAR to your helios account:
   ```bash
   scp build/libs/lab1-0.0.1-SNAPSHOT.jar sXXXXXX@helios.cs.ifmo.ru:~/lab1-blps/
   ```

4. **Run the Application**
   SSH into helios and run the application:
   ```bash
   ssh sXXXXXX@helios.cs.ifmo.ru
   cd ~/lab1-blps
   java -jar lab1-0.0.1-SNAPSHOT.jar
   ```

   You can also run it in the background using `nohup`:
   ```bash
   nohup java -jar lab1-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
   ```
