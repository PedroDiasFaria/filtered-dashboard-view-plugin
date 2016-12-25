package org.synopsys.plugins.synopsysdashboardview;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;

/**
 * Created by faria on 24-Dec-16.
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
    public ArrayList<Tag> Tags = new ArrayList<Tag>();

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