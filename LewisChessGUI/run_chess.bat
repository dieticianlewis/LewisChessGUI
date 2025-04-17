@echo off
echo Compiling the project with Maven...
mvn clean package

if %errorlevel% neq 0 (
    echo Build failed. Exiting.
    pause
    exit /b %errorlevel%
)

echo Running the program...
java -cp target/classes;lib/chesslib-1.2.0.jar Chess
pause