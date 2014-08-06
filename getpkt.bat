@echo off
(adb shell "ls sdcard/*.pkt | tail -1") >> x.txt
set /p file=<x.txt
del x.txt
adb pull %file%

