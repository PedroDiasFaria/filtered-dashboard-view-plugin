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

package feup.pfaria.jenkins.plugins.filtereddashboardview;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;

/**
 * Custom Job class used in the Dashboard
 * Has Builds associated to it
 * Is associated to one or more Projects
 *
 * @author Pedro Faria  &lt;pedrodiasfaria@gmail.com&gt;
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

