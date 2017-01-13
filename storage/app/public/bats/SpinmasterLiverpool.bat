@echo off
set day=-2
echo >"%temp%\%~n0.vbs" s=DateAdd("d",%day%,now) : d=weekday(s)
echo>>"%temp%\%~n0.vbs" WScript.Echo year(s)^& right(100+month(s),2)^& right(100+day(s),2)
for /f %%a in ('cscript /nologo "%temp%\%~n0.vbs"') do set "result=%%a"
del "%temp%\%~n0.vbs"
set "YYYY=%result:~0,4%"
set "MM=%result:~4,2%"
set "DD=%result:~6,2%"
set "finicial=%dd%%mm%%yyyy%"
For /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set ffinal=%%b%%a%%c)

java -jar C:\Users\Administrator\Desktop\control-robots\storage\app\public\jars\SpinmasterLiverpool.jar spinmaster 424 %finicial% %ffinal%
exit