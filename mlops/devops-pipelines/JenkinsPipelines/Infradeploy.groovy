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
        def resource_group_name = 'rg-$(namespace)-$(postfix)-$(environment)'
        withCredentials([usernamePassword(credentialsId: 'MLOps-ServiceConnection', passwordVariable: 'AZURE_CLIENT_SECRET', usernameVariable: 'AZURE_CLIENT_ID')]) {
        sh '''
        az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $TenantId
        az account set -s $SubscriptionID
        az group create --location $location --name $resource_group_name
        '''
        }
    }
  
  
    stage('CreateAzureMLWorkspace') {
    def  aml_workspace_name = 'mlw-$(namespace)-$(postfix)-$(environment)'
      // generate version, it's important to remove the trailing new line in git describe output
      withCredentials([usernamePassword(credentialsId: 'MLOps-ServiceConnection', passwordVariable: 'AZURE_CLIENT_SECRET', usernameVariable: 'AZURE_CLIENT_ID')]) {
        // login Azure
        sh '''
          az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $AZURE_TENANT_ID
          az account set -s $AZURE_SUBSCRIPTION_ID
        '''
         // get login server
        def acrSettingsJson = sh script: "az acr show -n $acrName", returnStdout: true
        def loginServer = getAcrLoginServer acrSettingsJson
        // login docker
        // docker.withRegistry only supports credential ID, so use native docker command to login
        // you can also use docker.withRegistry if you add a credential
        sh "docker login -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET $loginServer"
        // build image
        def imageWithTag = "$loginServer/$imageName:$version"
        def image = docker.build imageWithTag
        // push image
        image.push()
        // update web app docker settings
        sh "az webapp config container set -g $webAppResourceGroup -n $webAppName -c $imageWithTag -r http://$loginServer -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET"
        // log out
        sh 'az logout'
        sh "docker logout $loginServer"
      }
    }
  }
}