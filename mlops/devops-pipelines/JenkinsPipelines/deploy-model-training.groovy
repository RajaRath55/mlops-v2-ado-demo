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
            az login --service-principal -u %AZURE_CLIENT_ID% -p %AZURE_CLIENT_SECRET% -t %Azure_TENANT_ID%
            az account set -s %AZURE_SUBSCRIPTION_ID%
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

        stage('Create custom environment'){
            powershell(
            '''
                    az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
                    az account set -s $AZURE_SUBSCRIPTION_ID
                    az configure --defaults group=rg-mlops-rajat-01-dev workspace='mlw-mlops-rajat-01-dev'
                    az ml environment create --name 'taxi-train-env' --file 'mlops/azureml/train/train-env.yml'
                    '''
            )
        }

        stage('CreateMLCompute'){
            powershell(
            '''
                    az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
                    az account set -s $AZURE_SUBSCRIPTION_ID
                    az configure --defaults group=rg-mlops-rajat-01-dev workspace='mlw-mlops-rajat-01-dev'
                    $compute_name=$(az ml compute show -n 'cpu-cluster' --query name -o tsv)
                    if ($null -eq $compute_name)
                    {
                        Write-Host "Compute does not exists. Creating the cluster..."
                        az ml compute create --name 'cpu-cluster' --type amlcompute --size 'STANDARD_D2_V2' --min-instances 0 --max-instances 1 --tier dedicated
                    }

                    else
                    {
                        Write-Host "Compute exists. Skipping cluster creation."
                    }
                    '''
            )
        }

        stage('Register data'){
            powershell(
            '''
                    az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
                    az account set -s $AZURE_SUBSCRIPTION_ID
                    az configure --defaults group=rg-mlops-rajat-01-dev workspace='mlw-mlops-rajat-01-dev'
                    az ml data create --file 'mlops/azureml/train/data.yml' --name 'taxi-data' --type 'uri_file'
            '''
            )
        }

        stage('Run Pipeline'){
            powershell(
            '''
                    az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
                    az account set -s $AZURE_SUBSCRIPTION_ID
                    az configure --defaults group=rg-mlops-rajat-01-dev workspace='mlw-mlops-rajat-01-dev'
                    $run_id=$(az ml job create -f 'mlops/azureml/train/pipeline.yml' \
                    --set experiment_name='dev_taxi_fare_train' \
                    inputs.subscriptionId=%AZURE_SUBSCRIPTION_ID% \
                    inputs.rg_group='rg-mlops-rajat-01-dev' \
                    inputs.workspace='mlw-mlops-rajat-01-dev' \
                    display_name=dev_taxi_fare_run_rajat --query name -o tsv)

                    if($null -eq $run_id)
                    {
                        Write-Host "Job creation failed"
                        exit 3
                    }

                    az ml job show -n $run_id --web
                    $status=$(az ml job show -n $run_id --query status -o tsv)

                    if($null -eq $status)
                    {
                        Write-Host "Status query failed"
                        exit 4
                    }

                    while($status -ne "Completed")
                    {
                        sleep 15 
                        $status=$(az ml job show -n $run_id --query status -o tsv)
                        Write-Host $status
                    }
                    if($status -ne "Completed")
                    {
                        Write-Host "Training Job failed or canceled"
                        exit 3
                    }
            '''
            )
        }
    }
}
