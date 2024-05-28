@echo off
rem Copyright (C) 2020 Ping Identity Corporation
rem All rights reserved.

copy ..\wrapper\UninstallPingFederateService.bat UninstallPingFederateServiceTmp.bat > out.txt
call UninstallPingFederateServiceTmp.bat
del  UninstallPingFederateServiceTmp.bat
del  out.txt