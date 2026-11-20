@echo off
@rem ######################################
@rem BoxLang Environment File Loader
@rem Loads environment variables from a .env file
@rem Usage: Call this script before executing BoxLang
@rem ######################################

@rem Default environment file
set "ENV_FILE=.env"

@rem Read --envfile=<path> out of the raw argument string.
@rem The arguments are never rebuilt and never forwarded from here: cmd splits every argument
@rem containing an equal sign when it assigns positional parameters or iterates a "for" set, so
@rem the value is read from %* one space delimited token at a time instead. BoxLang discards the
@rem flag itself, which means the arguments reach it exactly as they were typed.
set "_BL_EF_REMAINING=%*"

:bl_envfile_scan
if not defined _BL_EF_REMAINING goto :bl_envfile_scan_done
for /f "tokens=1,* delims= " %%A in ("!_BL_EF_REMAINING!") do (
    set "_BL_EF_TOKEN=%%A"
    set "_BL_EF_REMAINING=%%B"
)
if /i "!_BL_EF_TOKEN:~0,10!"=="--envfile=" set "ENV_FILE=!_BL_EF_TOKEN:~10!"
goto :bl_envfile_scan
:bl_envfile_scan_done
set "_BL_EF_TOKEN="
set "_BL_EF_REMAINING="

@rem The env files are NOT loaded here. The launcher calls :load_env_file itself, after it clears
@rem its own local scope, so the values land in the environment the JVM inherits. Loading them at
@rem this point would put them in a scope the launcher discards before Java starts.

@rem goto :eof This was exiting out of the script completely without ctually starting java

@rem To Skip over the subroutine we needed to have a label for it to skip to. 
goto :postenv

@rem Subroutine: load_env_file <filepath>
@rem Reads key=value pairs from the given file and exports them as environment variables
:load_env_file
if not exist "%~1" goto :eof

@rem Uncomment for debugging: echo Loading environment variables from %~1
@rem eol=# skips comment lines, tokens=1,* keeps any further equal signs in the value, and %%~B
@rem drops surrounding quotes. No setlocal/endlocal dance is needed, which is what makes the
@rem values survive into the JVM environment.
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~1") do if not "%%A"=="" if not "%%~B"=="" set "%%A=%%~B"

@rem break out of the subroutine otherwise anything after this in the main script will run as if it is part of it.
goto :eof

@rem ######################################
@rem End .env loading
@rem ######################################

:postenv