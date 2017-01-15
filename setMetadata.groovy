/**
 * This is a Groovy Script function you can add
 * to your Pipeline Jobs to include Metadata in them
 * Only works with StringParameters
 *
 * Used until Metadata Plugin supports WorkflowJobs
 */

import hudson.model.*

@NonCPS
def setMetadata(map){

    def npl = new ArrayList<ParametersAction>();
    for(e in map){
        npl.add(new StringParameterValue(e.key.toString(), e.value.toString()));
    }
    def newPa = null
    def oldPa = currentBuild.build().getAction(ParametersAction.class)
    if (oldPa != null) {
        currentBuild.build().actions.remove(oldPa)
        newPa = oldPa.createUpdated(npl)
    } else {
        newPa = new ParametersAction(npl)
    }
    currentBuild.build().actions.add(newPa);
}

def map = [:]
//Example to populate the map:
map['metadata:TagName1'] = 'value1';
map['metadata:TagName2'] = 'value2';

setMetadata(map);