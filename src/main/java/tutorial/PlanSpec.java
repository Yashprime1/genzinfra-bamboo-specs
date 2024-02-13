package tutorial;

import com.atlassian.bamboo.specs.api.BambooSpec;
import com.atlassian.bamboo.specs.api.builders.deployment.Deployment;
import com.atlassian.bamboo.specs.api.builders.deployment.Environment;
import com.atlassian.bamboo.specs.api.builders.deployment.ReleaseNaming;
import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.builders.task.CheckoutItem;
import com.atlassian.bamboo.specs.builders.task.CleanWorkingDirectoryTask;
import com.atlassian.bamboo.specs.builders.task.InjectVariablesTask;
import com.atlassian.bamboo.specs.builders.task.ScriptTask;
import com.atlassian.bamboo.specs.builders.task.VcsCheckoutTask;
import com.atlassian.bamboo.specs.model.task.InjectVariablesScope;
import com.atlassian.bamboo.specs.util.BambooServer;

/**
 * Plan configuration for Bamboo.
 *
 * @see <a href="https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs">Bamboo Specs</a>
 */
@BambooSpec
public class PlanSpec {

    /**
     * Run 'main' to publish your plan.
     */
    public static void main(String[] args) throws Exception {
        // by default credentials are read from the '.credentials' file
        BambooServer bambooServer = new BambooServer("http://13.232.252.237:8085");

        Plan plan1 = new PlanSpec().createPlan1("Dashboard","DASH");
        Plan plan2 = new PlanSpec().createPlan2("Notification-Backend","NB");
        Plan plan3 = new PlanSpec().createPlan3("Automated-Tests","AUTOTEST");

        Deployment deployment1=new PlanSpec().createDeploymentProject1("PROJ","DASH");
        Deployment deployment2=new PlanSpec().createDeploymentProject2("PROJ","NB");
        
        bambooServer.publish(plan1);
        bambooServer.publish(plan2);
        bambooServer.publish(plan3);
        

        bambooServer.publish(deployment1);
        bambooServer.publish(deployment2);

        // PlanPermissions planPermission = new PlanSpec().createPlanPermission(plan.getIdentifier());
        // bambooServer.publish(planPermission);
    }

    // PlanPermissions createPlanPermission(PlanIdentifier planIdentifier) {
    //     Permissions permissions = new Permissions()
    //             .userPermissions("admin", PermissionType.ADMIN)
    //             .groupPermissions("bamboo-admin", PermissionType.ADMIN)
    //             .loggedInUserPermissions(PermissionType.BUILD)
    //             .anonymousUserPermissionView();

    //     return new PlanPermissions(planIdentifier)
    //             .permissions(permissions);
    // }

    Project project() {
        return new Project()
                .name("My Project")
                .key("PROJ");
    }

    Plan createPlan1(String PlanName, String PlanKey) {
        Plan plan = new Plan(project(), PlanName, PlanKey)
                .description("Plan created for Dashboard")
                .linkedRepositories("ultron");
        plan.stages(
            new Stage("Clone, Build and Deploy").jobs(
                 new Job("Clone,Build and Deploy","JOB1").tasks(
                    new VcsCheckoutTask()
                        .description("Checkout Repo")
                        .checkoutItems(new CheckoutItem().repository("ultron"))
                        .cleanCheckout(true),        
                    new ScriptTask()
                        .description("Docker Buld and Push Image")
                        .interpreterBinSh()
                        .fileFromPath("dockerBuildAndPush.sh")
                        .argument("dashboard")
                 )
            )
        );
        
        return plan;
    }

    Plan createPlan2(String PlanName, String PlanKey) {
        Plan plan = new Plan(project(), PlanName, PlanKey)
                .description("Plan created for Notification Backend")
                .linkedRepositories("ultron");
        plan.stages(
            new Stage("Clone, Build and Deploy").jobs(
                 new Job("Clone,Build and Deploy","JOB1").tasks(
                    new VcsCheckoutTask()
                        .description("Checkout Repo")
                        .checkoutItems(new CheckoutItem().repository("ultron"))
                        .cleanCheckout(true),
                    new ScriptTask()
                        .description("Docker Buld and Push Image")
                        .interpreterBinSh()
                        .fileFromPath("dockerBuildAndPush.sh")
                        .argument("notificationbackend")
                 )
            )
        );
        return plan;
    }

    Plan createPlan3(String PlanName, String PlanKey) {
        Plan plan = new Plan(project(), PlanName, PlanKey)
                .description("Plan created for Automated Tests")
                .linkedRepositories("ultron","cloudformation");

        plan.stages(
            new Stage("Stage 1 : Trigger Component Builds").jobs(
                 new Job("Trigger Dashboard Build","BUILDDASHJOB").tasks(
                    new ScriptTask()
                        .description("Trigger DASH Plan")
                        .interpreterBinSh()
                        .inlineBody("#!/bin/bash\n" +
                                    "echo $bamboo_clienttoken\n" +
                                    "DashBuildResultKey=$(curl --request POST --url 'http://13.232.252.237:8085/rest/api/latest/queue/PROJ-DASH' --header \"Authorization: Bearer $bamboo_clienttoken\" --header 'Accept: application/json' --header 'Content-Type: application/json' --data '{}' | jq -r '.buildResultKey')\n" +
                                    "echo DashBuildResultKey=$DashBuildResultKey >> ../variables.txt\n" +
                                    "buildState=$(curl --request GET --url \"http://13.232.252.237:8085/rest/api/latest/result/$DashBuildResultKey\" --header \"Authorization: Bearer $bamboo_clienttoken\" --header 'Accept: application/json' | jq -r '.buildState' ) \n" +
                                    "echo $buildState\n"+
                                    "while [[ \"$buildState\" == \"Unknown\" ]]\n"+
                                    "do\n"+
                                      "buildState=$(curl --request GET --url \"http://13.232.252.237:8085/rest/api/latest/result/$DashBuildResultKey\" --header \"Authorization: Bearer $bamboo_clienttoken\"  --header 'Accept: application/json' | jq -r '.buildState' ) \n" +
                                    "done\n" +
                                    "if [[ \"$buildState\" == \"Successful\" ]];then\n"+
                                        "echo \"Dashboard Build Completed Successfully\"\n"+
                                        "exit 0\n"+
                                    "else\n"+
                                        "echo \"Dashboard Build Failed\"\n"+
                                        "exit 1\n"+
                                    "fi"
                                ),   
                    new ScriptTask()
                    .description("Trigger DASH Plan")
                    .interpreterBinSh()
                    .inlineBody("#!/bin/bash\n" +
                                "pwd\n"+
                                "ls"
                            ),   
                    new InjectVariablesTask()
                        .path("../variables.txt")
                        .namespace("yash")
                        .scope(InjectVariablesScope.RESULT)
                 ),
                 new Job("Trigger NB Build","BUILDNBJOB").tasks(
                    new ScriptTask()
                        .description("Trigger NB Plan")
                        .interpreterBinSh()
                        .inlineBody("#!/bin/bash\n" +
                                    "echo $bamboo_clienttoken\n" +
                                    "NbBuildResultKey=$(curl --request POST --url 'http://13.232.252.237:8085/rest/api/latest/queue/PROJ-NB' --header \"Authorization: Bearer $bamboo_clienttoken\" --header 'Accept: application/json' --header 'Content-Type: application/json' --data '{}' | jq -r '.buildResultKey')\n" +
                                    "echo NbBuildResultKey=$NbBuildResultKey >> ../variables.txt\n" +
                                    "buildState=$(curl --request GET --url \"http://13.232.252.237:8085/rest/api/latest/result/$NbBuildResultKey\" --header \"Authorization: Bearer $bamboo_clienttoken\" --header 'Accept: application/json' | jq -r '.buildState' ) \n" +
                                    "echo $buildState\n"+
                                    "while [[ \"$buildState\" == \"Unknown\" ]]\n"+
                                    "do\n"+
                                      "buildState=$(curl --request GET --url \"http://13.232.252.237:8085/rest/api/latest/result/$NbBuildResultKey\" --header \"Authorization: Bearer $bamboo_clienttoken\"  --header 'Accept: application/json' | jq -r '.buildState' ) \n" +
                                    "done\n" +
                                    "if [[ \"$buildState\" == \"Successful\" ]];then\n"+
                                        "echo \"NB Build Completed Successfully\"\n"+
                                        "exit 0\n"+
                                    "else\n"+
                                        "echo \"NB Build Failed\"\n"+
                                        "exit 1\n"+
                                    "fi"   
                                ),
                    new InjectVariablesTask()
                        .path("variables.txt")
                        .namespace("yash")
                        .scope(InjectVariablesScope.RESULT) 
                 )
            ),
            new Stage("Stage 2 : Trigger Component Deployments").jobs(
                 new Job("Trigger Dashboard Deployment","DEPLOYDASHJOB").tasks(
                    new InjectVariablesTask()
                        .path("../variables.txt")
                        .namespace("yash")
                        .scope(InjectVariablesScope.RESULT),
                    new ScriptTask()
                        .description("Trigger DASH Deployment")
                        .interpreterBinSh()
                        .inlineBody("#!/bin/bash\n" +
                                    "echo $bamboo_clienttoken\n" +
                                    "cat ../variables.txt\n" +
                                    "echo $bamboo_yash_DashBuildResultKey\n" +
                                    "echo '{\"planResultKey\" : \"'${bamboo_yash_DashBuildResultKey}'\", \"name\" : \"'release-$bamboo_yash_DashBuildResultKey'\"}' > data.json\n" +
                                    "cat data.json\n" +
                                    "version=$(curl -X POST 'http://13.232.252.237:8085/rest/api/latest/deploy/project/1015809/version' --header \"Authorization: Bearer $bamboo_clienttoken\"  -H \"Accepts: application/json\" -H \"Content-Type: application/json\" --data-raw \"$(cat data.json)\" | jq -r '.id')\n" + 
                                    "curl -X POST \"http://13.232.252.237:8085/rest/api/latest/queue/deployment/?environmentId=1081345&versionId=$version\" --header \"Authorization: Bearer $bamboo_clienttoken\" -H \"Accepts: application/json\" | jq '.'\n" +
                                    "if [[ \"$buildState\" == \"Successful\" ]];then\n"+
                                        "echo \"Dashboard Build Completed Successfully\"\n"+
                                        "exit 0\n"+
                                    "else\n"+
                                        "echo \"Dashboard Build Failed\"\n"+
                                        "exit 1\n"+
                                    "fi"
                                )                                
                 ),
                 new Job("Trigger NB Deployment","DEPLOYNBJOB").tasks(
                    new ScriptTask()
                        .description("Trigger NB Deployment")
                        .interpreterBinSh()
                        .inlineBody("#!/bin/bash\n" +
                                    "echo $bamboo_clienttoken\n" +
                                    "echo $bamboo_yash_NbBuildResultKey\n" +
                                    "echo '{\"planResultKey\" : \"'${bamboo_yash_NbBuildResultKey}'\", \"name\" : \"'release-$bamboo_yash_NbBuildResultKey'\"}' > data.json\n" +
                                    "cat data.json\n" +
                                    "version=$(curl -X POST 'http://13.232.252.237:8085/rest/api/latest/deploy/project/1015810/version' --header \"Authorization: Bearer $bamboo_clienttoken\"  -H \"Accepts: application/json\" -H \"Content-Type: application/json\" --data-raw \"$(cat data.json)\" | jq -r '.id')\n" + 
                                    "deployresulturl=$(curl -X POST \"http://13.232.252.237:8085/rest/api/latest/queue/deployment/?environmentId=1081346&versionId=$version\" --header \"Authorization: Bearer $bamboo_clienttoken\" -H \"Accepts: application/json\" | jq -r '.link.href')\n" +
                                    "if [[ \"$buildState\" == \"Successful\" ]];then\n"+
                                        "echo \"Dashboard Build Completed Successfully\"\n"+
                                        "exit 0\n"+
                                    "else\n"+
                                        "echo \"Dashboard Build Failed\"\n"+
                                        "exit 1\n"+
                                    "fi"
                                )    
                 )
            )
        );
        return plan;
    }

    Deployment createDeploymentProject1(String ProjectKey,String PlanKey) {
        Deployment deployment = new Deployment(new PlanIdentifier(ProjectKey, PlanKey), "Deployment-"+ProjectKey+PlanKey)
                                    .releaseNaming(new ReleaseNaming("release-1.1")
                                        .autoIncrement(true))
                                        .environments(new Environment("Mu-Dash")
                                        .tasks( 
                                            new CleanWorkingDirectoryTask()
                                            .description("Clean the working directory")
                                            .enabled(true),
                                        new ScriptTask()
                                            .description("Clone Repo")
                                            .interpreterBinSh()
                                            .inlineBody("git clone https://x-access-token:$bamboo_gittoken@github.com/Yashprime1/genzinfra-cloudformation.git"),       
                                        new ScriptTask()
                                            .description("Replace Tags")
                                            .interpreterBinSh()
                                            .fileFromPath("genzinfra-cloudformation/scripts/deploy.sh"),
                                        new CleanWorkingDirectoryTask()
                                            .description("Clean the working directory")
                                            .enabled(true)
                                        ));
        return deployment;
    }

    Deployment createDeploymentProject2(String ProjectKey,String PlanKey) {
        Deployment deployment = new Deployment(new PlanIdentifier(ProjectKey, PlanKey),  "Deployment-"+ProjectKey+PlanKey)
                                    .releaseNaming(new ReleaseNaming("release-1.1")
                                        .autoIncrement(true))
                                        .environments(
                                            new Environment("Mu-Nb")
                                            .tasks(
                                                new CleanWorkingDirectoryTask()
                                                    .description("Clean the working directory")
                                                    .enabled(true),
                                                new ScriptTask()
                                                    .description("Clone Repo")
                                                    .interpreterBinSh()
                                                    .inlineBody("git clone https://x-access-token:$bamboo_gittoken@github.com/Yashprime1/genzinfra-cloudformation.git"),       
                                                new ScriptTask()
                                                    .description("Replace Tags")
                                                    .interpreterBinSh()
                                                    .fileFromPath("genzinfra-cloudformation/scripts/deploy.sh"),
                                                new CleanWorkingDirectoryTask()
                                                    .description("Clean the working directory")
                                                    .enabled(true)
                                            )
                                        );
        return deployment;
    }
}
