
<div id="feedback" class="well well-sm">
	<div class="feedback-in">Feedback ?</div>
	<form action="feedback" method="post" role="form">
		<div class="form-group">
			<label> 	
				<input type="email" name="email" placeholder="Your email">
			</label>
		</div>
		<div class="form-group">
			<label>Comment: (navigation, bugs, colors...) 	<br/>	
				<textarea rows="5" cols="50" name="content"></textarea>
			</label>
		</div>
	  	<button type="submit" class="btn btn-default">Submit</button>
		<button type="button" class="btn btn-default" id="closebutton">Close</button>
	</form>
</div>