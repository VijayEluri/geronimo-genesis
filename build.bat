@if "%DEBUG%" == "" @echo off
@rem 
@rem $Id$
@rem 

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

:init
@rem Get command-line arguments, handling Windowz variants
if not "%OS%" == "Windows_NT" goto win9xME_args
if "%eval[2+2]" == "4" goto 4NT_args

@rem Regular WinNT shell
set CMD_LINE_ARGS=%*
goto execute

:win9xME_args
@rem Slurp the command line arguments.  This loop allows for an unlimited number
set CMD_LINE_ARGS=

:win9xME_args_slurp
if "x%1" == "x" goto execute
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto win9xME_args_slurp

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute

mvn -Dstage=bootstrap %CMD_LINE_ARGS%
mvn %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal

@rem Optional pause the batch file
if "%DBCOPY_BATCH_PAUSE%" == "on" pause
