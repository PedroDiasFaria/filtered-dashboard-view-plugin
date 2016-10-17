package org.synopsys.plugins.synopsysdashboardview;

import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by faria on 10-Oct-16.
 */
public class SynopsysDashboardView extends View{


    //From missioncontrol
    private transient int getBuildsLimit;
    private String viewName;
    private int fontSize;
    private int buildQueueSize;
    private int buildHistorySize;
    private boolean useCondensedTables;
    private String statusButtonSize;
    private String layoutHeightRatio;
    private String filterRegex;


    @DataBoundConstructor
    public SynopsysDashboardView(String name, String viewName) {
        super(name);

        //From missioncontrol
        /**************/
        this.viewName = viewName;
        this.fontSize = 16;
        this.buildQueueSize = 10;
        this.buildHistorySize = 16;
        this.useCondensedTables = false;
        this.statusButtonSize = "";
        this.layoutHeightRatio = "6040";
        this.filterRegex = null;
        /**************/
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        return new ArrayList<TopLevelItem>();
    }

    @Override
    protected void submit(StaplerRequest req) throws ServletException, IOException {
        JSONObject json = req.getSubmittedForm();

        //From missioncontrol
        /**********************/
        this.fontSize = json.getInt("fontSize");
        this.buildHistorySize = json.getInt("buildHistorySize");
        this.buildQueueSize = json.getInt("buildQueueSize");
        this.useCondensedTables = json.getBoolean("useCondensedTables");
        if (json.get("useRegexFilter") != null ) {
            String regexToTest = req.getParameter("filterRegex");
            try {
                Pattern.compile(regexToTest);
                this.filterRegex = regexToTest;
            } catch (PatternSyntaxException x) {
                Logger.getLogger(ListView.class.getName()).log(Level.WARNING, "Regex filter expression is invalid", x);
            }
        } else {
            this.filterRegex = null;
        }
        this.statusButtonSize = json.getString("statusButtonSize");
        this.layoutHeightRatio = json.getString("layoutHeightRatio");
        /************************/

        save();
    }

    @Override
    public Item doCreateItem(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        return Jenkins.getInstance().doCreateItem(req, rsp);
    }

    @Override
    public boolean contains(TopLevelItem item) {
        return false;
    }


    /********************/
    //Bellow all from mission control:
    /********************/
    protected Object readResolve() {
        if (getBuildsLimit == 0)
            getBuildsLimit = 250;

        if (fontSize == 0)
            fontSize = 16;

        if (buildHistorySize == 0)
            buildHistorySize = 16;

        if (buildQueueSize == 0)
            buildQueueSize = 10;

        if (statusButtonSize == null)
            statusButtonSize = "";

        if (layoutHeightRatio == null)
            layoutHeightRatio = "6040";

        return this;
    }
    public int getFontSize() {
        return fontSize;
    }

    public int getBuildHistorySize() {
        return buildHistorySize;
    }

    public int getBuildQueueSize() {
        return buildQueueSize;
    }

    public boolean isUseCondensedTables() {
        return useCondensedTables;
    }

    public String getTableStyle() {
        return useCondensedTables ? "table-condensed" : "";
    }

    public String getStatusButtonSize() {
        return statusButtonSize;
    }

    public String getLayoutHeightRatio() {
        return layoutHeightRatio;
    }

    public String getFilterRegex() {
        return filterRegex;
    }

    public String getTopHalfHeight() {
        return layoutHeightRatio.substring(0, 2);
    }

    public String getBottomHalfHeight() {
        return layoutHeightRatio.substring(2, 4);
    }

    @Override
    public boolean hasPermission(final Permission p) { return true; }

    /**
     * This descriptor class is required to configure the View Page
     */
    @Extension
    public static final class DescriptorImpl extends ViewDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.SynopsysDasbhoardView_DisplayName();
        }
    }

    public Api getApi() {
        return new Api(this);
    }

    @Exported(name="builds")
    public Collection<Build> getBuildHistory() {
        List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
        RunList builds = new RunList(jobs).limit(getBuildsLimit);
        ArrayList<Build> l = new ArrayList<Build>();
        Pattern r = filterRegex != null ? Pattern.compile(filterRegex) : null;

        for (Object b : builds) {
            Run build = (Run)b;
            Job job = build.getParent();

            // Skip Maven modules. They are part of parent Maven project
            if (job.getClass().getName().equals("hudson.maven.MavenModule"))
                continue;

            // If filtering is enabled, skip jobs not matching the filter
            if (r != null && !r.matcher(job.getName()).find())
                continue;

            Result result = build.getResult();
            l.add(new Build(job.getName(),
                    build.getFullDisplayName(),
                    build.getNumber(),
                    build.getStartTimeInMillis(),
                    build.getDuration(),
                    build.getUrl(),
                    result == null ? "BUILDING" : result.toString()));
        }

        return l;
    }

    //added jobUrl
    @ExportedBean(defaultVisibility = 999)
    public class Build {
        @Exported
        public String jobName;
        @Exported
        public String buildName;
        @Exported
        public int number;
        @Exported
        public long startTime;
        @Exported
        public long duration;
        @Exported
        public String result;
        @Exported
        public String jobUrl;

        public Build(String jobName, String buildName, int number, long startTime, long duration, String jobUrl, String result) {
            this.jobName = jobName;
            this.buildName = buildName;
            this.number = number;
            this.startTime = startTime;
            this.duration = duration;

            this.jobUrl = jobUrl;

            this.result = result;


        }
    }

    /*******/
    //Test Function to get all info
    // search by jobId ? or jobname
    // add argument
    /*@Exported//(name="allJobData")
    public Collection<JobData> getAllJobData(String jobId){

        Job job = Jenkins.getInstance().getItem();
       ArrayList<JobData> jobData = new  ArrayList<JobData>();
        jobData.add(new JobData("ALL PROPERTIES OF THIS JOB RETURNED"));


        return jobData;
    }

    //what's default visibility?
    @ExportedBean(defaultVisibility = 999)
    public class JobData{
        @Exported
        public String allProperties;

        public JobData(String allProperties){
            this.allProperties = allProperties;
        }
    }

    /*******/

    @Exported(name="allJobsStatuses")
    public Collection<JobStatus> getAllJobsStatuses() {
        List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
        ArrayList<JobStatus> statuses = new ArrayList<JobStatus>();
        String status, name;

        /********/
        String url;
        int lastBuildNr = 0;
        /********/
        Pattern r = filterRegex != null ? Pattern.compile(filterRegex) : null;

        for (Job j : jobs) {
            // Skip matrix configuration sub-jobs and Maven modules
            if (j.getClass().getName().equals("hudson.matrix.MatrixConfiguration")
                    || j.getClass().getName().equals("hudson.maven.MavenModule"))
                continue;

            // If filtering is enabled, skip jobs not matching the filter
            if (r != null && !r.matcher(j.getName()).find())
                continue;

            // Get the url to link it in the dashboard
            url = j.getUrl();
            if (j.isBuilding()) {
                status = "BUILDING";
            } else {
                Run lb = j.getLastBuild();
                if (lb == null) {
                    status = "NOTBUILT";
                    lastBuildNr = 0;
                } else {
                    status = lb.getResult().toString();
                    lastBuildNr = j.getLastBuild().getNumber();
                }
            }

            ItemGroup parent = j.getParent();
            if (parent != null && parent.getClass().getName().equals("com.cloudbees.hudson.plugins.folder.Folder")) {
                name = parent.getFullName() + " / " + j.getName();
            } else {
                name = j.getName();
            }

            String dir = j.getBuildDir().toString();

            JobData jobData = new JobData(dir, lastBuildNr);

            statuses.add(new JobStatus(name, status, url, jobData));
        }

        return statuses;
    }

    @ExportedBean(defaultVisibility = 999)
    public class JobStatus {
        @Exported
        public String jobName;
        @Exported
        public String status;
        @Exported
        public String jobUrl;

        /*********/
        @Exported
        public JobData jobData;
        /*********/

        public JobStatus(String jobName, String status, String jobUrl, JobData jobData) {
            this.jobName = jobName;
            this.status = status;
            this.jobUrl = jobUrl;

            /*********/
            this.jobData = jobData;
            /*********/
        }
    }

    @ExportedBean(defaultVisibility = 999)
    public class JobData{
        @Exported
        public String dir;
        @Exported
        public int lastBuildNr;

        JobData(String dir, int lastBuildNr){
            this.dir = dir;
            this.lastBuildNr = lastBuildNr;
        }
    }

    /*************************/
}
