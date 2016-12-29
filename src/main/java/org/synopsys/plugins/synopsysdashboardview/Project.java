package org.synopsys.plugins.synopsysdashboardview;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;

/**
 * Created by faria on 24-Dec-16.
 */
@ExportedBean(defaultVisibility = 999)
public class Project{
    @Exported
    public String projectName; //will be the view name
    @Exported
    public String projectUrl; //view url
    @Exported
    public ArrayList<JobData> projectJobs = new ArrayList<JobData>();  //list of all the jobs related to this project
    @Exported
    public String projectStatus = "NOTBUILT";

    Project(String name, String url, ArrayList<JobData> jobs){
        this.projectName = name;
        this.projectUrl = "../../" + url; //jenkinsHome/...
        if(jobs != null) {
            this.projectJobs = jobs;
            this.setStatus();
        }
    }

    public void setStatus(){

        int success = 0;
        int unstable = 0;
        int failure = 0;
        String status = "NOTBUILT"; //consider not built by default

        for (JobData job : this.projectJobs) {
            switch (job.Status.value) {
                case "FAILURE":
                    failure++;
                    break;
                case "SUCCESS":
                    success++;
                    break;
                case "UNSTABLE":
                    unstable++;
                    break;
                default:
                    break;
            }
        }

        //Always consider the worst status for the project
        if(success > 0){
            status = "SUCCESS";
        }if(unstable > 0){
            status = "UNSTABLE";
        }if(failure > 0){
            status = "FAILURE";
        }
        this.projectStatus = status;
    }

}