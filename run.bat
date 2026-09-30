@echo off
if not exist lib\*.jar (echo ERROR: put mysql-connector-j jar into the lib folder first & pause & exit /b 1)
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d out @sources.txt || (pause & exit /b 1)
java -cp "out;lib/*" com.expense.Main
pause
