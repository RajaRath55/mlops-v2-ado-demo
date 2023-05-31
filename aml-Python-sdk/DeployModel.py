from azure.ai.ml import MLClient
from azure.ai.ml.entities import Workspace
from azure.identity import DefaultAzureCredential
from azure.ai.ml.entities import Data
from azure.ai.ml.entities import Environment
from azure.ai.ml import command
from azure.ai.ml import Input
from azure.ai.ml import Output
from azure.ai.ml.entities import PipelineJob

### Create ML Client for Connecting to workspace ###
subscription_id="fe6d5460-fd46-48d7-92bf-f843bb791aae"
resource_group="rg-kpmg-practice"
workspace_name = "mlw-example"
ml_client = MLClient(DefaultAzureCredential(),subscription_id, resource_group,workspace_name)

### Register Data ###
data_asset = Data(name="taxi-data",
                  path="D:/Learning/MLops-CLIV2/mlops-v2-ado-demo/data/taxi-data.csv",
                  type= "uri_file",
                  version="2")

#ml_client.data.create_or_update(data_asset)


### Create custom Environemnt ###
conda_env = Environment(name="taxi-train-env",
                        image="mcr.microsoft.com/azureml/openmpi4.1.0-ubuntu20.04:latest",
                        version="1",             
                        conda_file="D:\Learning\MLops-CLIV2\mlops-v2-ado-demo\data-science\environment/train-conda.yml")

ml_client.environments.create_or_update(conda_env)

### Create Pipeline ###
taxi_pipeline = PipelineJob()

)

### Create Job PrepareData###
prepare_data_input = Input(type="uri_file",
                           path="azureml:taxi-data@latest")

prepare_data_output = Output(train_data="",
                             val_data="",
                             test_data="",
                             trained_model="",
                             evaluation_output="",
                             model_info_output_path="")
PrepareData = command(name="prep_data",
                      display_name="prep-data",
                      code="D:\Learning\MLops-CLIV2\mlops-v2-ado-demo\data-science\src\prep",
                      environment="azureml:taxi-train-env@latest",
                      inputs=dict(raw_data=prepare_data_input)
                      command="python prep.py --raw_data inputs. --train_data ${{outputs.train_data}} --val_data ${{outputs.val_data}} --test_data ${{outputs.test_data}}"


)
