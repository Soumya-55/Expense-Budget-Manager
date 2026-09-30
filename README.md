# Expense & Budget Manager
Java (JDK 17+) + JDBC + MySQL + HTML/CSS/JS. No Maven, no Tomcat, no frameworks.

## One-time setup
1. Install JDK 17 or newer (Temurin: https://adoptium.net) - check with `java -version` and `javac -version`
2. Install MySQL Server 8 + remember your root password. Make sure it is running.
3. Install VS Code + extension "Extension Pack for Java" (Microsoft).
4. Download the MySQL driver and put it in the `lib/` folder:
   https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar
5. Edit `config.properties` -> set db.user and db.password.

## Run
- VS Code: File > Open Folder > this folder. Open src/com/expense/Main.java and click "Run" above main (or F5).
- OR double-click run.bat (Windows) / ./run.sh (Mac/Linux).
Then open http://localhost:8080

The database and tables are created automatically.

## Troubleshooting
- "COULD NOT CONNECT TO MYSQL": MySQL not running / wrong password / jar missing in lib/.
- "Address already in use": change server.port in config.properties.
- Blank/404 page: app must run from the project root folder (F5 in VS Code does this).
