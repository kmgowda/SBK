@rem Copyright (c) KMG. All Rights Reserved.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem     http://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@echo off
set "SBK_JAVA_INSTALL=%~1"
if not defined SBK_JAVA_INSTALL set "SBK_JAVA_INSTALL=true"
set "SBK_JAVA_RESOLVED_HOME="
for /f "usebackq delims=" %%J in (`powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0sbk-java-bootstrap.ps1" -InstallIfMissing %SBK_JAVA_INSTALL%`) do set "SBK_JAVA_RESOLVED_HOME=%%J"
if errorlevel 1 exit /b 1
if not defined SBK_JAVA_RESOLVED_HOME exit /b 1
set "SBK_JAVA_HOME=%SBK_JAVA_RESOLVED_HOME%"
set "JAVA_EXE=%SBK_JAVA_HOME%\bin\java.exe"
exit /b 0
