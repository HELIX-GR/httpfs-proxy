$(document).ready(function () {
	
	var state = {
		users: {
			currentId: null
	    },
	};
	
	var renderButtons = function() {
		if (state.users.currentId == null) 
			$("a#edit-user-btn, a#delete-user-btn").addClass('disabled')
		else
			$("a#edit-user-btn, a#delete-user-btn").removeClass('disabled')
	};
	
	var renderTable = function() {
		$("#users-table tr.user-info")
	      .removeClass("table-active");
		$("#users-table tr.user-info")
		  .filter('[data-user-id="' + (state.users.currentId || '') + '"]')
		  .addClass("table-active")
	};
	
	var renderPage = function() {
		renderButtons();
		renderTable();
	}; 
	
	var selectUser = function(userId) {
		state.users.currentId = state.users.currentId == userId? null : userId;
		renderPage();
	};
	
	// Add event listeners
	
	$("#users-table tr.user-info").on('click', function (ev) {
		let userId = $(ev.target).closest("tr.user-info").data("user-id");
		selectUser(userId);
		return false;
	});
		
	$("#edit-user-btn").on('click', function(ev) {
		window.location.assign(`/admin/users/${state.users.currentId}/edit`);
		return false;
	});
	
	$("#delete-user-btn").on('click', function(ev) {
		window.location.assign(`/admin/users/${state.users.currentId}/delete`);
		return false;
	});
});