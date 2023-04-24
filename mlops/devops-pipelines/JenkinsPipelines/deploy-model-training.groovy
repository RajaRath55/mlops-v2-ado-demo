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

    
    stage('Configure Azure ML Workspace'){
    	def aml_workspace_name = 'mlw-$namespace-$postfix-$environment'
    	def resource_group_name = 'rg-$namespace-$postfix-$environment'
        withCredentials([usernamePassword(credentialsId: 'MLOps-ServiceConnection', passwordVariable: 'AZURE_CLIENT_SECRET', usernameVariable: 'AZURE_CLIENT_ID')]) {
        sh '''
        az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
        az account set -s $SubscriptionID
	az configure --defaults group=$resource_group_name workspace=$aml_workspace_name
        '''
        }
    }
  
  
    stage('Register Azure ML environment') {
    def  aml_workspace_name = 'mlw-$namespace-$postfix-$environment'
    def resource_group_name = 'rg-$namespace-$postfix-$environment'
      // generate version, it's important to remove the trailing new line in git describe output
      withCredentials([usernamePassword(credentialsId: 'MLOps-ServiceConnection', passwordVariable: 'AZURE_CLIENT_SECRET', usernameVariable: 'AZURE_CLIENT_ID')]) {
        // login Azure
        sh '''
          az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $AZURE_TENANT_ID
          az account set -s $AZURE_SUBSCRIPTION_ID
	  az ml environment create --file "mlops/azureml/train/train-env.yml"
	  az ml environment create --name $environment_name --file "mlops/azureml/train/train-env.yml"
        '''
      }
    }

    stage('Create ML Compute'){
    	def  aml_workspace_name = 'mlw-$namespace-$postfix-$environment'
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
          az ml compute create --name $cluster_name --type amlcompute --size $size --min-instances $min_instances --max-instances $max_instances --tier $cluster_tier
        else
          echo "Compute exists. Skipping cluster creation."
          exit 0
        fi
        '''
        }
    }
  }
}
