for /f "tokens=1,2 delims==" %%A in (local.env) do set %%A=%%B
gradlew.bat --stop && gradlew.bat bootRun --args='--spring.profiles.active=default'