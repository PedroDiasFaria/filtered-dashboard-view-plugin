package org.synopsys.plugins.synopsysdashboardview;

import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.security.Permission;
import hudson.util.RunList;
import hudson.views.ViewsTabBar;
import jenkins.model.Jenkins;
import net.sf.json.*;
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
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import java.net.*;
import java.io.*;


//
import hudson.views.ListViewColumn;
import hudson.views.ViewsTabBar;

import org.kohsuke.stapler.*;
import org.kohsuke.stapler.export.Exported;
import static hudson.Util.fixEmpty;

import org.kohsuke.stapler.bind.JavaScriptMethod;

//Metadata

/**
 * Created by faria on 10-Oct-16.
 */
@SuppressWarnings({"unused", "all"})
public class SynopsysDashboardView extends View implements ViewGroup, StaplerProxy{

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

    //Allways hold at least one view
    private CopyOnWriteArrayList<View> views = new CopyOnWriteArrayList<View>();
    private transient ViewGroupMixIn viewGroupMixIn;
    private String defaultViewName;

    private ArrayList<Project> allProjects = new ArrayList<>();
    private ArrayList<JobData> allJobs = new ArrayList<>();
    private ArrayList<Build> allBuilds = new ArrayList<>();
    private int projectBuildTableSize;
    /******/
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

        this.projectBuildTableSize = 15;
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        return new ArrayList<TopLevelItem>();
    }

    @Override
    protected void submit(StaplerRequest req) throws ServletException, IOException {
        JSONObject json = req.getSubmittedForm();
        defaultViewName = Util.fixEmpty(req.getParameter("defaultView"));

        //From missioncontrol
        /**********************/
        this.fontSize = json.getInt("fontSize");
        this.buildHistorySize = json.getInt("buildHistorySize");
        this.buildQueueSize = json.getInt("buildQueueSize");
        this.useCondensedTables = json.getBoolean("useCondensedTables");
        this.projectBuildTableSize = json.getInt("projectBuildTableSize");
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

        if (projectBuildTableSize == 0)
            projectBuildTableSize = 15;

        /*ViewGruopMix in and sets primary view*/
        if (views == null) {
            views = new CopyOnWriteArrayList<View>();
        }

        if (views.isEmpty()) {
            // preserve the non-empty invariant
            views.add(new ListView("Default", this));
        }

        viewGroupMixIn = new ViewGroupMixIn(this) {

            @Override
            protected List<View> views() {
                return views;
            }

            @Override
            protected String primaryView() {
                return defaultViewName;
            }

            @Override
            protected void primaryView(String name) {
                defaultViewName = name;
            }
        };

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

    public int getProjectBuildTableSize() {
        return projectBuildTableSize;
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

    //Viewgroup functions
    //https://github.com/jenkinsci/nested-view-plugin/blob/master/src/main/java/hudson/plugins/nested_view/NestedView.java
    /**********/

    @Override
    public ItemGroup<? extends TopLevelItem> getItemGroup(){
        return getOwnerItemGroup();
    }

    @Override
    public View getPrimaryView(){
        /*
            View v = getView(defaultViewName);
            if(v==null) // fallback
                v = views.get(0);

            return v;*/
        return viewGroupMixIn.getPrimaryView();
    }

    public boolean canDelete(View view){
        return false;
    }

    @Override
    public List<Action> getViewActions(){
        return getOwner().getViewActions();
    }

    public ViewsTabBar getViewsTabBar(){
        return Hudson.getInstance().getViewsTabBar();
    }

    @Override
    public View getView(String name){
       /* for(View v : views)
            if(v.getViewName().equals(name))
                return v;
        // Fallback to subview of primary view if it is a ViewGroup
        View pv = getPrimaryView(); //line that causes problems
        if (pv instanceof ViewGroup)
            return ((ViewGroup)pv).getView(name);
        return null;*/

       return viewGroupMixIn.getView(name);
    }

    @Override
    public void deleteView(View view) throws IOException{
        viewGroupMixIn.deleteView(view);
        save();
    }

    public void onViewRenamed(View view, String oldName, String newName){

    }
    /**********/

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
        //System.out.println("REFRESHING AND HERE WE GO AGAIN");
        getBuildHistory();
        //System.out.println("BUILDS DONE");
        getAllJobs();
        //System.out.println("JOBS DONE");
        getProjects();
        //System.out.println("PROJECTS DONE");
        return new Api(this);
    }

    /********/
    /** StapplerProxy **/
    public Object getTarget() {
        // Proxy to handle redirect when a default subview is configured
        return (getDefaultView() != null &&
                "".equals(Stapler.getCurrentRequest().getRestOfPath()))
                ? new DefaultViewProxy() : this;
    }

    public View getDefaultView() {
        // Don't allow default subview for a NestedView that is the Jenkins default view..
        // (you wouldn't see the other top level view tabs, as it'd always jump into subview)
        return isDefault() ? null : getView(defaultViewName);
    }

    public class DefaultViewProxy {
        public void doIndex(StaplerRequest req, StaplerResponse rsp)
                throws IOException, ServletException {
            if (getDefaultView() != null)
                rsp.sendRedirect2("view/" + defaultViewName);
            else
                req.getView(SynopsysDashboardView.this, "index.jelly").forward(req, rsp);
        }
    }

    /********/

    public ArrayList<Tag> getBuildTags(int buildNr, String jobName){
        ArrayList<Tag> tags = new ArrayList<Tag>();

        try {
            String requestString;
            String jenkinsUrl = Jenkins.getInstance().getRootUrl();
            String encodedJobName = java.net.URLEncoder.encode(jobName, "UTF-8").replace("+", "%20");
            String metadataApiRequest = "metadata-httpcli/get?job=" + encodedJobName + "&build=" + buildNr;
            requestString = jenkinsUrl + metadataApiRequest;
            URL url = new URL(requestString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder resultStringB = new StringBuilder();
            String response;

            while((response = rd.readLine()) != null){
                resultStringB.append(response);
            }
            rd.close();

            JSONArray jsonArray =  (JSONArray)JSONSerializer.toJSON(resultStringB.toString());

            //String resultString = resultStringB.substring(1, resultStringB.toString().length()-1);
            //JSONObject resultJson = JSONObject.fromObject(resultString);

            for(int i=0; i<jsonArray.size(); i++){
                String name = jsonArray.getJSONObject(i).get("name").toString();
                //name = .last-saved
                switch (name){
                    /*case "job-info.last-saved.time" :
                        break;
                    case "job-info.last-saved.user.display-name" :
                        break;
                    case "job-info.last-saved.user.full-name" :
                        break;
                    case "build.result" :
                        break;
                    case "build.duration.ms" :
                        break;
                    case "build.duration.display" :
                        break;
                    case "build.builtOn" :
                        break;
                    case "build.scheduled" :
                        break;*/
                    case "job-info":
                        break;
                    case "build":
                        break;
                    default:
                        String value = jsonArray.getJSONObject(i).get("value").toString();
                        tags.add(new Tag(name, value));
                        break;
                }
            }
        }catch (IOException e){
        }

        return tags;
    }

    @Exported(name="builds")
    public ArrayList<Build> getBuildHistory(){
        List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
        RunList builds = new RunList(jobs).limit(getBuildsLimit);
        //TODO: unlimited builds or each job has its own in its bean (2nd better, 1st only for display)
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

            //TODO GET METADATA AKA TAGS
            //TODO http request get to metadata api
            ArrayList<Tag> tags = getBuildTags(build.getNumber(), job.getName());

            /*
            tags.add(new Tag("version", "1.0"));
            tags.add(new Tag("machine", "xyz"));
            tags.add(new Tag("version", "2.0Beta"));*/

            l.add(new Build(job.getName(),
                    build.getFullDisplayName(),
                    build.getNumber(),
                    build.getStartTimeInMillis(),
                    build.getDuration(),
                    build.getUrl(),
                    result == null ? "BUILDING" : result.toString(),
                    tags));
        }

        this.allBuilds = new ArrayList<>(l);
        return l;
    }

    public ArrayList<Build> getBuildsFromJob(Job job){
        ArrayList<Build> builds = new ArrayList<>();
        int size = 0;

        for(Object b : job.getBuilds()){
            if(size < getProjectBuildTableSize()) {
                Run build = (Run) b;
                builds.add(getBuildByName(build.getFullDisplayName()));
            }else
                break;
            size++;
        }
        return builds;
    }

    public Build getBuildByName(String buildName){
        for(Build build : this.allBuilds){
            if(build.buildName.equals(buildName)){
                return build;
            }
        }
        return null;
    }

    /*******/

    /*******/

    @Exported(name="allJobs")
    public Collection<JobData> getAllJobs() {
        List<Job> jenkinsJobs = Jenkins.getInstance().getAllItems(Job.class);
        ArrayList<JobData> jobs = new ArrayList<JobData>();
        ArrayList<Build> builds = new ArrayList<Build>();
        String status, name;

        /********/
        String url = "";
        String buildUrl = "";
        String metadata = "";
        int lastBuildNr = 0;
        /********/
        Pattern r = filterRegex != null ? Pattern.compile(filterRegex) : null;

        for (Job j : jenkinsJobs) {
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
                metadata = "BUILDING";
                buildUrl = "BUILDING";
            } else {
                Run lb = j.getLastBuild();
                if (lb == null) {
                    status = "NOTBUILT";
                    lastBuildNr = 0;
                    buildUrl = lb.getUrl();
                    //metadata = lb.getDescription();

                } else {
                    status = lb.getResult().toString();
                    lastBuildNr = j.getLastBuild().getNumber();
                    buildUrl = lb.getUrl();
                    //metadata = lb.getDescription();
                }
            }

            ItemGroup parent = j.getParent();
            if (parent != null && parent.getClass().getName().equals("com.cloudbees.hudson.plugins.folder.Folder")) {
                name = parent.getFullName() + " / " + j.getName();
            } else {
                name = j.getName();
            }

            String dir = j.getBuildDir().toString();
            builds = getBuildsFromJob(j);

            jobs.add(new JobData(name, status, url, dir, Integer.toString(lastBuildNr),buildUrl, metadata, builds));
        }

        this.allJobs = new ArrayList<>(jobs);
        return jobs;
    }

    public JobData getJobByName(String jobName){
        for(JobData job : this.allJobs){
            if(job.JobName.value.equals(jobName)){
                return job;
            }
        }
        return null;
    }

    public ArrayList<JobData> getJobsFromProject(View project){
        ArrayList<JobData> jobs = new ArrayList<>();

        for(Object j : project.getAllItems()){
                Job nj = (Job)j;
                jobs.add(getJobByName(nj.getName()));
        }

        return jobs;
    }

    public Collection<View> getViews(){
        return Jenkins.getInstance().getViews();
    }

    //Calling each view as a separate project
    @Exported(name="allProjects")
    public Collection<Project> getProjects(){

        List<View> projects = new ArrayList<View>(getViews());
        ArrayList<Project> allProjects = new ArrayList<>();

        //Get name of view
        for(View p : projects){
            if(p.getDisplayName().equals("All") ||                              //Only counting real projects, excluding THIS and 'All'
                    p.getDisplayName().equals(this.getDisplayName()))
                continue;
            else{
                Project newProject = new Project(p.getDisplayName(), p.getUrl(), getJobsFromProject(p), null);
                newProject.setStatus();
                allProjects.add(newProject);
            }
        }

        this.allProjects = new ArrayList<>(allProjects);
        return allProjects;
    }

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
        public String buildUrl;
        @Exported
        public ArrayList<Tag> Tags;

        public Build(String jobName, String buildName, int number, long startTime, long duration, String buildUrl, String result, ArrayList<Tag> tags) {
            this.jobName = jobName;
            this.buildName = buildName;
            this.number = number;
            this.startTime = startTime;
            this.duration = duration;
            this.buildUrl = "../../" + buildUrl;
            this.result = result;
            this.Tags = tags;
        }
    }

    @ExportedBean(defaultVisibility = 999)
    public class JobData {
        @Exported
        public JobVar JobName = new JobVar("Name", "", "");
        @Exported
        public JobVar Status = new JobVar("Status", "", "");
        @Exported
        public JobVar JobUrl = new JobVar("URL", "", "");
        @Exported
        public JobVar Dir = new JobVar("Directory", "", "expandable");
        @Exported
        public JobVar LastBuildNr = new JobVar("Last Build", "", "expandable");
        @Exported
        public JobVar LastBuildUrl = new JobVar("Last Build URL", "", "expandable");
        @Exported
        public JobVar Metadata = new JobVar("Metadata", "", "expandable");
        @Exported
        public ArrayList<Build> Builds = new ArrayList<>();

        public JobData(String jobName, String status, String jobUrl, String dir, String lastBuildNr, String lastBuildUrl, String metadata, ArrayList<Build> builds) {
            this.JobName.setValue(jobName);
            this.Status.setValue(status);
            this.JobUrl.setValue("../../" + jobUrl);            //jenkinsHome/...
            this.Dir.setValue(dir);
            this.LastBuildNr.setValue("../../" + lastBuildNr);             //jenkinsHome/...
            this.LastBuildUrl.setValue("../../" + lastBuildUrl);           //jenkinsHome/...
            this.Metadata.setValue(metadata);
            this.Builds = builds;
        }
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

    @ExportedBean(defaultVisibility = 999)
    public class Project{
        @Exported
        public String projectName; //will be the view name
        @Exported
        public String projectUrl; //view url
        @Exported
        public ArrayList<JobData> projectJobs;  //list of all the jobs related to this project
        @Exported
        public String projectStatus;

        Project(String name, String url, ArrayList<JobData> jobs, String status){
            this.projectName = name;
            this.projectUrl = "../../" + url; //jenkinsHome/...
            this.projectJobs = jobs;
            this.projectStatus = status;
        }

        public void setStatus(){

            String status = "SUCCESS"; //consider successfull by default

            for(JobData job : this.projectJobs){
                if(job.Status.value.equals("FAILURE")) {
                    status = "FAILURE";
                }
            }

            this.projectStatus = status;

        }
    }

    @ExportedBean(defaultVisibility = 999)
    public class Tag{
        @Exported
        public String label;
        @Exported
        public String value;

        Tag(String label, String value){
            this.label = label;
            this.value = value;
        }
    }
}


//TODO: Refactor everything
//TODO: maybe remove visibility? don't want duplicates of JobVar, jobs within projects, builds within jobs etc
//TODO: create a var for each list of job, build and project instead of always creating new ones
//TODO: check buildUrl !