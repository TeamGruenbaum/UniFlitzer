for /f "tokens=1,2 delims==" %%A in (test.env) do set %%A=%%B
gradlew.bat :test --tests "de.uniflitzer.backend.UnitTests"