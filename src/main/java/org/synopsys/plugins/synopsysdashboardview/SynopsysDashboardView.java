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

import javax.servlet.ServletException;
import java.io.IOException;

import java.util.*;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.kohsuke.stapler.*;

import com.sonyericsson.hudson.plugins.metadata.model.*;
import com.sonyericsson.hudson.plugins.metadata.model.values.MetadataValue;

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
    //private transient ViewGroupMixIn viewGroupMixIn;
    private String defaultViewName;

    private ArrayList<Build> allBuilds = new ArrayList<Build>();
    private int projectBuildTableSize;

    private HashMap<String, Boolean> selectedViews;
    private Map<String, ArrayList<String>> jobsInProjectMap;
    private Map<String, JobData> jobsMap;
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

        this.selectedViews = new HashMap<String, Boolean>();
        this.jobsInProjectMap = new TreeMap<String, ArrayList<String>>();
        this.jobsMap = new TreeMap<String, JobData>();
        this.projectBuildTableSize = 5;
    }

    @Override
    public Collection<TopLevelItem> getItems() {
        return new ArrayList<TopLevelItem>();
    }

    //TODO make it more robust
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
        JSONObject selectedViewsJSON = json.getJSONObject("selectedViews");
        selectViews(selectedViewsJSON);

        this.jobsMap = new TreeMap<>();
        this.jobsInProjectMap = new TreeMap<>();
        /************************/

        save();
    }

    public void selectViews(JSONObject selectedViewsJSON){
        Iterator<?> keys = selectedViewsJSON.keys();
        while(keys.hasNext()){
            String key = (String)keys.next();
            String value = selectedViewsJSON.get(key).toString();
            this.selectedViews.put(key, Boolean.valueOf(value));
        }
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
            projectBuildTableSize = 5;

        if(selectedViews == null){
            this.selectedViews = new HashMap<String, Boolean>();
        }

        //Forces a reset on the Maps, to ensure changes are apllied
        this.jobsInProjectMap = new TreeMap<String, ArrayList<String>>();
        this.jobsMap = new TreeMap<String, JobData>();

        //if (views == null) {
            views = new CopyOnWriteArrayList<View>();
        //}

        if (views.isEmpty()) {
            // preserve the non-empty invariant
            views.add(new ListView("Default", this));
        }

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

    public HashMap<String, Boolean> getSelectedViews(){
       return selectedViews;
    }

    @Override
    public ItemGroup<? extends TopLevelItem> getItemGroup(){
        return getOwnerItemGroup();
    }

    @Override
    public View getPrimaryView(){
            View v = getView(defaultViewName);
            if(v==null) // fallback
                v = views.get(0);

            return v;
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
        for(View v : views)
            if(v.getViewName().equals(name))
                return v;
        return null;
    }

    @Override
    public void deleteView(View view) throws IOException{
        views.remove(view);
        save();
    }

    public void onViewRenamed(View view, String oldName, String newName){
    }

    @Override
    public boolean hasPermission(final Permission p) {
        return true;
    }

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
        getAllProjects();
        getBuildHistory();
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
        return this.isDefault() ? null : getView(defaultViewName);
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

    public synchronized Collection<View> getViews(){
        return Jenkins.getInstance().getViews();
    }

    /******************/
    /**DashboardLogic**/
    /******************/

    //Calling each view as a separate project
    @Exported(name="allProjects")
    public Collection<Project> getAllProjects(){
        List<View> projects = new ArrayList<View>(getViews());
        ArrayList<Project> allProjects = new ArrayList<Project>();

        for(View p : projects){
            if(this.selectedViews.containsKey(p.getViewName()) && !p.getClass().equals(this.getClass())){   //Views of this class aren't shown inside it
                if(this.selectedViews.get(p.getViewName())){
                    Project newProject = new Project(p.getViewName(), p.getUrl(), getJobsFromProject(p));
                    allProjects.add(newProject);
                }
            }
        }
        return allProjects;
    }

    public ArrayList<JobData> getJobsFromProject(View project){
        ArrayList<JobData> jobs = new ArrayList<JobData>();
        ArrayList<String> jobNames = new ArrayList<String>();

        try {
            jobs = new ArrayList<JobData>();
            for (Object j : project.getAllItems()) {
                Job nj = (Job) j;
                jobNames.add(nj.getName());
                jobs.add(getJobByName(nj.getName()));
            }

            this.jobsInProjectMap.put(project.getViewName(), jobNames);
            return jobs;
        }catch (Error e){
            e.printStackTrace();
        }
        return null;
    }

    public JobData getJobByName(String jobName){

            try {
                List<Job> jenkinsJobs = Jenkins.getInstance().getAllItems(Job.class);
                for (Job j : jenkinsJobs) {
                    if(j.getName().equals(jobName)) {
                        ArrayList<Build> builds = new ArrayList<Build>();
                        String status;
                        String name;
                        String url = "";
                        String buildUrl = "";
                        int lastBuildNr = 0;
                        Pattern r = filterRegex != null ? Pattern.compile(filterRegex) : null;

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
                            buildUrl = "BUILDING";
                        } else {
                            Run lb = j.getLastBuild();
                            if (lb == null) {
                                status = "NOTBUILT";
                                lastBuildNr = 0;
                                buildUrl = "NOTBUILT";

                            } else {
                                status = lb.getResult().toString();
                                lastBuildNr = j.getLastBuild().getNumber();
                                buildUrl = lb.getUrl();
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

                        JobData newJob = new JobData(name, status, url, dir, Integer.toString(lastBuildNr), buildUrl, builds);
                        this.jobsMap.put(jobName, newJob);
                        return newJob;
                    }
                }
            } catch (Error e) {
                e.printStackTrace();
            }
        return null;
    }

    //Gets all child values defined in the Metadata Plugin, and creates tags according their values
    public ArrayList<Tag> getBuildTags(int buildNr, String jobName){
        ArrayList<Tag> tags = new ArrayList<Tag>();
        MetadataBuildAction metadataBuild = Jenkins.getInstance().getItemByFullName(jobName, Job.class).getBuildByNumber(buildNr).getAction(MetadataBuildAction.class);

        if(metadataBuild != null) {
            Collection<String> metadataNames = metadataBuild.getChildNames();
            for (String name : metadataNames) {
                switch (name) {
                    //Hide metadata-plugin predefined tags
                    case "job-info":
                        break;
                    case "build":
                        break;
                    default:
                        MetadataValue value = metadataBuild.getChild(name);
                        tags.add(new Tag(name.toUpperCase(), value.getValue().toString().toLowerCase()));
                        break;
                }
            }
        }
        return tags;
    }

    //Gets last 'buildHistorySize' builds from the Jobs in Projects displayed on this Dashboard
    @Exported(name="buildHistory")
    public ArrayList<Build> getBuildHistory(){
        try {
            List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);
            RunList builds = new RunList(jobs);
            int buildHistorySize = 0;
            ArrayList<Build> l = new ArrayList<Build>();
            Pattern r = filterRegex != null ? Pattern.compile(filterRegex) : null;

            for (Object b : builds) {
                if(buildHistorySize < getBuildHistorySize()) {
                    Run build = (Run) b;
                    Job job = build.getParent();
                    if(jobsMap.containsKey(job.getName())) {
                        String buildUrl = build.getUrl();

                        // Skip Maven modules. They are part of parent Maven project
                        if (job.getClass().getName().equals("hudson.maven.MavenModule"))
                            continue;

                        // If filtering is enabled, skip jobs not matching the filter
                        if (r != null && !r.matcher(job.getName()).find())
                            continue;

                        Result result = build.getResult();
                        //HttpRequest to Metadata Plugin (doGet)
                        ArrayList<Tag> tags = new ArrayList<Tag>(getBuildTags(build.getNumber(), job.getName()));

                        l.add(new Build(job.getName(),
                                build.getFullDisplayName(),
                                build.getNumber(),
                                build.getStartTimeInMillis(),
                                build.getDuration(),
                                buildUrl == null ? "null" : buildUrl,
                                result == null ? "BUILDING" : result.toString(),
                                tags));
                        buildHistorySize++;
                    }
                }else
                    break;
            }
            return l;
        }catch (Error e){
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<Build> getBuildsFromJob(Job job){
        ArrayList<Build> builds = new ArrayList<Build>();
        int size = 0;

        for(Object b : job.getBuilds()){
            if(size < getProjectBuildTableSize()) {
                Run build = (Run) b;
                Result result = build.getResult();
                ArrayList<Tag> tags = getBuildTags(build.getNumber(), job.getName());
                String buildUrl = build.getUrl();

                builds.add(new Build(job.getName(),
                            build.getFullDisplayName(),
                            build.getNumber(),
                            build.getStartTimeInMillis(),
                            build.getDuration(),
                            buildUrl == null ? "null" : buildUrl,
                            result == null ? "BUILDING" : result.toString(),
                            tags));
            }else
                break;
            size++;
        }
        return builds;
    }

    @Exported(name="allJobs")
    public Collection<JobData> getAllJobs() {
        ArrayList<JobData> allJobs = new ArrayList<JobData>();
        for(Map.Entry<String, JobData> entry : this.jobsMap.entrySet()){
            allJobs.add(entry.getValue());
        }
        return allJobs;
    }
}