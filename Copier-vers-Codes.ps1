# A executer manuellement dans PowerShell sur le PC.
# Copie le depot sans supprimer ou remplacer un dossier existant.
[CmdletBinding()]
param([string]$Destination = 'C:\codes\NuvioDesktop-MAL')

$ErrorActionPreference = 'Stop'
$source = [IO.Path]::GetFullPath($PSScriptRoot)
$target = [IO.Path]::GetFullPath($Destination).TrimEnd('\')
if (-not $target.StartsWith('C:\codes\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'La destination doit etre un sous-dossier de C:\codes.'
}
if (Test-Path -LiteralPath $target) {
    throw "Le dossier $target existe deja. Rien n'a ete modifie."
}
New-Item -ItemType Directory -Path $target | Out-Null
$excluded = @('.gradle-user', '.gradle', '.kotlin')
foreach ($module in @('composeApp', 'androidApp', 'desktopSentry', 'composeMediaPlayer')) {
    $excluded += Join-Path $source "$module\build"
}
& robocopy.exe $source $target /E /XD $excluded /NFL /NDL /NJH /NJS
$copyResult = $LASTEXITCODE
if ($copyResult -ge 8) {
    throw "Copie incomplete (code $copyResult). Le dossier partiel est conserve dans $target."
}
Write-Host "Code copie dans $target"
Write-Host "L'executable n'est pas encore compile. Voir FORK-MAL.md."
exit 0
