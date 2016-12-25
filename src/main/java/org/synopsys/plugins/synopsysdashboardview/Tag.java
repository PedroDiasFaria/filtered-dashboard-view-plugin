package org.synopsys.plugins.synopsysdashboardview;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Created by faria on 24-Dec-16.
 */
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