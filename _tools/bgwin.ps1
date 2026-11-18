param(
  [string]$Action = "shot",   # shot | spin | click
  [int]$X = 0,
  [int]$Y = 0,
  [string]$Name = "pw"
)

Add-Type -AssemblyName System.Drawing.Common
Add-Type @"
using System;using System.Runtime.InteropServices;
public class BhWin {
 [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
 [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
 [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint msg, IntPtr w, IntPtr l);
 public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
 public static void Click(IntPtr h, int x, int y) {
   IntPtr lp = (IntPtr)((y << 16) | (x & 0xFFFF));
   PostMessage(h, 0x0200, IntPtr.Zero, lp);
   System.Threading.Thread.Sleep(60);
   PostMessage(h, 0x0201, (IntPtr)1, lp);
   System.Threading.Thread.Sleep(110);
   PostMessage(h, 0x0202, IntPtr.Zero, lp);
 }
}
"@

$shotDir = 'C:\Users\26767\Doubao\chats\2026-11-18\new-chat\what-to-eat\_shots'
# 取「最新启动」的那个窗口：用户自己开着的 App 也在跑，别抓到他的会话
$win = Get-Process java,javaw -ErrorAction SilentlyContinue |
  Where-Object { $_.MainWindowTitle -eq '今天吃什么' -and $_.MainWindowHandle -ne 0 } |
  Sort-Object StartTime -Descending | Select-Object -First 1
if (-not $win) { "NO_WINDOW"; exit 1 }
$h = $win.MainWindowHandle

function Grab([string]$path) {
  $r = New-Object BhWin+RECT
  [void][BhWin]::GetWindowRect($h, [ref]$r)
  $w = $r.Right - $r.Left
  $ht = $r.Bottom - $r.Top
  $bmp = New-Object System.Drawing.Bitmap $w, $ht
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $hdc = $g.GetHdc()
  $ok = [BhWin]::PrintWindow($h, $hdc, 2)
  $g.ReleaseHdc($hdc)
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $g.Dispose()
  $bmp.Dispose()
  "grab=$ok ${w}x${ht}"
}

switch ($Action) {
  "shot"  { Grab "$shotDir\$Name.png" }
  "spin"  { [BhWin]::Click($h, $X, $Y); "clicked $X,$Y" }
  "click" { [BhWin]::Click($h, $X, $Y); "clicked $X,$Y" }
  "size"  {
    $r = New-Object BhWin+RECT
    [void][BhWin]::GetWindowRect($h, [ref]$r)
    $curW = $r.Right - $r.Left
    if ($curW -ne $X) {
      Add-Type @"
using System;using System.Runtime.InteropServices;
public class BhSize {
  [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);
}
"@
      [void][BhSize]::SetWindowPos($h, [IntPtr]::Zero, 48, 48, $X, $Y, 0x0004)
      "resized to $X x $Y"
    } else {
      "already $X x $Y"
    }
  }
}
