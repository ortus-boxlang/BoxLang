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

@rem goto :eof This was exiting out of the script completely without ctually starting java

@rem To Skip over the subroutine we needed to have a label for it to skip to. 
goto :postenv

@rem Subroutine: load_env_file <filepath>
@rem Reads key=value pairs from the given file and exports them as environment variables
:load_env_file
echo File Processed %~1
if not exist "%~1" goto :eof

@rem Uncomment for debugging: echo Loading environment variables from %~1
@rem setlocal enabledelayedexpansion
for /f "usebackq tokens=* delims==" %%a in ("%~1") do (
  set "line=%%a"

  @rem Skip empty lines and comments starting with #
  if not "!line!"=="" if not "!line:~0,1!"=="#" (

    :: Clean and split the key and value
    for /f "tokens=1* delims==" %%B in ("!line!") do (
      set "key=%%B"
      set "val=%%C"

      :: Remove any surrounding quotes from the value if they exist
      set "val=!val:"=!"

      for /f "tokens=1,2 delims==" %%D in ("!key!=!val!") do (
          endlocal
          set "%%D=%%E"
          setlocal EnableExtensions EnableDelayedExpansion
      )
    )
  )
  
)

@rem break out of the subroutine otherwise anything after this in the main script will run as if it is part of it.
goto :eof

@rem ######################################
@rem End .env loading
@rem ######################################

:postenv