function format_date(dt) {
  return dt.getFullYear()
    + '-' + (dt.getMonth()<9?'0':'') + (dt.getMonth() + 1)
    + '-' + (dt.getDate()<10?'0':'') + dt.getDate()
    + ' ' + (dt.getHours()<10?'0':'') + dt.getHours()
    + ':' + (dt.getMinutes()<10?'0':'') + dt.getMinutes()
    + ':' + (dt.getSeconds()<10?'0':'') + dt.getSeconds();
}

function format_interval(iv) {
  if (iv < 1000) { return iv + 'ms'; }

  ivStr = '';
  // Days
  if (iv > 86400000) {
    ivStr = Math.floor(iv/86400000) + 'd';
    iv = iv - Math.floor(iv/86400000)*86400000;
  }
  // Hours
  if (iv > 3600000) {
    ivStr += ' ' + Math.floor(iv/3600000) + 'h';
    iv = iv - Math.floor(iv/3600000)*3600000;
  }
  // Minutes
  if (iv > 60000) {
    ivStr += ' ' + Math.floor(iv/60000) + 'm';
    iv = iv - Math.floor(iv/60000)*60000;
  }
  // Seconds
  if (iv > 1000)
    ivStr += ' ' + Math.floor(iv/1000) + 's';
  return ivStr;
}

function reload_jenkins_build_queue(tableSelector, jenkinsUrl, buildQueueSize) {
  $.getJSON( jenkinsUrl + '/queue/api/json', function( data ) {
    // Remove all existing rows
    $(tableSelector + ' tbody').find('tr').remove(); 
    i = 0;
    $.each( data.items, function( key, val ) {
      i++;
      if (i > buildQueueSize) {
        return;
      }
      startDate = new Date(val.inQueueSince);
      now = new Date();
      waitingFor = now.getTime() - val.inQueueSince;
      taskName = val.task.name.replace(/(,?)\w*=/g, "$1");
      newRow = '<tr><td class="text-left"><a href="' + val.task.url + '">'+ taskName + '</a></td><td>' + format_date(startDate) + '</td><td>' + format_interval(waitingFor) + '</td></tr>';
      $(tableSelector + ' tbody').append(newRow);
    });
  });
}

function reload_jenkins_node_statuses(divSelector, jenkinsUrl, nodeStatuses, buttonClass) {
  $.getJSON( jenkinsUrl + '/computer/api/json', function( data ) {
    // Remove all existing rows
    $(divSelector + ' button').remove();
    $.each( data.computer, function( key, val ) {
      classes = !val.offline ? 'btn-success' : 'btn-danger';
      if (val.displayName == "master")
        nodeLinkName = '(master)';
      else
        nodeLinkName = val.displayName;
      newDiv = '<a href="' + jenkinsUrl + '/computer/' + encodeURIComponent(nodeLinkName) + '/"><button class="btn ' + buttonClass + ' ' + classes + ' col-lg-6">' + val.displayName + ' &#47; ' + val.numExecutors + '</button></a>';
      $(divSelector).append(newDiv);
    });
  });
}

function reload_jenkins_build_history(tableSelector, viewUrl, buildHistorySize) {
  $.getJSON( viewUrl + '/api/json', function( data ) {
    // Remove all existing rows
    $(tableSelector + ' tbody').find('tr').remove();
    i = 0;
    $.each( data.builds, function( key, val ) {
      i++;
      if (i > buildHistorySize) {
        return;
      }
      dt = new Date(val.startTime + val.duration);
      jobName = val.buildName.replace(/(.*) #.*/, '$1');
      switch (val.result) {
        case 'SUCCESS':
          classes = '';
          break;
        case 'FAILURE':
          classes = 'danger';
          break;
        case 'ABORTED':
        case 'UNSTABLE':
          classes = 'warning';
          break;
        case 'BUILDING':
          classes = 'info invert-text-color';
          break;
        default:
          classes = '';
      }
      newRow = '<tr class="' + classes + '"><td class="text-left">' + jobName + '</td><td>' + val.number + '</td><td>' + format_date(dt) + '</td><td>' + format_interval(val.duration) + '</td></tr>';
      $(tableSelector + ' tbody').append(newRow);
    });
  });
}

function reload_jenkins_jobs(divSelector, viewUrl, buttonClass) {
  $.getJSON( viewUrl + '/api/json', function( data ) {
    // Remove all existing divs
    $(divSelector + ' button').remove();
    $.each( data.allJobs, function( key, val ) {
      switch (val.Status.value) {
        case 'SUCCESS':
          classes = 'btn-success';
          break;
        case 'FAILURE':
          classes = 'btn-danger';
          break;
        case 'ABORTED':
        case 'UNSTABLE':
          classes = 'btn-warning';
          break;
        case 'NOTBUILT':
          classes = 'invert-text-color';
          break;
        case 'BUILDING':
          classes = 'btn-info invert-text-color';
          break;
        default:
          classes = 'btn-primary';
      }

      expandable = "";

      //Div with extra information
      for (var key in val) {
        if(val[key].additionalInfo == "expandable"){
          expandable+=
            '<div class="jobDataElem">' +
                '<b class="dataElemLabel col-md-3 pull-left">'+ val[key].label +': </b>' + '<span class="dataElemValue col-md-9 pull-right">' + val[key].value + '</span></br>' +
            '</div>'
        }
      }

      newDiv =
      '<button id="' + val.JobName.value + '" class="btn ' + buttonClass + ' ' + classes + ' col-lg-6">' + '<p>' + val.JobName.value + '</p>' +
          '<p><a class="goTo" href="' + val.JobURL.value + '">' + '(Go To Job)' + '</a></p>' +

        //  '<a class="pull-left btn btn-primary" data-toggle="collapse" data-target="expandable_'+val.JobName.value+'">Expand+</a>'+

            '<div id="expandable_'+val.JobName.value+'" class="">' +
                expandable +
            '</div>' +

      '</button>';

        console.log(val.Metadata.value);
      $(divSelector).append(newDiv);
    });
  });
}

function reload_jenkins_projects(divSelector, viewUrl, buttonClass){

    $.getJSON( viewUrl + '/api/json', function(data){
        $(divSelector + ' button').remove();

        $.each(data.allProjects, function(key, val){
            //get jobs from view:   val.name

          switch (val.projectStatus) {
            case 'SUCCESS':
              classes = 'btn-success';
              break;
            case 'FAILURE':
              classes = 'btn-danger';
              break;
            case 'ABORTED':
            case 'UNSTABLE':
              classes = 'btn-warning';
              break;
            case 'NOTBUILT':
              classes = 'invert-text-color';
              break;
            case 'BUILDING':
              classes = 'btn-info invert-text-color';
              break;

            //REMOVE ALL AND THIS.NAME
            case 'All':
              classes = 'btn-warning'
              break;

            default:
              classes = 'btn-primary';
          }

         var newDiv =
          '<button id="' + val.projectName + '" class="btn ' + buttonClass + ' ' + classes + ' col-lg-6">' + '<p>' + val.projectName + '</p>' +

              '<p class="btn btn-warning" id="' + val.projectName+ '_btn">Expand Project</p>' +
              '<p ><a class="goTo" href="' + val.projectUrl + '">' + '(Go To Project)' + '</a></p>' +

          '</button>';

           //newDiv.onclick = function() { alert('blah'); };//open_project('#main-dashboard', val.projectUrl, val.projectName);

          $(divSelector).append(newDiv);

          var openProjectBtn = document.getElementById(val.projectName+'_btn');

          openProjectBtn.addEventListener("click", function(e){
            open_project('#main-dashboard', viewUrl, val);
          }, false);

           //console.log("VIEWS: " + val.projectName);
        });
    });
}

function open_project(divSelector, viewUrl, project){

        //$(divSelector).remove();

        hide_dashboard();

        var project_container = document.getElementById('project-container');
        project_container.style.display = 'block';

        project_container_id = project.projectName + '_project_container';

        goBackBtn = '<a id="goBackBtn">Go Back</a>';

        filler = "";

                    for(var i=0; i<10; i++){
                        filler+= '<button class="btn btn-warning">' + i + '</button></br>';
                    }

        var json = JSON.stringify(project, null, 4);
        console.log(json);

        project_container_div =
        '<div id="'+ project_container_id +'">' +
        '<div class="btn btn-danger"><h4>' + project.projectName + ' project will be on this big container</h4>'+
            filler +
        '</div>' +
        '<div>' + json + '</div>' +
        '<br>' + goBackBtn +
        '</div>';


        $(project_container).append(project_container_div);

        var gBB = document.getElementById('goBackBtn'); //goBackBtn listener
        var projectDiv = document.getElementById(project_container_id);

          gBB.addEventListener("click", function(e){
            restore_dashboard();
            project_container.style.display = 'none';

            projectDiv.remove();

            this.remove();

           console.log('Removed all from project_container');

          }, false);


    console.log("open_project finish");
}

function hide_dashboard(){
    var main_dashboard = document.getElementById('main-dashboard');
    main_dashboard.style.display = 'none';

    console.log("dashboard hidden");
}

function restore_dashboard(){
    var main_dashboard = document.getElementById('main-dashboard');
    main_dashboard.style.display = 'block';

    console.log("dashboard restored");
}

function getProjectByName(data, projectName) {
    var allProjects = data.allProjects;
    for (var i = 0; i < allProjects.length; i++) {
        if (allProjects[i].projectName == projectName) {
            return(allProjects[i]);
        }
    }
}

//TODO: create the project container with all relevant info
//TODO: searches are all in js
//TODO: create a good architecture, and remove build limit, or put a build limit only for display