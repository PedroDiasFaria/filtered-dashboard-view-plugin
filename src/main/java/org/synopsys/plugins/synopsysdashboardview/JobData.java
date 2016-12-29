package org.synopsys.plugins.synopsysdashboardview;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;

/**
 * Created by faria on 24-Dec-16.
 */
@ExportedBean(defaultVisibility = 999)
public class JobData {
    @Exported
    public JobVar JobName = new JobVar("Name", "", "");
    @Exported
    public JobVar Status = new JobVar("Status", "", "");
    @Exported
    public JobVar JobUrl = new JobVar("URL", "", "");
    @Exported
    public JobVar Dir = new JobVar("Directory", "", "");
    @Exported
    public JobVar LastBuildNr = new JobVar("Build Number", "", "expandable");
    @Exported
    public JobVar LastBuildUrl = new JobVar("Last Build URL", "", "");
    @Exported
    public ArrayList<Build> Builds = new ArrayList<Build>();

    public JobData(String jobName, String status, String jobUrl, String dir, String lastBuildNr, String lastBuildUrl, ArrayList<Build> builds) {
        this.JobName.setValue(jobName);
        this.Status.setValue(status);
        this.JobUrl.setValue("../../" + jobUrl);            //jenkinsHome/...
        this.Dir.setValue(dir);
        this.LastBuildNr.setValue(lastBuildNr);             //jenkinsHome/...
        this.LastBuildUrl.setValue("../../" + lastBuildUrl);           //jenkinsHome/...
        this.Builds = builds;
    }

    @ExportedBean(defaultVisibility = 999)
    public class JobVar{
        @Exported
        public String label;
        @Exported
        public String value;
        @Exported
        public String additionalInfo;

        JobVar(String label, String value, String info){
            this.label = label;
            this.value = value;
            this.additionalInfo = info;
        }
        public void setValue(String value){
            this.value = value;
        }
    }
}

