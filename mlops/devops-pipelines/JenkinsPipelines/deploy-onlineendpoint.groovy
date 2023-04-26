node {
    stage('InstallAzureMLCLI'){
        powershell(
        '''
        az version
        az extension add -n ml -y
        az extension update -n ml
        az extension list
        '''
        )
    }
    stage('init') {
      checkout scm
    }
    withCredentials([azureServicePrincipal(credentialsId: 'MLOps-Azure-Serviceprinciple',
    subscriptionIdVariable: 'SubscriptionID',
    clientIdVariable: 'AZURE_CLIENT_ID',
    clientSecretVariable: 'AZURE_CLIENT_SECRET',
    tenantIdVariable: 'Azure_TENANT_ID')]){
        stage('CreateResourceGroup'){

            powershell( '''
            az login --service-principal -u %AZURE_CLIENT_ID% -p %AZURE_CLIENT_SECRET% -t %Azure_TENANT_ID%
            az account set -s %SubscriptionID%
            az group create --location 'westeurope' --name 'rg-mlops-rajat-01-dev'
            '''
            )

        }

        stage('CreateAzureMLWorkspace') {
            // login Azure
            powershell(
            '''
            # az login --service-principal -u %AZURE_CLIENT_ID% -p %AZURE_CLIENT_SECRET% -t %Azure_TENANT_ID%
            # az account set -s %AZURE_SUBSCRIPTION_ID%
            Write-Host "Checking workspace mlw-mlops-rajat-01-dev" 
            $wkspcs=$(az ml workspace list -g  'rg-mlops-rajat-01-dev' --query [].display_name -o tsv )
            $ws_exists="false"
            echo "found workspaces" $wkspcs
            Foreach ($ws in $wkspcs)
            {
                if($ws -eq "mlw-mlops-rajat-01-dev")
                {
                $ws_exists="true"
                echo "Workspace mlw-mlops-rajat-01-dev already exists"
                break
                }
                
            }
            if($ws_exists -eq "false")
            {
            Write-Host "Creating Workspace mlw-mlops-rajat-01-dev rg-mlops-rajat-01-dev westeurope now .."
            az ml workspace create --name 'mlw-mlops-rajat-01-dev' -g 'rg-mlops-rajat-01-dev' -l 'westeurope'
            break
            }
                '''
            )
        }

        stage('CreateOnlineEndPoint'){
            powershell(
            '''
                    az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
                    az account set -s $AZURE_SUBSCRIPTION_ID
                    az configure --defaults group=rg-mlops-rajat-01-dev workspace='mlw-mlops-rajat-01-dev'
                    $ENDPOINT_EXISTS=$(az ml online-endpoint list -o tsv --query "[?name=='taxi-fare-online-mlops-rajat-01-dev'][name]")
                    az ml online-endpoint list -o tsv

                    if($null -eq $ENDPOINT_EXISTS)
                    {
                        az ml $(endpoint_type)-endpoint create --name 'taxi-fare-online-mlops-rajat-01-dev' -f 'mlops/azureml/deploy/online/online-endpoint.yml'
                    }

                    else
                    {
                        echo "Endpoint exists"
                    }
                    '''
            )
        }

        stage('CreateOnlineDeployment'){
            powershell(
            '''
                    az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
                    az account set -s $AZURE_SUBSCRIPTION_ID
                    az configure --defaults group=rg-mlops-rajat-01-dev workspace='mlw-mlops-rajat-01-dev'
                    az ml online-deployment create --name 'taxi-online-dp' --endpoint 'taxi-fare-online-mlops-rajat-01-dev' -f 'mlops/azureml/deploy/online/online-deployment.yml'
                    '''
            )
        }

        stage('AllocateTraffic'){
            powershell(
            '''
                    az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
                    az account set -s $AZURE_SUBSCRIPTION_ID
                    az configure --defaults group=rg-mlops-rajat-01-dev workspace='mlw-mlops-rajat-01-dev'
                    az ml online-endpoint update --name 'taxi-fare-online-mlops-rajat-01-dev' --traffic "taxi-online-dp=100"
                    '''
            )
        }

        stage('TestDeployment'){
            powershell(
            '''
                    az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
                    az account set -s $AZURE_SUBSCRIPTION_ID
                    az configure --defaults group=rg-mlops-rajat-01-dev workspace='mlw-mlops-rajat-01-dev'
                    $RESPONSE=$(az ml online-endpoint invoke -n 'taxi-fare-online-mlops-rajat-01-dev' --deployment-name 'taxi-online-dp' --request-file 'data/taxi-request.json')
                    Write-Host $RESPONSE
                    '''
            )
        }
    }
}