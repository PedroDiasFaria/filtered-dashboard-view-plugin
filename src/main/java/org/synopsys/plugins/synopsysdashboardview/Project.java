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

package org.synopsys.plugins.synopsysdashboardview;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;

/**
 * Custom View class used in the Dashboard
 * Has JobData associated to it
 *
 * @author Pedro Faria  &lt;pedrodiasfaria@gmail.com&gt;
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
        int aborted = 0;
        int unstable = 0;
        int failure = 0;
        String status = "NOTBUILT"; //consider not built by default

        for (JobData job : this.projectJobs) {
            switch (job.Status.value) {
                case "FAILURE":
                    failure++;
                    break;
                case "ABORTED":
                    aborted++;
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
        }if(aborted > 0){
            status = "ABORTED";
        }if(unstable > 0){
            status = "UNSTABLE";
        }if(failure > 0){
            status = "FAILURE";
        }
        this.projectStatus = status;
    }

}