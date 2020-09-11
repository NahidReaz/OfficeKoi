package com.example.officekoi;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class CustomerMapActivity<GeoLocation> extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;

    private Button mLogout, mRequest, mSettings, mHistory;

    private LatLng desiredLocation;

    private Boolean requestBol = false;

    

    private SupportMapFragment mapFragment;

    private String  requestBooking;

    private String mRadioGroup;

    private LinearLayout mOwnerInfo;

    private ImageView mOwnerProfileImage;

    private TextView mOwnerName, mOwnerPhone, mOwnerSpaceAddress, mLocationCallback;

    

    private RatingBar mRatingBar;
    private Object supportFragmentManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_costumer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        destinationLatLng = new LatLng(0.0,0.0);

        mOwnerInfo = (LinearLayout) findViewById(R.id.ownerInfo);

        mOwnerProfileImage = (ImageView) findViewById(R.id.ownerProfileImage);

        mOwnerName = (TextView) findViewById(R.id.ownerName);
        mOwnerPhone = (TextView) findViewById(R.id.ownerPhone);
        mOwnerSpaceAddress = (TextView) findViewById(R.id.ownerSpaceAddress);

        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);


        mLogout = (Button) findViewById(R.id.logout);
        mRequest = (Button) findViewById(R.id.request);
        mSettings = (Button) findViewById(R.id.settings);
        mHistory = (Button) findViewById(R.id.history);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return();
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (requestBol){
                    endBooking();


                }else{
                    int selectId = mRadioGroup.getCheckedRadioButtonId();

                    final RadioButton radioButton = (RadioButton) findViewById(selectId);

                    if (radioButton.getText() == null){
                        return;
                    }

                    requestBooking = radioButton.getText().toString();

                    requestBol = true;

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    /*pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup))); */

                    mRequest.setText("Getting your Space....");

                    getClosestOwner();
                }
            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                startActivity(intent);

            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrOwner", "Customers");
                startActivity(intent);

            }
        });

        PlaceAutoCompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {

            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
            }
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
            }
        });


    }



    private int radius = 1;
    private Boolean ownerFound = false;
    private String ownerFoundID;

    GeoQuery geoQuery;
    private void getClosestOwner(){
        DatabaseReference ownerLocation = FirebaseDatabase.getInstance().getReference().child("ownersAvailable");

        GeoFire geoFire = new GeoFire(ownerLocation);
        /*geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);*/
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            private String key;
            private GeoLocation location;

            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!ownerFound && requestBol){
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Owners").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                                Map<String, Object> ownerMap = (Map<String, Object>) dataSnapshot.getValue();
                                if (ownerFound){
                                    return;
                                }

                                if(ownerMap.get("booking").equals(requestBooking)){
                                    ownerFound = true;
                                    ownerFoundID = dataSnapshot.getKey();

                                    DatabaseReference ownerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Owners").child(ownerFoundID).child("customerRequest");
                                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("customerSpaceId", customerId);
                                    /*map.put("destination", destination);
                                    map.put("destinationLat", destinationLatLng.latitude);
                                    map.put("destinationLng", destinationLatLng.longitude);*/
                                    ownerRef.updateChildren(map);

                                    getOwnerLocation();
                                    getOwnerInfo();
                                    getHasBookingEnded();
                                    mRequest.setText("Looking for Owner Location....");
                                }
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

                this.key = key;
                this.location = location;
            }

            @Override
            public void onGeoQueryReady() {
                if (!ownerFound)
                {
                    radius++;
                    getClosestOwner();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    /*-------------------------------------------- Map specific functions -----
    |  Function(s) getDriverLocation
    |
    |  Purpose:  Get's most updated driver location and it's always checking for movements.
    |
    |  Note:
    |	   Even tho we used geofire to push the location of the driver we can use a normal
    |      Listener to get it's location with no problem.
    |
    |      0 -> Latitude
    |      1 -> Longitudde
    |
    *-------------------------------------------------------------------*/
    private Marker mOwnerMarker;
    private DatabaseReference ownerLocationRef;
    private ValueEventListener ownerLocationRefListener;
    private void getOwnerLocation(){
        ownerLocationRef = FirebaseDatabase.getInstance().getReference().child("ownersBooked").child(ownerFoundID).child("l");
        ownerLocationRefListener = ownerLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && requestBol){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng ownerLatLng = new LatLng(locationLat,locationLng);
                    if(mOwnerMarker != null){
                        mOwnerMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(LastLocation.latitude);
                    loc1.setLongitude(LastLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(ownerLatLng.latitude);
                    loc2.setLongitude(ownerLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if (distance<100){
                        mRequest.setText("Found Nearby Space...");
                    }else{
                        mRequest.setText("Space Found: " + String.valueOf(distance));
                    }



                    mOwnerMarker = mMap.addMarker(new MarkerOptions().position(ownerLatLng).title("your desired space").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    /*-------------------------------------------- getOwnerInfo -----
    |  Function(s) getOwnerInfo
    |
    |  Purpose:  Get all the user information that we can get from the user's database.
    |
    |  Note: --
    |
    *-------------------------------------------------------------------*/
    private void getOwnerInfo(){
        mOwnerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Owners").child(ownerFoundID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    if(dataSnapshot.child("name")!=null){
                        mOwnerName.setText(dataSnapshot.child("name").getValue().toString());
                    }
                    if(dataSnapshot.child("phone")!=null){
                        mOwnerPhone.setText(dataSnapshot.child("phone").getValue().toString());
                    }
                    if(dataSnapshot.child("spaceaddress")!=null){
                        mOwnerSpaceAddress.setText(dataSnapshot.child("spaceaddress").getValue().toString());
                    }
                    if(dataSnapshot.child("profileImageUrl").getValue()!=null){
                        Glide.with(getApplication()).load(dataSnapshot.child("profileImageUrl").getValue().toString()).into(mOwnerProfileImage);
                    }

                    int ratingSum = 0;
                    float ratingsTotal = 0;
                    float ratingsAvg = 0;
                    for (DataSnapshot child : dataSnapshot.child("rating").getChildren()){
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingsTotal++;
                    }
                    if(ratingsTotal!= 0){
                        ratingsAvg = ratingSum/ratingsTotal;
                        mRatingBar.setRating(ratingsAvg);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private DatabaseReference bookingHasEndedRef;
    private ValueEventListener bookingHasEndedRefListener;
    private void getHasBookingEnded(){
        bookingHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Owners").child(ownerFoundID).child("customerRequest").child("customerBookingId");
        bookingHasEndedRefListener = bookingHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                }else{
                    endBooking();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void endBooking(){
        requestBol = false;
        geoQuery.removeAllListeners();
        ownerLocationRef.removeEventListener(ownerLocationRefListener);
        bookingHasEndedRef.removeEventListener(bookingHasEndedRefListener);

        if (ownerFoundID != null){
            DatabaseReference ownerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Owners").child(ownerFoundID).child("customerRequest");
            ownerRef.removeValue();
            OwnerFoundID = null;

        }
        ownerFound = false;
        radius = 1;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

        /*if(pickupMarker != null){
            pickupMarker.remove();
        }*/
        if (mOwnerMarker != null){
            mOwnerMarker.remove();
        }
        mRequest.setText("Book Space");

        mOwnerInfo.setVisibility(View.GONE);
        mOwnerName.setText("");
        mOwnerPhone.setText("");
        mOwnerSpaceAddress.setText("Destination: --");
        mOwnerProfileImage.setImageResource(R.mipmap.ic_default_user);
    }

    /*-------------------------------------------- Map specific functions -----
    |  Function(s) onMapReady, buildGoogleApiClient, onLocationChanged, onConnected
    |
    |  Purpose:  Find and update user's location.
    |
    |  Note:
    |	   The update interval is set to 1000Ms and the accuracy is set to PRIORITY_HIGH_ACCURACY,
    |      If you're having trouble with battery draining too fast then change these to lower values
    |
    |
    *-------------------------------------------------------------------*/
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            }else{
                checkLocationPermission();
            }
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location : locationResult.getLocations()){
                if(getApplicationContext()!=null){
                    mLastLocation = location;

                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());

                    //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    //mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                    if(!getOwnersAroundStarted)
                        getOwnersAround();
                }
            }
        }
    };

    /*-------------------------------------------- onRequestPermissionsResult -----
    |  Function onRequestPermissionsResult
    |
    |  Purpose:  Get permissions for our app if they didn't previously exist.
    |
    |  Note:
    |	requestCode: the nubmer assigned to the request that we've made. Each
    |                request has it's own unique request code.
    |
    *-------------------------------------------------------------------*/
    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 1:{
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else{
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }




    boolean getOwnersAroundStarted = false;
    List<Marker> markers = new ArrayList<Marker>();
    private void getOwnersAround(){
        getDriversAroundStarted = true;
        DatabaseReference ownerLocation = FirebaseDatabase.getInstance().getReference().child("ownersAvailable");

        GeoFire geoFire = new GeoFire(ownerLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLongitude(), mLastLocation.getLatitude()), 999999999);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key))
                        return;
                }

                LatLng driverLocation = new LatLng(location.latitude, location.longitude);

                Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).title(key).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
                mDriverMarker.setTag(key);

                markers.add(mDriverMarker);


            }

            @Override
            public void onKeyExited(String key) {
                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key)){
                        markerIt.remove();
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key)){
                        markerIt.setPosition(new LatLng(location.Latitude, location.longitude));
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }



    public void setSupportFragmentManager(FragmentManager supportFragmentManager) {
        this.supportFragmentManager = supportFragmentManager;
    }
}
