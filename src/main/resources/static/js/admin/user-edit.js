$(document).ready(function () {
	
	var state = {};

	$("#input-password, #input-password1").on("change", function () {
		let $password = $("#input-password");
		let $password1 = $("#input-password1");
		let validity = $password.get(0).validity;
		let validity1 = $password1.get(0).validity
	
		let message = "";
		if ((validity.valid || validity.customError) && validity1.valid && 
				!($password.val() == $password1.val())) {
			message = "The two passwords do not match!";
		}
		
		$password.get(0).setCustomValidity(message);
		return true;
	});
});