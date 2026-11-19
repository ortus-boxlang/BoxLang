@rem ######################################
@rem Launcher config: files, then environment variables
@rem ######################################
@rem Nothing here knows about a specific module. Everything is derived from the launcher that was
@rem invoked, least specific first:
@rem   files: boxlang.{options,properties} in the BoxLang home, then <launcher>.{options,properties}
@rem   env:   BOXLANG_OPTS / BOXLANG_PROPS, then <LAUNCHER>_OPTS / <LAUNCHER>_PROPS
@rem Later sources win, because the JVM uses the last occurrence of a repeated option.
@rem A module launcher records itself in BOXLANG_PROCESS_PATH; a direct run falls back to this
@rem launcher. The BoxLang home is resolved the same way the AppCDS cache directory is.
set "_BL_CFG_JVM_ARGS="
if defined BOXLANG_HOME (
    set "_BL_CFG_HOME=%BOXLANG_HOME%"
) else (
    set "_BL_CFG_HOME="
    set "_BL_CFG_PREV_HOME=0"
    for %%A in (%*) do (
        if "!_BL_CFG_PREV_HOME!"=="1" (
            set "_BL_CFG_HOME=%%~A"
            set "_BL_CFG_PREV_HOME=0"
        ) else if "%%~A"=="--bx-home" (
            set "_BL_CFG_PREV_HOME=1"
        )
    )
    if not defined _BL_CFG_HOME set "_BL_CFG_HOME=%USERPROFILE%\.boxlang"
)

if defined BOXLANG_PROCESS_PATH (set "_BL_CFG_LAUNCHER=%BOXLANG_PROCESS_PATH%") else (set "_BL_CFG_LAUNCHER=%~f0")
for %%F in ("%_BL_CFG_LAUNCHER%") do set "_BL_CFG_BASE=%%~dpnF"
for %%F in ("%_BL_CFG_LAUNCHER%") do set "_BL_CFG_NAME=%%~nF"

@rem Prefix for the process specific env vars: box -> BOX, boxlang-miniserver -> BOXLANG_MINISERVER
@rem Environment lookups are case insensitive on Windows, so the name is used as it is on disk.
set "_BL_CFG_ENV_NAME=!_BL_CFG_NAME!"
set "_BL_CFG_ENV_NAME=!_BL_CFG_ENV_NAME:-=_!"
set "_BL_CFG_ENV_NAME=!_BL_CFG_ENV_NAME:.=_!"
set "_BL_CFG_ENV_NAME=!_BL_CFG_ENV_NAME: =_!"

@rem Skip the helper labels during normal execution
goto :bl_cfg_collect

@rem Raw JVM arguments, one per line, # for comments. Example:
@rem   -Xmx2g
@rem   --add-opens java.base/java.lang=ALL-UNNAMED
:bl_cfg_add_options
if not exist "%~1.options" goto :eof
for /f "usebackq eol=# tokens=* delims=" %%A in ("%~1.options") do (
    if not "%%A"=="" set "_BL_CFG_JVM_ARGS=!_BL_CFG_JVM_ARGS! %%A"
)
goto :eof

@rem System properties, one key=value per line, # for comments. Example:
@rem   boxlang.logging.loggers.runtime.level=TRACE
:bl_cfg_add_properties
if not exist "%~1.properties" goto :eof
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~1.properties") do (
    if not "%%A"=="" if not "%%~B"=="" set "_BL_CFG_JVM_ARGS=!_BL_CFG_JVM_ARGS! -D%%A=%%~B"
)
goto :eof

@rem Semicolon delimited key=value pairs, the same format the CommandBox property variables use.
@rem Example: boxlang.logging.loggers.runtime.level=TRACE;foo=bar
:bl_cfg_add_props_var
if not defined _BL_CFG_PROPS goto :eof
set "_BL_CFG_PIECE="
for /f "tokens=1,* delims=;" %%A in ("!_BL_CFG_PROPS!") do (
    set "_BL_CFG_PIECE=%%A"
    set "_BL_CFG_PROPS=%%B"
)
if not defined _BL_CFG_PIECE goto :eof
for /f "tokens=1,* delims==" %%A in ("!_BL_CFG_PIECE!") do (
    if not "%%A"=="" set "_BL_CFG_JVM_ARGS=!_BL_CFG_JVM_ARGS! -D%%A=%%~B"
)
goto :bl_cfg_add_props_var

:bl_cfg_collect
call :bl_cfg_add_options "%_BL_CFG_HOME%\boxlang"
call :bl_cfg_add_properties "%_BL_CFG_HOME%\boxlang"
call :bl_cfg_add_options "%_BL_CFG_BASE%"
call :bl_cfg_add_properties "%_BL_CFG_BASE%"

@rem General BoxLang layer
if defined BOXLANG_OPTS set "_BL_CFG_JVM_ARGS=!_BL_CFG_JVM_ARGS! %BOXLANG_OPTS%"
if defined BOXLANG_PROPS (
    set "_BL_CFG_PROPS=%BOXLANG_PROPS%"
    call :bl_cfg_add_props_var
)

@rem Process layer, named after the launcher that was invoked. Skipped when that launcher is
@rem boxlang itself, since BOXLANG_OPTS / BOXLANG_PROPS were just applied.
if /i not "%_BL_CFG_ENV_NAME%"=="BOXLANG" (
    call set "_BL_CFG_SPECIFIC_ARGS=%%%_BL_CFG_ENV_NAME%_OPTS%%"
    call set "_BL_CFG_SPECIFIC_PROPS=%%%_BL_CFG_ENV_NAME%_PROPS%%"
    if defined _BL_CFG_SPECIFIC_ARGS set "_BL_CFG_JVM_ARGS=!_BL_CFG_JVM_ARGS! !_BL_CFG_SPECIFIC_ARGS!"
    if defined _BL_CFG_SPECIFIC_PROPS (
        set "_BL_CFG_PROPS=!_BL_CFG_SPECIFIC_PROPS!"
        call :bl_cfg_add_props_var
    )
)
@rem ######################################
@rem End launcher config
@rem ######################################
