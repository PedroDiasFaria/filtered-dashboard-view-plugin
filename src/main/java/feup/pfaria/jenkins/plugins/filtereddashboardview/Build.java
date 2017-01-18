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
 * Custom Build class used in the Dashboard
 * Has Tags associated to it
 * Is associated to one JobData
 *
 * @author Pedro Faria  &lt;pedrodiasfaria@gmail.com&gt;
 */
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
    public ArrayList<Tag> tags = new ArrayList<Tag>();

    public Build(String jobName, String buildName, int number, long startTime, long duration, String buildUrl, String result, ArrayList<Tag> tags) {
        this.jobName = jobName;
        this.buildName = buildName;
        this.number = number;
        this.startTime = startTime;
        this.duration = duration;
        this.buildUrl = "../../" + buildUrl;
        this.result = result;
        this.tags = new ArrayList<Tag>(tags);
    }
}