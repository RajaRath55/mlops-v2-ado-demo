// Jenkinsfile
node {
  def GITHUBCREDID    = "<github-token>"
  def CURRENTRELEASE  = "<release>"
  def DBTOKEN         = "<databricks-token>"
  def DBURL           = "https://<databricks-instance>"
  def SCRIPTPATH      = "}/Automation/Deployments"
  def NOTEBOOKPATH    = "/Workspace"
  def LIBRARYPATH     = "/Libraries"
  def BUILDPATH       = "${GITREPO}/Builds/${env.JOB_NAME}-${env.BUILD_NUMBER}"
  def OUTFILEPATH     = "${BUILDPATH}/Validation/Output"
  def TESTRESULTPATH  = "${BUILDPATH}/Validation/reports/junit"
  def WORKSPACEPATH   = "/Shared/<path>"
  def DBFSPATH        = "dbfs:<dbfs-path>"
  def CLUSTERID       = "<cluster-id>"
  def CONDAPATH       = "<conda-path>"
  def CONDAENV        = "<conda-env>"

  stage('CreateCluster') {
      withCredentials([string(credentialsId: DBTOKEN, variable: 'TOKEN')]) {
        sh """#!/bin/bash
            # Configure Conda environment for deployment & testing
            source ${CONDAPATH}/bin/activate ${CONDAENV}

            # Configure Databricks CLI for deployment
            echo "${DBURL}
            $TOKEN" | databricks configure --token
           databricks clusters create --json-file ADB/Clusters/computeengine.json
           """
      }
  }
  stage('Checkout') { // for display purposes
    checkout scm
  }
  stage('CreateCluster')
  }
}
