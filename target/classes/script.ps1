$application = New-Object -ComObject word.application
$application.Visible = $false

foreach ($arg in $args)
{
  Write-Host "Arg: $arg";
  $document = $application.documents.open("$arg")
}

$binding = "System.Reflection.BindingFlags" -as [type]
$properties = $document.BuiltInDocumentProperties
foreach($property in $properties)
{
 $pn = [System.__ComObject].invokemember("name",$binding::GetProperty,$null,$property,$null)
  trap [system.exception]
   {
     write-host -foreground blue "Value not found for $pn"
    continue
   }
  "$pn`: " +
   [System.__ComObject].invokemember("value",$binding::GetProperty,$null,$property,$null)

}

Write-Host "end report";

$application.quit()