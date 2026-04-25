@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@setlocal enabledelayedexpansion
@rem ##########################################################################
@rem
@rem  boxlang startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and BOXLANG_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
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
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem ######################################
@rem AppCDS: auto-generate and cache a class-data archive for faster startup
@rem ######################################
if defined BOXLANG_HOME (
  set "BL_HOME_DIR=%BOXLANG_HOME%"
) else (
  set "BL_HOME_DIR="
  set "_BL_PREV_HOME=0"
  for %%A in (%*) do (
    if "!_BL_PREV_HOME!"=="1" (
      set "BL_HOME_DIR=%%~A"
      set "_BL_PREV_HOME=0"
    ) else if "%%~A"=="--bx-home" (
      set "_BL_PREV_HOME=1"
    )
  )
  if not defined BL_HOME_DIR set "BL_HOME_DIR=%USERPROFILE%\.boxlang"
)
set "BL_JSA_DIR=!BL_HOME_DIR!\cache"
for %%F in ("%APP_HOME%\lib\boxlang-*.jar") do set "BL_JAR=%%~fF" & set "BL_JARNAME=%%~nF"
if defined BL_JAR (
  set "BL_VER=!BL_JARNAME:boxlang-=!"
  set "BL_JSA=!BL_JSA_DIR!\boxlang-!BL_VER!.jsa"
  if defined JAVA_HOME (set "BL_JAVA=%JAVA_HOME%\bin\java.exe") else (set "BL_JAVA=java.exe")
  if not exist "!BL_JSA!" (
    if not exist "!BL_JSA_DIR!" mkdir "!BL_JSA_DIR!"
    set "BL_CLASSLIST=!BL_JSA_DIR!\boxlang-!BL_VER!.classlist"
    "!BL_JAVA!" -XX:DumpLoadedClassList="!BL_CLASSLIST!" -cp "%BL_JAR%" ortus.boxlang.runtime.BoxRunner --version >nul 2>&1
    "!BL_JAVA!" -Xshare:dump -XX:SharedClassListFile="!BL_CLASSLIST!" -XX:SharedArchiveFile="!BL_JSA!" -cp "%BL_JAR%" ortus.boxlang.runtime.BoxRunner >nul 2>&1
    del "!BL_CLASSLIST!" >nul 2>&1
  )
  if exist "!BL_JSA!" (
    set "JAVA_OPTS=%JAVA_OPTS% -XX:TieredStopAtLevel=1 -Xshare:auto -XX:SharedArchiveFile=!BL_JSA!"
  )
)
@rem ######################################
@rem End AppCDS
@rem ######################################



@rem ######################################
@rem Load environment variables from .env file
@rem ######################################
@rem echo off
@rem ######################################
@rem BoxLang Environment File Loader
@rem Loads environment variables from a .env file
@rem Usage: Call this script before executing BoxLang
@rem ######################################

@rem Default environment file
set "ENV_FILE=.env"
set "FILTERED_ARGS="

@rem Parse command line arguments to find --envfile parameter
for %%a in (%*) do (
    set "arg=%%a"
    setlocal enabledelayedexpansion
    if "!arg:~0,10!"=="--envfile=" (
        endlocal
        set "ENV_FILE=%%a"
        set "ENV_FILE=!ENV_FILE:~10!"
    ) else (
        endlocal
        if defined FILTERED_ARGS (
            set "FILTERED_ARGS=!FILTERED_ARGS! %%a"
        ) else (
            set "FILTERED_ARGS=%%a"
        )
    )
)
@rem Load the environment file if it exists
if exist "%ENV_FILE%" (
  @rem Uncomment for debugging: echo Loading environment variables from %ENV_FILE%
  setlocal enabledelayedexpansion
  for /F "usebackq tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
    set "keyName=%%a"
    set "val=%%b";
    if DEFINED val (
      set val=!val:"=!
      rem Skip comments and empty lines
      set "fstChar=!val:~0,1!"
      if not "!fstChar!"=="#" if not "!fstChar!"=="~" (
        set "!keyName!=!val!"
      )        
    )
  )
)


@rem ######################################
@rem End .env loading
@rem ######################################

@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\boxlang-1.12.0.jar


@rem Execute boxlang
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %BOXLANG_OPTS%  -classpath "%CLASSPATH%" ortus.boxlang.runtime.BoxRunner %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable BOXLANG_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%BOXLANG_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
