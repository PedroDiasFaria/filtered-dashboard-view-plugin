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
      //taskName = val.task.name.replace(/(,?)\w*=/g, "$1");  //TODO Uncaught TypeError: Cannot read property 'replace' of undefined
      taskName = val.task.name;
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
    $.each( data.buildHistory, function( key, val ) {
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
      newRow = '<tr class="' + classes + '"><td class="text-left">' + jobName + '</td><td><a href="'+ val.buildUrl +'" >' + val.number + '</a></td><td>' + format_date(dt) + '</td><td>' + format_interval(val.duration) + '</td></tr>';
      $(tableSelector + ' tbody').append(newRow);
    });
  });
}

function reload_jenkins_jobs(divSelector, viewUrl, buttonClass) {
  $.getJSON( viewUrl + '/api/json', function( data ) {
    // Remove all existing divs
    $(divSelector + ' button').remove();
    $.each( data.allJobs, function( key, val ) {
      switch (val.status.value) {
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
      '<button id="' + val.jobName.value + '" class="btn ' + buttonClass + ' ' + classes + ' col-lg-6">' + '<p>' + val.jobName.value + '</p>' +
            '<div id="expandable_'+val.jobName.value+'" class="">' +
                expandable +
            '</div>' +
          '<p><a class="goTo" href="' + val.jobUrl.value + '">' + '(Go To Job)' + '</a></p>' +
      '</button>';

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
            case 'All':
              classes = 'btn-warning'
              break;

            default:
              classes = 'btn-primary';
          }

         var newDiv =
          '<button id="' + val.projectName + '" class="btn ' + buttonClass + ' ' + classes + ' col-lg-6">' + '<p>' + val.projectName + '</p>' +
              '<p class="btn" id="' + val.projectName+ '_btn">Expand Project</p>' +
              '<p ><a class="goTo" href="' + val.projectUrl + '">' + '(Go To Project)' + '</a></p>' +
          '</button>';

          $(divSelector).append(newDiv);

          var openProjectBtn = document.getElementById(val.projectName+'_btn');

          openProjectBtn.addEventListener("click", function(e){
            open_project('#main-dashboard', viewUrl, val);
          }, false);
        });
    });
}

function open_project(divSelector, viewUrl, project){

        hide_dashboard();

        var project_container = document.getElementById('project-container');
        project_container.style.display = 'block';

        project_container_id = project.projectName + '_project_container';

        goBackBtn = '<button class="btn btn-default btn-sm goBackBtn" id="goBackBtn">Go Back</button>';

        var projectTable = createTable(project.projectJobs);
        var projectTags = createTags(project.projectJobs);
        var tagsFilter = createTagsFilter(projectTags);

        project_container_div =
        '<div id="'+ project_container_id +'">' +
        '<div><h1><strong>&nbsp&nbsp' + project.projectName + '</strong></h1></div>' +
        '<br><div class="container col-sm-12">' + goBackBtn +  '</div>' +
        '<br><div class="container col-sm-12">' + tagsFilter + '</div>' +
        '<br><div class="container col-sm-12">' + projectTable + '</div>' +
        '<br><div class="container col-sm-12">' + goBackBtn +  '</div>' +
        '</div>';

        $(project_container).append(project_container_div);
        var table = $('#project-table').DataTable({
            "order" : [0, 'asc'],
            "sPaginationType": 'full_numbers',
            "aoColumnDefs" : [
                { 'bSortable': false, 'aTargets': ['no-sort'] }
            ],
            searchHighlight: true
        });

        $('input.filter').on('change', function () {
            var filter = [];
            $('.filter').each(function (index, elem) {
                if (elem.checked) filter.push($(elem).val());
            });
            var filterString = filter.join(' ');

            //If you want to search by 'OR' instead of 'AND' in the filters:
            /*
            var filterString = filter.join('|');
            var filterRegex = "";
            if(filterString)
              filterString = '^(?=.*?(' + filterString + ')).*?';       //'OR' regex
            */

            //Hides columns that don't have selected filters
            table.columns().every( function () {
                var column = this;
                if(column.index() > 0 ){
                    //Resets visibility
                    column.visible(true);
                    if(filter.length > 0){
                        //If the filter isn't found...
                        var found =
                        filter.some(function(elem){
                            return table.columns().data()[column.index()].join().includes(elem);
                        })
                        //...hides the column
                        column.visible(found);
                    }
                }
            } );

            table.search(filterString, true).draw();
            table.search('');                   //clears the 'search' form
        });

        var projectDiv = document.getElementById(project_container_id);
        var gBB = $('.goBackBtn');
        gBB.on("click", function(){
                    restore_dashboard();
                    project_container.style.display = 'none';
                    projectDiv.remove();
                    this.remove();
        });
}

function hide_dashboard(){
    var main_dashboard = document.getElementById('main-dashboard');
    main_dashboard.style.display = 'none';
}

function restore_dashboard(){
    var main_dashboard = document.getElementById('main-dashboard');
    main_dashboard.style.display = 'block';
}

function getProjectByName(data, projectName) {
    var allProjects = data.allProjects;
    for (var i = 0; i < allProjects.length; i++) {
        if (allProjects[i].projectName == projectName) {
            return(allProjects[i]);
        }
    }
}

//populate the table
//there can be rows(builds) without any value for its job
var createTable = function(projectJobs, tableSize){

        var projectTable = new tableClass();

        for(let job of projectJobs){
            var rowIndex = 1;
            newCol = {jobName : "", url: ""};
            newCol.jobName = job.jobName.value;
            newCol.url = job.jobUrl.value;
            projectTable.columns.push(newCol);
            for(let build of job.builds){
                newCell = {url : "", result : "", jobName : "", tags : "", buildNr : ""};
                newCell.url = build.buildUrl;
                newCell.result = build.result;
                newCell.jobName = job.jobName.value;
                newCell.buildNr = build.number;
                newCell.tags = build.tags;
                if(projectTable.rows[rowIndex])
                    projectTable.rows[rowIndex].push(newCell);
                else{
                    projectTable.rows[rowIndex] = [];
                    projectTable.rows[rowIndex].push(newCell);
                    }
                rowIndex++;
            }
        }

        var thead = "";
        var tbody = "";

        for(let column of projectTable.columns){
            thead+='<th class="no-sort job-name-header"><div><span><a href="'+ column.url +'" target="_blank"><h4>' + column.jobName + '</h4></a></span></div></th>';
        }

        for(var rowNr in  projectTable.rows){
            if(projectTable.rows.hasOwnProperty(rowNr)){
                //You can search by 'row:nr' to display only that row: e.g. row:5
                tbody+= '<tr><td class="row-nr">'+  rowNr + '</td>';
                for(let column of projectTable.columns){
                    rowArray = projectTable.rows[rowNr];
                    newCell = "";
                    for(let cell of rowArray){
                        if(cell.jobName == column.jobName ){
                            newCell= '<td class="cell ' + cell.result + '">';
                            newCell+= '<span style="display:none">row:'+ rowNr +'</span>';
                            if(cell.url == "null"){
                                newCell+= '<a href="javascript:;"><div>' + cell.result+ '</div>';
                            }else{
                                newCell+= '<a href="'+ cell.url +'" target="_blank" style="text-decoration:none">' +
                                            '<div>' +
                                            cell.result + '<br>#' +
                                            cell.buildNr +
                                            '</div>';
                            }
                            //only show if there are tags
                            if(cell.tags.length > 0 ){
                                newCell+= '<span class="callTags"><b>Tags: </b>';
                                for(let tag of cell.tags){
                                    newCell+= tag.value + ' ';
                                }
                                newCell+= '</span>';
                            }
                            newCell+= '</a></td>';
                        }
                    }
                    if(newCell){
                        tbody+=newCell;
                    }else{
                        tbody+= '<td class="cell"> NO DATA </td>';
                    }
                }
                tbody+= '</tr>';
            }
        }

        table =             '<table id="project-table" class="project-table table table-striped" cellspacing="0" width="100%">' +
                                '<thead>'+
                                    '<tr><th style="width: 3%" class="text-left "><h4>Row#</h4></th>' + thead + '</tr>'+
                                '</thead>'+
                                '<tbody>'+
                                    tbody +
                                '<tbody>'+
                            '</table>';

        return table;
}

function jobsHide(idButton, height){
       $(idButton).click(function(){
            if( $('#jenkinsJobsDiv').css('display') == 'none' ){
                $('#jenkinsJobsDiv').css('display', 'block');
                $('#jenkinsProjectsDiv').css('height', height + '%');
                $(idButton).html("Hide Jobs");
            }else{
                $('#jenkinsJobsDiv').css('display', 'none');
                $('#jenkinsProjectsDiv').css('height', '100%');
                $(idButton).html("Show Jobs");
            }
        });
}

var createTags = function(projectJobs){

    var tags = new Array();

    for(let job of projectJobs){
        for(let build of job.builds){
            for(let tag of build.tags){
                if(!tags.hasOwnProperty(tag.label)){
                    tags[tag.label] = [];
                }
                if(tags[tag.label].indexOf(tag.value) === -1){
                    tags[tag.label].push(tag.value);
                }
            }
        }
    }
    return tags;
}

var createTagsFilter = function(tags){

    var tagsDiv = '<div class="tagsDiv" id="tagsDiv" ><h3><b>Filter by:</b></h3>';

    for(var tag in tags){
        if(tags.hasOwnProperty(tag)){
            tagsDiv+= '<div class="container" style="width: 250px; float:left; border:2px #4e5d6c solid"><h4><b>' + tag + '</b></h4>';
            tagsDiv+= '<ul class="list-group">';
            values = tags[tag];
                for(let value of values){
                   id = tag +'_'+ value;
                    tagsDiv+=   '<li class="list-group-item">';
                        tagsDiv+= value;
                        tagsDiv+=   '<div class="material-switch pull-right">'+
                                        '<input id="'+ id +'" type="checkbox" class="filter" value="' + value + '" name="'+ id +'"/>'+
                                        '<label for="' + id + '" class="label-primary"></label>'+
                                    '<div>'+
                                '</li>';
              }
            tagsDiv+='</ul></div>';
        }
    }
    tagsDiv+= '</div><br>';
    return tagsDiv;
}

var tableCell = function(url, status){
    this.url = url;
    this.status = status;
}

var tableClass = function(){
    this.columns = [];
    this.rows = [];
}