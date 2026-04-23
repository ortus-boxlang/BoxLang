@echo off
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

@rem Load the user's home secrets file first, then the project-level env file
call :load_env_file "%USERPROFILE%\.box.env"
call :load_env_file "%ENV_FILE%"
goto :eof

@rem Subroutine: load_env_file <filepath>
@rem Reads key=value pairs from the given file and exports them as environment variables
:load_env_file
if not exist "%~1" goto :eof
@rem Uncomment for debugging: echo Loading environment variables from %~1
for /f "usebackq tokens=1,* delims==" %%a in ("%~1") do (
    set "line=%%a"
    setlocal enabledelayedexpansion
    @rem Skip comments and empty lines
    if not "!line:~0,1!"=="#" if not "%%a"=="" (
        @rem Remove quotes from value if present
        set "value=%%b"
        if not "!value!"=="" (
            set "value=!value:"=!"
            endlocal
            set "%%a=!value!"
        ) else (
            endlocal
        )
    ) else (
        endlocal
    )
)
goto :eof
