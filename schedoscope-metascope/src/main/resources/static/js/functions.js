/**
 * Copyright 2015 Otto (GmbH & Co KG)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var initializeFieldEditor = function(fieldname) {
  var fieldEditor = "#" + fieldname + "Editor";
  $(fieldEditor).summernote(
      {
        height : 150,
        minHeight : null,
        maxHeight : null,
        focus : true,
        toolbar : [ [ 'style', [ 'style' ] ], [ 'font', [ 'bold', 'italic', 'underline', 'clear' ] ],
            [ 'fontname', [ 'fontname' ] ], [ 'fontsize', [ 'fontsize' ] ], [ 'color', [ 'color' ] ],
            [ 'para', [ 'ul', 'ol', 'paragraph' ] ], [ 'height', [ 'height' ] ], [ 'table', [ 'table' ] ],
            [ 'insert', [ 'link', 'hr' ] ], [ 'view', [ 'codeview' ] ], [ 'help', [ 'help' ] ] ],
      });
  $(fieldEditor).on('summernote.change', function(customEvent, contents, $editable) {
    console.log($(fieldEditor).code());
    $('#' + fieldname + 'Input').val($(fieldEditor).code());
  });

  var fieldCommentEditor = "#" + fieldname + "CommentEditor";
  $(fieldCommentEditor).summernote(
      {
        height : 75,
        minHeight : null,
        maxHeight : null,
        focus : true,
        toolbar : [ [ 'font', [ 'bold', 'italic', 'underline', 'clear' ] ], [ 'fontname', [ 'fontname' ] ],
            [ 'color', [ 'color' ] ], [ 'para', [ 'ul', 'ol', 'paragraph' ] ], [ 'insert', [ 'link' ] ], ],
      });
  $(fieldCommentEditor).on('summernote.change', function(customEvent, contents, $editable) {
    $('#' + fieldname + 'CommentInput').val($(fieldCommentEditor).code());
  });
}

var initTablesorter = function() {
  $('#partitionsTable,#schemaTable,#parameterTable').each(function() {
    $(this).tablesorter({
      widgets : [ 'zebra', 'columns' ],
      usNumberFormat : false,
      sortReset : true,
      sortRestart : true
    });
  });
}

/**
 * sets a cookie for a filter (open / closed state)
 */
var setFilterStatus = function(e, filterName) {
  var open = !$('#' + filterName).is(':visible');
  if (open) {
    $.cookie(filterName, "open");
    $('#' + e.id).children().eq(1).replaceWith(
        '<span id="#' + filterName + 'Caret" class="dropup"><span class="caret"></span></span>')
  } else {
    $.cookie(filterName, "closed");
    $('#' + e.id).children().eq(1).replaceWith('<span id="#' + filterName + 'Caret" class="caret"></span>')
  }
}

/**
 * clears the search field and triggers a query
 */
var clearSearchField = function() {
  $('#searchInputField').val('');
  $("#filterForm").submit()
}

var preSubmitFilterForm = function() {
  var params = getQueryParameter();
  for ( var key in params) {
    if (!(key === "searchQuery") && !(key === "e") && !(key === "p") && !(key === "p")) {
      $('<input />').attr('type', 'hidden').attr('name', key).attr('value', params[key]).appendTo('#filterForm');
    }
  }
  return true;
}

var increaseViewCount = function(fqdn) {
  $.ajax({
    url : '/table/viewcount',
    type : 'POST',
    data : {
      fqdn : fqdn
    }
  });
}

/**
 * Gets the query parameters from the URL
 */
var getQueryParameter = function() {
  var query_string = {};
  var query = window.location.search.substring(1);
  var vars = query.split("&");
  for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split("=");
    if (typeof query_string[pair[0]] === "undefined") {
      query_string[pair[0]] = decodeURIComponent(pair[1]);
    } else if (typeof query_string[pair[0]] === "string") {
      var arr = [ query_string[pair[0]], decodeURIComponent(pair[1]) ];
      query_string[pair[0]] = arr;
    } else {
      query_string[pair[0]].push(decodeURIComponent(pair[1]));
    }
  }
  return query_string;
}

var createCategory = function(taxonomyId) {
  $('#taxonomyId').val(taxonomyId);
  $('#createCategoryModal').modal('show');
}

var createCategoryObject = function(categoryId) {
  $('#createCategoryObjectCategoryId').val(categoryId);
  $('#createCategoryObjectModal').modal('show');
}

var editTaxonomy = function(taxonomyId, taxonomyName) {
  $('#editTaxonomyId').val(taxonomyId);
  $('#editTaxonomyName').val(taxonomyName);
  $('#editTaxonomyModal').modal('show');
}

var editCategory = function(categoryId, categoryName) {
  $('#editCategoryId').val(categoryId);
  $('#editCategoryName').val(categoryName);
  $('#editCategoryModal').modal('show');
}

var editCategoryObject = function(categoryObjectId, categoryObjectName, categoryDescription) {
  $('#editCategoryObjectId').val(categoryObjectId);
  $('#editCategoryObjectName').val(categoryObjectName);
  $('#editCategoryObjectDescription').val(categoryDescription);
  $('#editCategoryObjectModal').modal('show');
}

var deleteTaxonomy = function(taxonomyId) {
  $('#deleteTaxonomyTaxonomyId').val(taxonomyId);
  $('#deleteTaxonomyModal').modal('show');
}

var deleteCategory = function(categoryId) {
  $('#deleteCategoryCategoryId').val(categoryId);
  $('#deleteCategoryModal').modal('show');
}

var deleteCategoryObject = function(categoryObjectId) {
  $('#deleteCategoryObjectCategoryObjectId').val(categoryObjectId);
  $('#deleteCategoryObjectModal').modal('show');
}

var showCategoryObjects = function(taxonomyId, categoryId) {
  $(".coTable" + taxonomyId).css("display", "none");
  $("#categoryObjects" + categoryId).css("display", "inline");
}

var editUser = function(username, email, fullname, admin, group) {
  $('#editUsername').val(username);
  $('#editEmail').val(email);
  $('#editFullname').val(fullname);
  $('#adminCheckbox').prop('checked', admin);
  $("#editUserGroup").val(group);
  $("#editUserModal").modal('show');
}

/**
 * Opens the 'delete user' modal
 */
var deleteUser = function(username) {
  $('#confirmLabel').text("Do you realy want to delete the user '" + username + "' ?");
  $('#delUsername').val(username);
  $("#deleteUserModal").modal('show');
}

/**
 * Adds a business object tag
 */
var addCo = function(taxonomy, name, description, category, id) {
  if ($("[id='" + taxonomy + category + name + "']").length == 0) {
    var div = $("[id='" + taxonomy + "Div']").find(".bootstrap-tagsinput");
    var tag = $('<a id="' + taxonomy + category + name
        + '" style="margin-right: 6px;" data-toggle="popover" class="tag label label-info" data-original-title="'
        + name + ' (Category: ' + category + ')" data-placement="bottom" data-content="' + description
        + '\n\n" data-coid="' + id + '">' + name + '<span data-role="remove"></span></a></div>');
    div.prepend(tag);
    tag.after(' ');
    $('[data-toggle="tooltip"]').tooltip()
    $('[data-toggle="popover"]').popover({
      trigger : "hover"
    })
    $("[id='" + taxonomy + "Div']").find("span").on('click', function() {
      var text = $(this).parent().text();
      $(this).parent().remove();
      $('[role="tooltip"]').remove();
    });
  }
}

/**
 * AShows the documentation for the selected field
 */
var showFieldDocu = function(id) {
  $('.hide-elem').each(function() {
    $(this).css("display", "none");
  });
  $('.show-elem').each(function() {
    $(this).css("display", "inline");
  });
  var targetDiv = $("#" + id);
  $('.fieldDocumentation').css("display", "none");
  targetDiv.css("display", "inline");
  window.scrollTo(0, document.body.scrollHeight);
}

/**
 * Shows / Hides the identity table
 */
var hideExpandIdentity = function() {
  var targetDiv = $('#identityTable');
  targetDiv.toggle();
  if (!targetDiv.is(':visible')) {
    $('#minimizeButtonCaret').addClass("glyphicon-triangle-bottom");
    $('#minimizeButtonCaret').removeClass("glyphicon-triangle-top");
  } else {
    $('#minimizeButtonCaret').removeClass("glyphicon-triangle-bottom");
    $('#minimizeButtonCaret').addClass("glyphicon-triangle-top");
  }
}

/**
 * Sets the input of a partition filter in 'Sample' section
 */
var setSampleFilter = function(id, value) {
  $("#" + id).val(value)
}

var getSample = function(fqdn) {
  $.ajax({
    url : '/table/view/sample',
    type : 'GET',
    data : {
      fqdn : fqdn
    },
    success : setSamples
  });
  $.ajax({
    url : '/table/view/samplefilter',
    type : 'GET',
    data : {
      fqdn : fqdn
    },
    success : initSampleFilter
  });
}

var getViews = function(fqdn, viewPage) {
  $.ajax({
    url : '/table/view/views',
    type : 'GET',
    data : {
      fqdn : fqdn,
      partitionPage : viewPage
    },
    success : setViews
  });
}

var getParameterValue = function(fqdn, next) {
  $.ajax({
    url : '/table/view/parametervalues',
    type : 'GET',
    dataType : "html",
    data : $('#datadisForm').serialize() + "&next=" + next,
    success : function(html) {
      initDataDisFilter(html, next)
    }
  });
}

/**
 * Gets a sample for a table and specified parameters
 */
var filterSamples = function() {
  $.ajax({
    url : '/table/view/sample',
    type : 'GET',
    data : $('#sampleForm').serialize(),
    success : setSamples
  });
}

/**
 * Displays the sample
 */
var setSamples = function(data) {
  $("#samples").html(data);
}

var initSampleFilter = function(data) {
  $("#sampleForm").html(data);
}

var setViews = function(data) {
  $("#partitionsBody").html(data);
  $("#partitionSection").show();
  $("#loadingViewsLabel").hide();

  $('.showFirst').click(function() {
    var link = $(this);
    var list = $(this).next().slideToggle(function() {
      if (list.is(":visible")) {
        link.children().eq(1).replaceWith('<span class="dropup"><span class="caret"></span></span>');
      } else {
        link.children().eq(1).replaceWith('<span class="caret"></span>');
      }
    });
    $('.showFirst > li').not(this).find('ul').slideUp();
  });

  var url = document.location.toString();
  if (url.match('#')) {
    var urlPart = url.split('#')[1];
    if (urlPart.split('-')[0] === 'partitionsContent') {
      var row = $('#' + urlPart.split('-')[1]);
      row.children().each(function() {
        $(this).css('background-color', 'rgba(153, 222, 255, 0.2)');
      })
      setTimeout(function() {
        $('html,body').animate({
          scrollTop : row.offset().top - 70
        }, 'slow');
      }, 100);
    }
  }
}

var initDataDisFilter = function(data, next) {
  $("#dd" + next).html(data);
  $("#dd" + next).css("opacity", "1");
  $("#dd" + next).css("pointer-events", "auto");
}

var setDataDisFilter = function(id, value, next, fqdn) {
  $("#" + id).val(value);
  var param = $("#" + id).attr("name");
  var thisPosition = $("#dd" + param).data("nr");
  $('.datadis').each(function(i, obj) {
    var pos = $(this).data("nr");
    if (pos > thisPosition) {
      $(this).css("opacity", "0.4");
      $(this).css("pointer-events", "none");
      $("#" + pos + "DDFilter").val("");
      $("#datadisButton").prop("disabled", true);
    }
  });
  getParameterValue(fqdn, next);
  if (next === 'null') {
    $("#datadisButton").prop("disabled", false);
  }
}

/**
 * Shows the 'Write a comment' editor
 */
var expandFunction = function(id) {
  $('#' + id + 'CommentEditorSection').css("display", "inline");
  window.scrollTo(0, document.body.scrollHeight);
}

/**
 * Edit comment
 */
var editComment = function(commentDiv) {
  var div = $("#" + commentDiv);
  div.toggle();
  var html = div.children().first().html();

  var editor = "#" + commentDiv + "editEditor";
  $(editor).summernote(
      {
        height : 75,
        minHeight : null,
        maxHeight : null,
        focus : true,
        toolbar : [ [ 'font', [ 'bold', 'italic', 'underline', 'clear' ] ], [ 'fontname', [ 'fontname' ] ],
            [ 'color', [ 'color' ] ], [ 'para', [ 'ul', 'ol', 'paragraph' ] ], [ 'insert', [ 'link' ] ], ],
      });

  $(editor).on('summernote.change', function(customEvent, contents, $editable) {
    $('#' + commentDiv + 'text').val($(editor).code());
  });

  $(editor).code(html);

  var editorSection = "#" + commentDiv + "editEditorSection";
  $(editorSection).toggle();
}

/**
 * Shows the 'Create Documentation' editor and hides the current documentation
 */
var toggleEditDocu = function(id, hideTextWrapper) {
  $('#' + id + 'TextWrapper').toggle();
  $('#' + id + 'DocuEditButton').toggle();
  $('#' + id + 'EditorWrapper').toggle();
  $('#' + id + 'SubmitDocuButton').toggle();
  $('#' + id + 'CancelDocuButton').toggle();

  if (hideTextWrapper) {
    $('#' + id + 'TextWrapper').hide();
  }
}

var successToast = function(title, message) {
  $.toast({
    text : message,
    heading : title,
    icon : 'success',
    showHideTransition : 'fade',
    allowToastClose : true,
    hideAfter : 2500,
    position : 'top-center',
    textAlign : 'left',
    loader : false
  });
}

