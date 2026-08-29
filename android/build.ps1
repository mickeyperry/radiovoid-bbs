# RadioVoid BBS — native app build (no Gradle, no Android Studio)
# Uses the portable toolchain in ..\tools (JDK 17, build-tools 34, platform 34).
$ErrorActionPreference = 'Stop'
$A = $PSScriptRoot
$T = (Resolve-Path "$A\..\tools").Path
$JDK = "$T\jdk\jdk-17.0.20.1+1"
$BT = "$T\sdk\build-tools\34.0.0"
$AJAR = "$T\sdk\platforms\android-34\android.jar"
$OUT = "$A\build"
$env:JAVA_HOME = $JDK
$env:PATH = "$JDK\bin;$BT;$env:PATH"

Remove-Item -Recurse -Force $OUT -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$OUT\gen","$OUT\classes" | Out-Null

Write-Host "[1/6] aapt2 compile resources"
& "$BT\aapt2.exe" compile --dir "$A\res" -o "$OUT\res.zip"
if ($LASTEXITCODE) { throw "aapt2 compile failed" }

Write-Host "[2/6] aapt2 link"
& "$BT\aapt2.exe" link -o "$OUT\base.apk" -I $AJAR --manifest "$A\AndroidManifest.xml" -A "$A\assets" --java "$OUT\gen" --auto-add-overlay "$OUT\res.zip"
if ($LASTEXITCODE) { throw "aapt2 link failed" }

Write-Host "[3/6] javac"
$srcs = Get-ChildItem -Recurse "$A\src","$OUT\gen" -Filter *.java | Select-Object -ExpandProperty FullName
& "$JDK\bin\javac.exe" -source 8 -target 8 -bootclasspath $AJAR -encoding UTF-8 -nowarn -d "$OUT\classes" @srcs 2>&1 | Where-Object { $_ -match 'error' } | Select-Object -First 25
if ($LASTEXITCODE) { throw "javac failed" }

Write-Host "[4/6] d8 dex"
& "$JDK\bin\jar.exe" --create --file "$OUT\classes.jar" -C "$OUT\classes" .
if ($LASTEXITCODE) { throw "jar failed" }
& "$BT\d8.bat" --release --lib $AJAR --min-api 26 --output $OUT "$OUT\classes.jar"
if ($LASTEXITCODE) { throw "d8 failed" }

Write-Host "[5/6] package + zipalign"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::Open("$OUT\base.apk", 'Update')
$e = $zip.CreateEntry('classes.dex'); $s = $e.Open()
$b = [IO.File]::ReadAllBytes("$OUT\classes.dex"); $s.Write($b, 0, $b.Length); $s.Close()
foreach ($so in Get-ChildItem -Recurse "$A\lib" -Filter *.so) {
    $rel = 'lib/' + $so.Directory.Name + '/' + $so.Name
    $e = $zip.CreateEntry($rel); $s = $e.Open()
    $b = [IO.File]::ReadAllBytes($so.FullName); $s.Write($b, 0, $b.Length); $s.Close()
}
$zip.Dispose()
& "$BT\zipalign.exe" -f 4 "$OUT\base.apk" "$OUT\aligned.apk"
if ($LASTEXITCODE) { throw "zipalign failed" }

Write-Host "[6/6] sign"
$KS = "$A\radiovoid.keystore"
if (-not (Test-Path $KS)) {
    & "$JDK\bin\keytool.exe" -genkeypair -keystore $KS -storepass radiovoid -keypass radiovoid -alias rv -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=RadioVoid BBS"
}
& "$BT\apksigner.bat" sign --ks $KS --ks-pass pass:radiovoid --ks-key-alias rv --out "$A\RadioVoid-BBS.apk" "$OUT\aligned.apk"
if ($LASTEXITCODE) { throw "apksigner failed" }
Write-Host "OK -> $A\RadioVoid-BBS.apk ($([int]((Get-Item "$A\RadioVoid-BBS.apk").Length/1KB)) KB)"
