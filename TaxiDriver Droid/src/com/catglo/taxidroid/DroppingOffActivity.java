package com.catglo.taxidroid;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.catglo.deliveryDatabase.DataBase;
import com.catglo.deliveryDatabase.DropOff;
import com.catglo.deliveryDatabase.Order;
import com.catglo.deliveryDatabase.TipTotalData;
import com.catglo.taxidroid.R;
import com.catglo.taxidroid.AddEditOrderBaseActivity.DropOffRow;


public class DroppingOffActivity extends AddEditOrderBaseActivity {	
	private int						listPosition;

	// Need handler for callbacks to the UI thread
	final Handler					messageHandler	= new Handler();

	private int dataBasePrimaryKey;
	Order order;

	private TextView address;

	private TextView waitTime;

	private Button multiFuntion;

	private EditText arrivialTime;

	private EditText coustomerGotInCabTime;
	
	static final int DRIVING_TO_COUSTOMER 	= 1;
	static final int WAITING_FOR_COUSTOMER 	= 2;
	static final int COUSTOMER_IN_CAB 		= 3;
	static final int COUSTOMER_OUT_OF_CAB 	= 4;
	int currentDropOff;
	
	private int pickUpState = DRIVING_TO_COUSTOMER;

	private TextView heading;

	private Button noShow;
	private Button cancelOrder;

	private AddressFinder addressFinder;

	private EditText scheduledPickUpTime;

	private ImageButton navigate;

	private Button addDropOff;

	private AutoCompleteTextView tripType;

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.out_the_door);
    
    	noShow = (Button) findViewById(R.id.button2);
    	cancelOrder = (Button) findViewById(R.id.button1);
    	addDropOff = (Button) findViewById(R.id.button3);
    	arrivialTime = (EditText) findViewById(R.id.editText2);
    	scheduledPickUpTime = (EditText) findViewById(R.id.editText1);
    	
    	address = (TextView)findViewById(R.id.currentAddress);
    	dropOffContainer = (LinearLayout) findViewById(R.id.dropOffContainer);
    	heading = (TextView) findViewById(R.id.textView4);
    	coustomerGotInCabTime = (EditText) findViewById(R.id.editText3);
    	navigate = (ImageButton) findViewById(R.id.Navigate);
		navigate.setOnClickListener(new OnClickListener(){public void onClick(View v) {
			String s = address.getText().toString().replace(' ', '+');
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q="+ s));
			startActivity(i);
		}});
		
    	
    	
		Intent intent = getIntent();
		dataBasePrimaryKey = intent.getIntExtra("DB Key", -1);
		order = dataBase.getOrder(dataBasePrimaryKey);
		dataBase.loadOrderDropOffs(order);
		currentDropOff=0;
	
		
		tripType = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView1);
    	tripType.setAdapter(dataBase.getOrderTypeAdapter());
    	tripType.setText(order.apartmentNumber);
    	tripType.setOnClickListener(new OnClickListener(){public void onClick(View v) {
    		tripType.showDropDown();
		}});
    	
		addressFinder = new AddressFinder(getApplicationContext());
		
		multiFuntion = (Button) findViewById(R.id.bigMainButton);
		multiFuntion.setOnClickListener(new OnClickListener(){public void onClick(View v) {
			switch (pickUpState) {
			case DRIVING_TO_COUSTOMER:  //Click transition from driving to customer mode to waiting for pickup
				//Set actual pickup time
				order.arivialTime.setTime(System.currentTimeMillis());
				setTimeFieldText(order.arivialTime,arrivialTime);
				multiFuntion.setText(R.string.pickup);
				heading.setText(R.string.waitingForCoustomer);
				pickUpState = WAITING_FOR_COUSTOMER;
			break;
			case WAITING_FOR_COUSTOMER: //Clicked on transition from waiting for customer to customer got in cab
				order.payedTime.setTime(System.currentTimeMillis());
				setTimeFieldText(order.payedTime,coustomerGotInCabTime);
				pickUpState = COUSTOMER_IN_CAB;
				multiFuntion.setText(R.string.dropOff);
				heading.setText(R.string.dropOffHeading);
				address.setText(order.dropOffs.get(0).address);
				
		        	
	    		final DropOffRow dr = addDropoffRow(R.layout.drop_edit_row);
	    		dr.paymentPart.setVisibility(View.GONE);
	 
	        	dr.address.setText(order.dropOffs.get(currentDropOff).address);
	        	dr.dropoffId.setText(""+order.dropOffs.get(currentDropOff).id);
	        	final int thisIndex=currentDropOff;
	        	dr.timestamp.setOnTouchListener(new OnTouchListener(){ public boolean onTouch(View arg0, MotionEvent arg1) {
	            	showTimeSliderDialog(dr.timestamp,order.dropOffs.get(thisIndex).time);
	    		    return true;
	    		}});

	        	noShow.setVisibility(View.GONE);
	        	cancelOrder.setVisibility(View.GONE);
			break;
			case COUSTOMER_IN_CAB:
				dropOffRows.get(currentDropOff).paymentPart.setVisibility(View.VISIBLE);
				LayoutParams lp = addDropOff.getLayoutParams();
				lp.width=150;
				addDropOff.setLayoutParams(lp);
				showPaymentDialog();
				
			break;
			case COUSTOMER_OUT_OF_CAB:
				dataBase.edit(order);
				finish();
				break;
			}
			
			
		}});
		
		if (intent.getBooleanExtra("immediate", false) == true){
			pickUpState = COUSTOMER_IN_CAB;
			heading.setText(R.string.dropOffHeading);
			multiFuntion.setText(R.string.dropOff);
			
			order.arivialTime.setTime(System.currentTimeMillis());
			setTimeFieldText(order.arivialTime,arrivialTime);
			order.payedTime.setTime(System.currentTimeMillis());
			setTimeFieldText(order.payedTime,coustomerGotInCabTime);
			
			address.setText(order.dropOffs.get(currentDropOff).address);
			
			final DropOffRow dr = addDropoffRow(R.layout.drop_edit_row);
    		dr.paymentPart.setVisibility(View.GONE);
 
        	dr.address.setText(order.dropOffs.get(currentDropOff).address);
        	dr.dropoffId.setText(""+order.dropOffs.get(currentDropOff).id);
        	final int thisIndex=currentDropOff;
        	dr.timestamp.setOnTouchListener(new OnTouchListener(){ public boolean onTouch(View arg0, MotionEvent arg1) {
            	showTimeSliderDialog(dr.timestamp,order.dropOffs.get(thisIndex).time);
    		    return true;
    		}});

        	noShow.setVisibility(View.GONE);
        	cancelOrder.setVisibility(View.GONE);
			order.streetHail=true;
			
			addressFinder.lookup(new AddressFinder.OnAddressFound() {public void found(String result) {
				final String thisResult = result;
				DroppingOffActivity.this.runOnUiThread(new Runnable(){public void run(){
					address.setText(thisResult);
					order.address = thisResult;
				}});
			}});
			
			/*		

			heading.setText(R.string.dropOffHeading);
			
			final DropOffRow dr = addDropoffRow(R.layout.drop_edit_row);
			
			
			dr.address.setText(order.dropOffs.get(0).address);
        	dr.dropoffId.setText(""+order.dropOffs.get(0).id);
        	dr.timestamp.setOnTouchListener(new OnTouchListener(){ public boolean onTouch(View arg0, MotionEvent arg1) {
            	showTimeSliderDialog(dr.timestamp,order.dropOffs.get(0).time);
    		    return true;
    		}});
        	
        	noShow.setVisibility(View.GONE);
        	cancelOrder.setVisibility(View.GONE);
        	*/
		} else {
			address.setText(order.address);
		}
		
        
		
		arrivialTime.setOnTouchListener(new OnTouchListener(){ public boolean onTouch(View arg0, MotionEvent arg1) {
        	showTimeSliderDialog(arrivialTime,order.arivialTime);
		    return true;
		}});
		
		coustomerGotInCabTime.setOnTouchListener(new OnTouchListener(){ public boolean onTouch(View arg0, MotionEvent arg1) {
        	showTimeSliderDialog(coustomerGotInCabTime,order.payedTime);
		    return true;
		}});
		
		setTimeFieldText(order.time,scheduledPickUpTime);
		scheduledPickUpTime.setOnTouchListener(new OnTouchListener(){ public boolean onTouch(View arg0, MotionEvent arg1) {
        	showTimeSliderDialog(scheduledPickUpTime,order.time);
		    return true;
		}});
		
		
		noShow.setOnClickListener(new OnClickListener(){public void onClick(View v) {
			showNoShowPaymentDialog();
		}});
		
		cancelOrder.setOnClickListener(new OnClickListener(){public void onClick(View v) {
			order.payed = Order.PAYMENTSTATUS_CANCELED;
			//TODO: delete any associated drop off records
			dataBase.edit(order);
			finish();
		}});
		
		
		
		
		addDropOff.setOnClickListener(new OnClickListener(){public void onClick(View v){
			DropOff d = new DropOff();
			order.dropOffs.add(d);
			final int thisIndex2 = order.dropOffs.size()-1;
			final DropOffRow dropOffRow = addDropoffRow(R.layout.drop_edit_row);
			Calendar now = Calendar.getInstance();
			now.setTimeInMillis(System.currentTimeMillis());
			dataBase.addDropoff(order.primaryKey, "", now);
			dropOffRow.timestamp.setOnTouchListener(new OnTouchListener(){ public boolean onTouch(View arg0, MotionEvent arg1) {
            	showTimeSliderDialog(dropOffRow.timestamp,order.dropOffs.get(thisIndex2).time);
    		    return true;
    		}});
        	d.time.setTime(System.currentTimeMillis());
			setTimeFieldText(d.time,dropOffRow.timestamp);
			if (pickUpState==COUSTOMER_OUT_OF_CAB){
				pickUpState=COUSTOMER_IN_CAB;
				multiFuntion.setText(R.string.dropOff);
				dropOffRow.paymentPart.setVisibility(View.GONE);
			}
			
		}});
    }
    
    
    private void showPaymentDialog(){
    	final DropOffRow thisDropOffRow = dropOffRows.get(currentDropOff);
		final int id = order.dropOffs.get(currentDropOff).id; 
		final int current = currentDropOff;
		
		//Update the form fields on dialog save
		Runnable runOnDialogSave = new Runnable(){public void run() {
			DropOff dropOff = order.dropOffs.get(current);
			order.payed = Order.PAYMENTSTATUS_PAID;
			thisDropOffRow.meterAmount.setText(getFormattedCurrency(dropOff.meterAmount));
			thisDropOffRow.payment.setText(getFormattedCurrency(dropOff.payment));
			thisDropOffRow.paymentType.setSelection(dropOff.paymentType);
			thisDropOffRow.timestamp.setText(getFormattedTime(dropOff.time));
			if (dropOff.paymentType == 1){
				thisDropOffRow.account.setVisibility(View.GONE);
				thisDropOffRow.extraLabel.setVisibility(View.GONE);
			}
		}};

    	//If the address field is left blank look up the address from the current GPS location
		if (thisDropOffRow.address.getEditableText().toString().length() < 1) {
			addressFinder.lookup(new AddressFinder.OnAddressFound() {public void found(String result) {
				final String thisResult = result;
				DroppingOffActivity.this.runOnUiThread(new Runnable(){public void run(){
					heading.setText(R.string.dropOffHeading);
					address.setText(thisResult);
					thisDropOffRow.address.setText(thisResult);
					DropOff dropOff = order.dropOffs.get(current);
					dropOff.address += thisResult;
					dataBase.editDropOff(dropOff.id, thisResult);
					
				}});
			}});
		}
		
		//If its the last drop off then switch to coutomer out of cab mode
		//otherwize add the next drop off row to the UI
		currentDropOff++;
		if (currentDropOff >= order.dropOffs.size()) {
			pickUpState = COUSTOMER_OUT_OF_CAB;
			multiFuntion.setText(R.string.done);

		} else {
			DropOff dropOff = order.dropOffs.get(currentDropOff);
			final DropOffRow dropOffRow = addDropoffRow(R.layout.drop_edit_row);
			dropOffRow.address.setText(order.dropOffs.get(currentDropOff).address);
			dropOffRow.dropoffId.setText(""+order.dropOffs.get(currentDropOff).id);
        	final int thisIndex2=currentDropOff;
        	dropOffRow.timestamp.setOnTouchListener(new OnTouchListener(){ public boolean onTouch(View arg0, MotionEvent arg1) {
            	showTimeSliderDialog(dropOffRow.timestamp,order.dropOffs.get(thisIndex2).time);
    		    return true;
    		}});
        	dropOff.time.setTime(System.currentTimeMillis());
			setTimeFieldText(dropOff.time,dropOffRow.timestamp);
		}

		final PaymentDialog dialog = new PaymentDialog(DroppingOffActivity.this,id,order.dropOffs.get(current),order,runOnDialogSave);
		dialog.show();
    }
		
	private void showNoShowPaymentDialog(){
		
    	Runnable runOnDialogSave=null;
     	final int id = order.dropOffs.get(0).id; 
		
		
		runOnDialogSave = new Runnable(){public void run() {
			order = dataBase.getOrder(order.primaryKey);
			dataBase.loadOrderDropOffs(order);
			order.payed = Order.PAYMENTSTATUS_PAID;
			order.payed = Order.PAYMENTSTATUS_NOSHOW;
			dataBase.edit(order);
			DroppingOffActivity.this.finish();	
		}};
		
	
		final PaymentDialogNoShow dialog = new PaymentDialogNoShow(DroppingOffActivity.this,id,order.dropOffs.get(currentDropOff),order,runOnDialogSave);
		dialog.show();
		
    }

    private void setTimeFieldText(Timestamp t, EditText edit){
    	Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(t.getTime());
		edit.setText(String.format("%tl:%tM %tp %ta", cal,cal,cal,cal));
    }
 
    protected void onSaveInstanceState (Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt("pickUpState", pickUpState);
	}
	
	protected void onRestoreInstanceState (Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		pickUpState = savedInstanceState.getInt("pickUpState");
		switch (pickUpState) {
		case WAITING_FOR_COUSTOMER:  //Click transition from driving to customer mode to waiting for pickup
			multiFuntion.setText(R.string.pickup);
			heading.setText(R.string.waitingForCoustomer);
		break;
		case COUSTOMER_IN_CAB: //Clicked on transition from waiting for customer to customer got in cab
			multiFuntion.setText(R.string.dropOff);
			heading.setText(R.string.dropOffHeading);
			address.setText(order.dropOffs.get(0).address);
        	noShow.setVisibility(View.GONE);
        	cancelOrder.setVisibility(View.GONE);
		break;
		case COUSTOMER_OUT_OF_CAB:
			dropOffRows.get(currentDropOff).paymentPart.setVisibility(View.VISIBLE);
			LayoutParams lp = addDropOff.getLayoutParams();
			lp.width=150;
		break;
		
		}
	}
	


}