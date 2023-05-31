from azure.ai.ml import MLClient
from azure.ai.ml.entities import Workspace
from azure.identity import DefaultAzureCredential
from azure.ai.ml.entities import AmlCompute

### Create ML Client for Connecting to workspace ###
subscription_id="fe6d5460-fd46-48d7-92bf-f843bb791aae"
resource_group="rg-kpmg-practice"
workspace_name = "mlw-example"
ml_client = MLClient(DefaultAzureCredential(),subscription_id, resource_group)

### Create workspace if not exists ###
ws_basic = Workspace(
    name=workspace_name,
    location="eastus",
    display_name="Basic workspace-example",
    description="This example shows how to create a basic workspace",
)
workspaceexists = False
for ws in ml_client.workspaces.list():
    if ws.name == workspace_name:
        workspaceexists = True
if workspaceexists == False :
    ml_client.workspaces.begin_create(ws_basic).result()

### Create workspace if not exists ###
compute_client = MLClient(DefaultAzureCredential(),subscription_id, resource_group,workspace_name)
cluster_basic = AmlCompute(
    name="compute-cluster",
    type="amlcompute",
    size="STANDARD_DS2_v2",
    location="eastus",
    min_instances=0,
    max_instances=1,
    idle_time_before_scale_down=120,
)
compute_client.compute.begin_create_or_update(cluster_basic).result()


