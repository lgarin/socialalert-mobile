package com.bravson.socialalert.android;

import java.util.Collections;
import java.util.List;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.Validator.Mode;
import com.mobsandgeeks.saripaar.Validator.ValidationListener;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public abstract class ValidatedActivity extends Activity implements ValidationListener {

	private final Validator validator = new Validator(this);
	
	private List<ValidationError> validationErrors; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		validator.setValidationMode(Mode.IMMEDIATE);
		validator.setValidationListener(this);
	}
	
	@Override
	public void onValidationSucceeded() {
		validationErrors = Collections.emptyList();
	}
	
	@Override
	public void onValidationFailed(List<ValidationError> errors) {
		
		ValidationError error = errors.get(0);
		String message = error.getCollatedErrorMessage(this);
		View view = error.getView();
		if (view instanceof EditText) {
			EditText textField = (EditText) view;
			textField.setError(message);
			textField.requestFocus();
		}
	}
	
	public boolean validate() {
		validationErrors = null;
		validator.validate();
		return validationErrors != null && validationErrors.isEmpty();
	}
}
