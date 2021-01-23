# az-mloverdrive

## Pull Repository via git

```powershell
:\>  git clone
```

change into the azure function directory:

```powershell
:\> cd az-mloverdrive/mloverdrive-functions
```

## Create Azure Enviroment

We will use a Prefix <YOUR-PREFIX> which will be part of each component which we create at Azure.

IMPORTANT: You need to make sure that the prefix is unique as it will also be use to create the needed Azure Storage Accounts.

NOTE: We used the YOUR-PREFIX=mloverdrive.

## Create Azure Resource Group

Create Resource group via Azure CLI.

```json
:\> az group create -l westeurope -n <PREFIX>-rg
```

## Create Azure Resources

```powershell
:\> az deployment group create --resource-group <PREFIX>-rg --mode Incremental --name create-<PREFIX> --template-file ./arm/azuredeploy.json -p prefix=<PREFIX>
```

## Retrieve Azure Resource Output Parameters

As part of the Output of the "az deployment" cmd yow will receive the following output Parameters

- STORAGE_INPUT_NAME
- STORAGE_INPUT_SASTOKEN
- SERVICEBUS_QUEUE_NAME
- FUNCTION_APP_NAME
- RESOURCE_GROUP_NAME
- APP_SERVICE_PLAN_NAME
- LOCATION

Example:

```json
"outputs": {
      "apP_SERVICE_PLAN_NAME": {
        "type": "String",
        "value": "mloverdrive-webserverfarm"
      },
      "functioN_APP_NAME": {
        "type": "String",
        "value": "mloverdrive-functionapp"
      },
      "javA_VERSION": {
        "type": "String",
        "value": "1.8"
      },
      "location": {
        "type": "String",
        "value": "westeurope"
      },
      "resourcE_GROUP_NAME": {
        "type": "String",
        "value": "mloverdrive-rg"
      },
      "servicebuS_QUEUE_NAME": {
        "type": "String",
        "value": "mloverdrive-servicebusqueue"
      },
      "storagE_INPUT_NAME": {
        "type": "String",
        "value": "mloverdriveinput.blob.core.windows.net"
      },
      "storagE_INPUT_SASTOKEN": {
        "type": "String",
        "value": "sv=2015-04-05&sr=c&se=2050-01-01T00%3A00%3A00.0000000Z&sp=acdlrw&sig=pJuqqVUQUkPiZY1mHY%2BGveybN3%2Bgwn2LeiB%2B9cwT2D0%3D"
      }
    }
```

### Modify JMETER test.properties

Replace the placeholder <STORAGE_INPUT_SASTOKEN> and <STORAGE_INPUT_NAME> inside the jmeter/test.properties with the output Parameter value:

```json
host=<STORATGE_INPUT_NAME>
sastoken=<STORAGE_INPUT_SASTOKEN>
testimage=mona.lisa.large.jpg
trump=1
tnum=1
```

### Modify JAVA Class File ServiceBusQueueHandler.java 
Replace the placeholder <SERVICE_BUS_QUEUE_NAME> inside src/main/com/cpinotossi/az/ServiceBusQueueHandler.java with the output Parameter value:

```java
/**
 * Azure Functions with Service Bus Trigger.
 */
public class ServiceBusQueueHandler {

    static final String SERVICEBUS_QUEUE_NAME = "<SERVICE_BUS_QUEUE_NAME>"; 
    static final String OUTPUT_STORAGE_CONNECTION_STRING = System.getenv("OUTPUT_STORAGE_CONNECTION_STRING");
    static final String INPUT_STORAGE_CONNECTION_STRING = "INPUT_STORAGE_CONNECTION_STRING";

```

### Modify Maven Project file pom.xml

Replace the placeholder

- <FUNCTION_APP_NAME>
- <RESOURCE_GROUP_NAME>
- <APP_SERVICE_PLAN_NAME>
- <LOCATION>

inside pom.xml with the output Parameter value:

```xml
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <java.version>1.8</java.version>
        <javaVersion>8</javaVersion>
        <azure.functions.maven.plugin.version>1.9.0</azure.functions.maven.plugin.version>
        <azure.functions.java.library.version>1.4.0</azure.functions.java.library.version>
        <functionAppName><FUNCTION_APP_NAME></functionAppName>
        <stagingDirectory>${project.build.directory}/azure-functions/${functionAppName}</stagingDirectory>
        <resourceGroupName><RESOURCE_GROUP_NAME></resourceGroupName>
        <appServicePlanName><APP_SERVICE_PLAN_NAME></appServicePlanName>
        <location><LOCATION></location>
        <os>windows</os>
    </properties>
```

## Build the JAR file

```powershell
mvn clean package
```

## Deploy and Run on Azure

```powershell
mvn azure-functions:deploy
```

## Retrieve Logs

```powershell
func azure functionapp logstream <FUNCTION_APP_NAME>
```

## Setup the Load Test with JMeter

We did setup a Windows 10 VM at Azure, at the same Region as our current Deployment.
Install JMeter on Windows 10 VM:

```powershell
C:\> Set-ExecutionPolicy RemoteSigned -scope CurrentUser
C:\> iex (new-object net.webclient).downloadstring('https://get.scoop.sh')
C:\> scoop install git
C:\> scoop bucket add java
C:\> scoop install openjdk
C:\> scoop bucket add extras
C:\> scoop install extras/jmeter
C:\> jmeter
```

## Upload JMeter Content

Upload the content of the folder "jmeter" to the VM.

## Run your Test

Don't use GUI mode for load testing !, only for Test creation and Test debugging.
For load testing, use CLI Mode (was NON GUI):
From inside the folder jmeter execute the following command:


```powershell
jmeter -n -t test.jmx -p test.properties
```

NOTE: The used Test Image of Mona Lisa has been originally been downloaded from here:

- http://ww1.prweb.com/prfiles/2010/01/26/134434/LDV1497.jpg

## Find the relevant logs inside the analytic worspace

Inside the Log Analytic Worspache <PREFIX>-insightsworkspace you can use the following command to retrieve the corresponding Blob Storage Logs:

```bash
StorageBlobLogs
| where TimeGenerated > ago(24h)
| where (OperationName == "PutBlob")
| limit 100
```

### Clean up

```powershell
az deployment group create --resource-group <PREFIX>-rg --mode Complete --name delete-<PREFIX> --template-file ./arm/empty.json
```

## Usefull Links:
- https://github.com/Azure/azure-functions-java-worker/tree/master
