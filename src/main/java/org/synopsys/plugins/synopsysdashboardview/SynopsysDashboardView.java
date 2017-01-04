/*
 *  The MIT License
 *
 *  Copyright 2017 Pedro Faria. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

/*
 *  Plugin based from 'Mission Control Plugin' - by Andrey Shevtsov
 *  source: https://wiki.jenkins-ci.org/display/JENKINS/Mission+Control+Plugin
 */

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

@SuppressWarnings({"unused", "all"})

/**
 * A Dashboard View that displays selected Views from Jekins instance.
 * Displays Views as Projects, Jobs inside those Views and their respective Builds
 * Displays a Build History and a Build Queue
 *
 * Metadata-Plugin is necessary to create Tags for each Build
 * This are latter used to filter searches on the Dashboard
 *
 * @author Pedro Faria  &lt;pedrodiasfaria@gmail.com&gt;
 */
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
    /** All the Views on this Jenkins instance */
    private CopyOnWriteArrayList<View> views = new CopyOnWriteArrayList<View>();
    //private transient ViewGroupMixIn viewGroupMixIn;
    private String defaultViewName;

    /** All Builds in this View */
    private ArrayList<Build> allBuilds = new ArrayList<Build>();
    private int projectBuildTableSize;

    /** All selected Views to display */
    private HashMap<String, Boolean> selectedViews;
    /** Map of Views associated with their Jobs */
    private Map<String, ArrayList<String>> jobsInProjectMap;
    /** Map of Jobs associated with their custom class JobData */
    private Map<String, JobData> jobsMap;

    /**
     * Dashboard constructor
     * @param name this view name
     */
    @DataBoundConstructor
    public SynopsysDashboardView(String name) {
        super(name);

        //From missioncontrol
        /**************/
        this.viewName = name;
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
        /**********************/

        JSONObject selectedViewsJSON = json.getJSONObject("selectedViews");
        selectViews(selectedViewsJSON);
        this.jobsMap = new TreeMap<>();
        this.jobsInProjectMap = new TreeMap<>();

        save();
    }

    /**
     *
     * @param selectedViewsJSON JSON Map with the Views to be displayed in the Dashboard
     */
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

    /**
     * Forces some default values
     * Refreshes Jobs and Builds that suffered changes
     * @return this View
     */
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

    /**
     * Treat each View as a separate Project
     * Gets all Projects associated to this Dashboard View
     *
     * @return allProjects
     */
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

    /**
     * Populates a Project with its associated Jobs and
     * respective Builds
     *
     * @param project the project to populate with jobs
     * @return jobs all Jobs from the selected Project
     */
    public ArrayList<JobData> getJobsFromProject(View project){
        ArrayList<JobData> jobs = new ArrayList<JobData>();
        ArrayList<String> jobNames = new ArrayList<String>();

        try {
            jobs = new ArrayList<JobData>();
            for (Object j : project.getAllItems()) {
                Job newJob = (Job) j;
                JobData newJobData = parseJob(newJob);
                if(newJobData != null) {
                    jobNames.add(newJob.getName());
                    jobs.add(newJobData);
                }
            }
            this.jobsInProjectMap.put(project.getViewName(), jobNames);
            return jobs;
        }catch (Error e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Parses the Job job from this Jenkins instance
     * to a custom JobData class
     *
     * @param job Job to parse
     * @return the Job as a JobData class
     */
    public JobData parseJob(Job job){
            try {
                ArrayList<Build> builds = new ArrayList<Build>();
                String status;
                String name;
                String url = "";
                String buildUrl = "";
                int lastBuildNr = 0;
                Pattern r = filterRegex != null ? Pattern.compile(filterRegex) : null;


                // Skip matrix configuration sub-jobs and Maven modules
                if (job.getClass().getName().equals("hudson.matrix.MatrixConfiguration")
                        || job.getClass().getName().equals("hudson.maven.MavenModule"))
                    return null;

                // If filtering is enabled, skip jobs not matching the filter
                if (r != null && !r.matcher(job.getName()).find())
                    return null;

                // Get the url to link it in the dashboard
                url = job.getUrl();

                if (job.isBuilding()) {
                    status = "BUILDING";
                    buildUrl = "BUILDING";
                } else {
                    Run lb = job.getLastBuild();
                    if (lb == null) {
                        status = "NOTBUILT";
                        lastBuildNr = 0;
                        buildUrl = "NOTBUILT";

                    } else {
                        status = lb.getResult().toString();
                        lastBuildNr = job.getLastBuild().getNumber();
                        buildUrl = lb.getUrl();
                    }
                }

                ItemGroup parent = job.getParent();
                if (parent != null && parent.getClass().getName().equals("com.cloudbees.hudson.plugins.folder.Folder")) {
                    name = parent.getFullName() + " / " + job.getName();
                } else {
                    name = job.getName();
                }

                String dir = job.getBuildDir().toString();
                builds = getBuildsFromJob(job);

                JobData newJob = new JobData(name, status, url, dir, Integer.toString(lastBuildNr), buildUrl, builds);
                this.jobsMap.put(job.getFullDisplayName(), newJob);
                return newJob;
            } catch (Error e) {
                e.printStackTrace();
            }
        return null;
    }

    /**
     * Gets all child values defined in the Metadata Plugin
     * and creates Tags according their values
     *
     * @param jobName name of the Job to search for
     * @param buildNr number of the Build of the Job
     * @return tags the list of Tags associated to this Build
     */
    public ArrayList<Tag> getBuildTags(String jobName, int buildNr){
        ArrayList<Tag> tags = new ArrayList<Tag>();

        try {
            //Get all the metadata in this build
            MetadataBuildAction metadataBuild = Jenkins.getInstance().getItemByFullName(jobName, Job.class)
                                                .getBuildByNumber(buildNr).getAction(MetadataBuildAction.class);

            if (metadataBuild != null) {
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
        }catch (Exception e){
            e.printStackTrace();
        }
        return tags;
    }

    /**
     * Gets last 'buildHistorySize' builds from
     * the Jobs associated to the Projects displayed
     * on this Dashboard
     *
     * @return buildList list of last 'buildHistorySize' Builds ran
     */
    @Exported(name="buildHistory")
    public ArrayList<Build> getBuildHistory(){
        try {
            Pattern r = filterRegex != null ? Pattern.compile(filterRegex) : null;
            ArrayList<Job> jobs = new ArrayList<>();

            for(String jobName : this.jobsMap.keySet()){
                Job job = Jenkins.getInstance().getItemByFullName(jobName, Job.class);

                // Skip Maven modules. They are part of parent Maven project
                if (job.getClass().getName().equals("hudson.maven.MavenModule"))
                    continue;

                // If filtering is enabled, skip jobs not matching the filter
                if (r != null && !r.matcher(job.getName()).find())
                    continue;

                jobs.add(job);
            }

            RunList builds = new RunList(jobs).limit(200);  //We can ignore if the builds of this project are beyond the 200th
            int buildHistorySize = 0;
            ArrayList<Build> buildList = new ArrayList<Build>();

            for (Object b : builds) {
                if(buildHistorySize < getBuildHistorySize()) {
                    Run build = (Run) b;
                    String buildUrl = build.getUrl();
                    Result result = build.getResult();
                    ArrayList<Tag> tags = new ArrayList<Tag>(getBuildTags(build.getParent().getName(), build.getNumber()));

                    buildList.add(new Build(build.getParent().getName(),
                            build.getFullDisplayName(),
                            build.getNumber(),
                            build.getStartTimeInMillis(),
                            build.getDuration(),
                            buildUrl == null ? "null" : buildUrl,
                            result == null ? "BUILDING" : result.toString(),
                            tags));
                    buildHistorySize++;
                }else
                    break;
            }
            return buildList;
        }catch (Error e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the Builds from the Job
     * and parses them to a custom Build class
     *
     * @param job job to get Builds from
     * @return builds list of Builds from the selected Job
     */
    public ArrayList<Build> getBuildsFromJob(Job job){
        ArrayList<Build> builds = new ArrayList<Build>();
        int size = 0;

        for(Object b : job.getBuilds()){
            if(size < getProjectBuildTableSize()) {
                Run build = (Run) b;
                Result result = build.getResult();
                ArrayList<Tag> tags = getBuildTags(job.getName(), build.getNumber());
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

    /**
     * Gets all the Jobs associated to the Views
     * displayed in the Dashboard
     *
     * @return allJobs list of Jobs displayed
     */
    @Exported(name="allJobs")
    public Collection<JobData> getAllJobs() {
        ArrayList<JobData> allJobs = new ArrayList<JobData>();
        for(Map.Entry<String, JobData> entry : this.jobsMap.entrySet()){
            allJobs.add(entry.getValue());
        }
        return allJobs;
    }
}