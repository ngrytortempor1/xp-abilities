@rem
@rem Gradle起動スクリプト for Windows
@rem

@if "%DEBUG%"=="" @echo off

@rem JAVA_HOMEの設定
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem ここでプロジェクトディレクトリを解決
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem DIRNAME末尾の\を確認
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem クラスパスの設定
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem JAVA_HOMEからjavaを探す
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2

goto fail

:execute
@rem Gradleラッパー実行
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem 終了コードを返す
exit /b %ERRORLEVEL%

:fail
exit /b 1
