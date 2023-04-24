pipeline {
 withEnv(['AZURE_SUBSCRIPTION_ID=SubscriptionID',
        'AZURE_TENANT_ID=TenantId']) {
    stage('init') {
      checkout scm
    }

    stage('InstallAzureMLCLI'){
        sh '''
        az version
        az extension add -n ml -y
        az extension update -n ml
        az extension list
        '''
    }

    
    stage('CreateResourceGroup'){
        def resource_group_name = 'rg-$namespace-$postfix-$environment'
        withCredentials([usernamePassword(credentialsId: 'MLOps-ServiceConnection', passwordVariable: 'AZURE_CLIENT_SECRET', usernameVariable: 'AZURE_CLIENT_ID')]) {
        sh '''
        az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
        az account set -s $SubscriptionID
        az group create --location $location --name $resource_group_name
        '''
        }
    }
  
  
    stage('CreateAzureMLWorkspace') {
    def  aml_workspace_name = 'mlw-$namespace-$postfix-$environment'
    def resource_group_name = 'rg-$namespace-$postfix-$environment'
      // generate version, it's important to remove the trailing new line in git describe output
      withCredentials([usernamePassword(credentialsId: 'MLOps-ServiceConnection', passwordVariable: 'AZURE_CLIENT_SECRET', usernameVariable: 'AZURE_CLIENT_ID')]) {
        // login Azure
        sh '''
          az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $AZURE_TENANT_ID
          az account set -s $AZURE_SUBSCRIPTION_ID

       echo "Checking workspace" $aml_workspace_name 

        # Create workspace
        wkspcs=$(az ml workspace list -g  $resource_group_name --query [].display_name -o tsv )
        ws_exists="false"

        echo "found workspaces" $wkspcs

        for ws in $wkspcs
        do
            if [[ $aml_workspace_name = $(echo $ws | tr -d '\r') ]]; then
                ws_exists="true"
                echo "Workspace $aml_workspace_name already exists"
                break
            fi
        done

        if [[ $ws_exists = "false" ]]; then
            echo "Creating Workspace $aml_workspace_name $resource_group_name $location now .."
            az ml workspace create --name $aml_workspace_name -g $resource_group_name -l $location
        fi
        '''
      }
    }

    stage('CreateMLCompute'){
        def resource_group_name = 'rg-$namespace-$postfix-$environment'
        withCredentials([usernamePassword(credentialsId: 'MLOps-ServiceConnection', passwordVariable: 'AZURE_CLIENT_SECRET', usernameVariable: 'AZURE_CLIENT_ID')]) {
        sh '''
        az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
        az account set -s $SubscriptionID
	az configure --defaults group=$resource_group_name workspace=$aml_workspace_name
        compute_name=$(az ml compute show -n $cluster_name --query name -o tsv)
        if [[ -z "$compute_name" ]]
        then
          echo "Compute does not exists. Creating the cluster..."
          az ml compute create --name $cluster_name --type amlcompute
                                  --size $size \
                                  --min-instances $min_instances \
                                  --max-instances $max_instances \
                                  --tier $cluster_tier
        else
          echo "Compute exists. Skipping cluster creation."
          exit 0
        fi
        '''
        }
    }
  }
}
