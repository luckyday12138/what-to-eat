Set-Location 'C:\Users\26767\Doubao\chats\2026-11-18\new-chat\what-to-eat'
$env:JAVA_HOME='D:\edge\jdk-25.0.4.1+1'
$env:GRADLE_USER_HOME='D:\Android\.gradle'
$env:JAVA_TOOL_OPTIONS='-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dfile.encoding=UTF-8'
foreach ($i in 0..3) {
  Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -eq '今天吃什么' } | Stop-Process -Force -ErrorAction SilentlyContinue
  Start-Sleep -Seconds 2
  Start-Process cmd.exe -ArgumentList '/c',"gradlew.bat :desktopApp:run --args=`"--tab $i`" --console=plain -q" -WindowStyle Hidden
  Start-Sleep -Seconds 45
  .\_tools\bgwin.ps1 -Action size -X 420 -Y 880 | Out-Null
  Start-Sleep -Seconds 3
  .\_tools\bgwin.ps1 -Action shot -Name "tab$i"
  Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -eq '今天吃什么' } | Stop-Process -Force -ErrorAction SilentlyContinue
  Start-Sleep -Seconds 2
}
'ALL DONE'
