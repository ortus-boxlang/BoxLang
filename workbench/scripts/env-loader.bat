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
goto :runJava

@rem Subroutine: load_env_file <filepath>
@rem Reads key=value pairs from the given file and exports them as environment variables
:load_env_file

if not exist "%~1" goto :eof
@rem Uncomment for debugging: echo Loading environment variables from %~1
setlocal enabledelayedexpansion
for /f "usebackq tokens=1,* delims==" %%a in ("%~1") do (
  @rem set the variable name to be assigned to
  set "keyName=%%a";
  
  @rem get the first character of the row in order to filter out comments or empty lines
  set "fstChar=!keyName:~0,1!" 
  
  @rem set the value to be assigned
  set "val=%%b";
 
  @rem check and see if a second variable exists at all
  if defined val (
  
  	@rem filter out lines starting with `#` and empty lines
    if not "!fstChar!"=="#" if not "!fstChar!"=="~" (
  
    	@rem strip out any quotes which are in the value
      	set val=!val:"=!
  		
  		@rem set the dynamically named variable which automatically is part of the global scope and can be seen in /* at the main level
  	    set "!keyName!=!val!"
    ) else (
      endlocal
    )
  ) else (
      endlocal
  ) 
)

@rem break out of the subroutine otherwise anything after this in the main script will run as if it is part of it.
goto :eof

@rem ######################################
@rem End .env loading
@rem ######################################