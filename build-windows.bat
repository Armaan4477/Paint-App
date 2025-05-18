@echo off
echo ===== Voting System Builder =====

set "LAUNCH4J_PATH=C:\Tools\launch4j"
set "CONFIG_XML=build_windows\launch4j-config.xml"
set "DIST_DIR=dist"
set "OUTPUT_EXE=VotingSystem.exe"

if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

echo Running Launch4j directly...
"%LAUNCH4J_PATH%\launch4jc.exe" "%CD%\%CONFIG_XML%"

if exist "%OUTPUT_EXE%" (
    echo SUCCESS: EXE created at %OUTPUT_EXE%
    if not exist "%DIST_DIR%\%OUTPUT_EXE%" (
        echo Moving EXE to dist directory...
        move "%OUTPUT_EXE%" "%DIST_DIR%\"
    )
) else (
    echo ERROR: EXE was not created
    echo Launching GUI version for troubleshooting...
    start "" "%LAUNCH4J_PATH%\launch4j.exe"
)

pause