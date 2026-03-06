@rem
@rem Gradle wrapper startup script for Windows.
@rem
@rem This repo previously missed wrapper scripts/jars, which broke local builds.
@rem

@echo off
setlocal

set APP_HOME=%~dp0
set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
set WRAPPER_SHARED_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper-shared.jar
set WRAPPER_CLI_JAR=%APP_HOME%gradle\wrapper\gradle-cli.jar
set WRAPPER_FILES_JAR=%APP_HOME%gradle\wrapper\gradle-files.jar

if not exist "%WRAPPER_JAR%" (
  echo Missing %WRAPPER_JAR%
  exit /b 1
)

if not exist "%WRAPPER_SHARED_JAR%" (
  echo Missing %WRAPPER_SHARED_JAR%
  exit /b 1
)

if not exist "%WRAPPER_CLI_JAR%" (
  echo Missing %WRAPPER_CLI_JAR%
  exit /b 1
)

if not exist "%WRAPPER_FILES_JAR%" (
  echo Missing %WRAPPER_FILES_JAR%
  exit /b 1
)

set CLASSPATH=%WRAPPER_JAR%;%WRAPPER_SHARED_JAR%;%WRAPPER_CLI_JAR%;%WRAPPER_FILES_JAR%

if exist "%JAVA_HOME%\bin\java.exe" (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_CMD=java
)

"%JAVA_CMD%" -Dorg.gradle.appname=gradlew -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

endlocal
