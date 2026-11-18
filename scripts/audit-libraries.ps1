param([string]$LibraryRoot = (Join-Path $PSScriptRoot '../../libraries'))
$ErrorActionPreference = 'Stop'
$taskLibraryRoot = (Resolve-Path -LiteralPath $LibraryRoot).Path
$taskAndroidRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskModules = @('megalodonte-core','megalodonte-base-desktop','megalodonte-components','megalodonte-reactivity','megalodonte-router','megalodonte-theme')
$taskPortable = @('megalodonte.ComputedState','megalodonte.ListenerManager','megalodonte.ForEachState','megalodonte.v2.ListState')
$taskInventory = foreach ($taskModule in $taskModules) {
    $taskSources = Join-Path $taskLibraryRoot "$taskModule/src/main/java"
    foreach ($taskPath in (& rg --files $taskSources -g '*.java')) {
        if ([IO.Path]::GetFileName($taskPath) -eq 'module-info.java') { continue }
        $taskSource = Get-Content -LiteralPath $taskPath -Raw
        $taskPackage = [regex]::Match($taskSource, '(?m)^package\s+([\w.]+);').Groups[1].Value
        $taskClass = [IO.Path]::GetFileNameWithoutExtension($taskPath)
        $taskName = "$taskPackage.$taskClass"
        $taskImports = @([regex]::Matches($taskSource, '(?m)^import\s+(?:static\s+)?([\w.*]+);') | ForEach-Object { $_.Groups[1].Value })
        $taskPlatformImports = @($taskImports | Where-Object { $_ -match '^(javafx|java\.awt|android)\.' })
        $taskJvmSpecific = @($taskImports | Where-Object { $_ -match '^(org\.slf4j|java\.desktop|javax\.swing)\.' })
        $taskBody = [regex]::Replace($taskSource, '(?s)/\*.*?\*/', '')
        $taskMembers = @([regex]::Matches($taskBody, '(?m)^\s*public\s+(?:(?:static|final|abstract|default|synchronized)\s+)*(?:<[^\n]+?>\s+)?[^;={}\n]+?\([^;{}]*\)(?:\s+throws\s+[^{;]+)?(?=\s*[{;])') | ForEach-Object { [regex]::Replace($_.Value.Trim(), '\s+', ' ') })
        $taskCounterpart = Join-Path $taskAndroidRoot ('framework-core/src/main/java/' + $taskName.Replace('.', '/') + '.java')
        $taskStatus = if ($taskModule -in @('megalodonte-core','megalodonte-theme')) { 'shared-artifact' }
            elseif ($taskName -in $taskPortable) { 'same-source-portable-artifact' }
            elseif (Test-Path -LiteralPath $taskCounterpart) { 'android-adapter-partial-api' }
            elseif ($taskPlatformImports.Count) { 'platform-adapter-required' }
            else { 'neutral-candidate-review-transitive-dependencies' }
        [pscustomobject]@{
            module = $taskModule; type = $taskName
            source = $taskPath.Substring($taskLibraryRoot.Length + 1).Replace('\', '/')
            status = $taskStatus; platformImports = $taskPlatformImports
            jvmSpecificImports = $taskJvmSpecific
            frameworkImports = @($taskImports | Where-Object { $_ -match '^megalodonte\.' })
            publicMemberDeclarations = $taskMembers
        }
    }
}
$taskReport = [ordered]@{
    schemaVersion = 1
    note = 'Static source inventory. Status and declarations do not certify full API or behavioral parity; transitive/platform signatures require review.'
    types = @($taskInventory | Sort-Object module,type)
}
$taskDestination = Join-Path $taskAndroidRoot 'docs/library-inventory.json'
$taskReport | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $taskDestination -Encoding utf8
$taskInventory | Group-Object module | Select-Object Name,Count
