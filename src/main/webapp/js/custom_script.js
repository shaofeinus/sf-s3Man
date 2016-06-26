/**
 * 
 */

// Prepares a standard directory string
function getDirectory(dir) {
	
	if(dir == null || uploadDir == '')
		dir = '/';
	
	dir = dir.replace(/\\/g, "/");
	
	if(dir.charAt(0) != '/')
		dir = '/' + dir;
	
	if(dir.charAt(dir.length - 1) != '/')
		dir += '/'; 
	
	return dir;
}

function initDelete() {
	// Delete selected files
	 $('#deleteSelected').click( function () {
		 
		 var filesTable = $('#filesTable').DataTable();
		 var rows = filesTable.rows('.selected').data();
		 
		 if(rows.length == 0) {
			 alert("No files selected");
			 return;
		 }
			 
		 var fileNamesString = "";
		 for(var i = 0; i < rows.length; i++)
			 fileNamesString += (rows[i].name + "\n");
		 
		 if(confirm('Delete these files?\n' + fileNamesString)) {
			 var filePaths = new Array();
			 for(var i = 0; i < rows.length; i++)
				 filePaths[i] = rows[i].directory + rows[i].name;
			 
			 $.get(
					 'action/delete',
					 {'filePaths': filePaths},
					 function() {location.reload(true);}
					 );
		 }
	 });
}

function initUpload() {
	// Confirm upload directory
	$("#fileUploader").click(function() {
		var uploadDir = getDirectory($("#uploadDir").val());
		if(confirm('Upload to directory "' + uploadDir + '"?')) {
			$.ajax({
				method: 'POST',
				url: 'action/setUploadDir',
				data: {'uploadDir': uploadDir},
				async: false
			})
			return true;
		} else 
			return false;
	});
	
	// File upload definition
	$("#fileUploader").uploadFile({
		url : "action/upload",
		multiple : true,
		dragDrop: false,
		onSelect: function() {
		    return true;
		},
		onSuccess: function() {
		    location.reload(true);
		}
	});
}

function initFileTable() {
	
	// Initialize Datatable
	$('#filesTable').DataTable({
		ajax: {
			url: 'action/view',
			dataSrc: 'fileList',
			
		},
		
		columnDefs: [
		             // Check box column
		             {
		            	 targets: [0], 
		            	 data: null, 
		            	 width: '5%', 
		            	 orderable: false,
		            	 className: 'select-checkbox', 
		            	 defaultContent: ''
		             },
		             
		             // Data columns
		             {targets: [1], data: 'directory'},
		             {targets: [2], data: 'name'},
		             {targets: [3], data: 'size', 
		            	 render: function(data, type, row, meta) {
		            		 return data.toFixed(2);
	            	 }},
		             {targets: [4], data: 'lastModifiedDate'},
		             
		             // Click to view column
		             {
		            	 targets: [5], 
		            	 data: null,
		            	 orderable: false,
	            		 render: function(data, type, row) {
	            			 var link = 'action/view?fileName=' + row.directory + row.name;
		            		 return '<a href="' + link + '" target="_blank">View</a>';
		            	 },
		             }
		             ],
	    
	    order: [[1, 'asc'], [4, 'desc']],
		  
		initComplete: function() {
			var column = this.api().column(1);
			var select = $('<select><option value="">All</option></select>')
			.appendTo($(column.footer()))
            .on( 'change', function () {
                var val = $.fn.dataTable.util.escapeRegex(
                    $(this).val()
                );

                column
                    .search( val ? '^'+val+'$' : '', true, true, true )
                    .draw();
            });
			
			column.data().unique().sort().each( function ( d, j ) {
                select.append( '<option value="'+d+'">'+d+'</option>' )
            } );
		}
	 });
	
	// Checkbox definition
	 $('#filesTable tbody').on( 'click', 'tr td:first-child', function () {
		 $(this).closest('tr').toggleClass('selected');
	 });
}

$(document).ready(function() {
	
	// Upload function definition
	initUpload();
	
	// File table definition
	initFileTable();

	// Delete function definition
	initDelete();
	 
});