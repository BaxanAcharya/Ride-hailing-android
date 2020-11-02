package com.example.rideshare.customer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.rideshare.MainActivity;
import com.example.rideshare.R;
import com.example.rideshare.driver.DriverMapActivity;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private Location lastlocation;
    private LocationRequest locationRequest;
    private Button dlogout, request, settings;
    private FirebaseAuth firebaseAuth;
    private LatLng pickupLocation;
    private Boolean requestStatus= false;
    private Marker pickupMarker;
    private String destination= "Mainali Tol";
    private LinearLayout driverInfo;
    private ImageView driverImage;
    private TextView driverName, driverPhone, driverCar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        dlogout = findViewById(R.id.clogout);
        request = findViewById(R.id.request);
        settings=findViewById(R.id.settings);
        driverInfo=findViewById(R.id.driverInfo);
        driverImage=findViewById(R.id.driverProfileImage);
        driverName=findViewById(R.id.driverName);
        driverPhone=findViewById(R.id.driverPhone);
        driverCar=findViewById(R.id.driverCar);



        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestStatus){
                    requestStatus=false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if (driverFoundId !=null){
                        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users")
                                .child("Riders").child(driverFoundId).child("customerRequests");
                        driverRef.removeValue();
                        driverFoundId=null;
                    }
                    driverFound=false;
                    radius=1;

                    String uid = firebaseAuth.getInstance().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequests");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.removeLocation(uid);
                    if (pickupMarker!=null){
                        pickupMarker.remove();
                    }
                    if (driverMarker!=null){
                        driverMarker.remove();
                    }
                    driverInfo.setVisibility(View.GONE);
                    driverName.setText("");
                    driverPhone.setText("");
                    driverCar.setText("");
                    driverImage.setImageResource(R.mipmap.ic_guest_user_foreground);
                    request.setText("Call Ride");
                }else {
                    requestStatus=true;
                    String uid = firebaseAuth.getInstance().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequests");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(uid, new GeoLocation(lastlocation.getLatitude(), lastlocation.getLongitude()));
                    pickupLocation = new LatLng(lastlocation.getLatitude(), lastlocation.getLongitude());
                   pickupMarker= mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup_marker_foreground)));
                    request.setText("Getting your driver...........");

                    getClosestDrivers();
                }

            }
        });
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        dlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });


        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(CustomerMapActivity.this, CustomerSettingActivity.class);
                startActivity(intent);
                return;
            }
        });

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyDfrP_Yz343-k0M7HxZgaI6AgvwyjWigEc", Locale.US);
        }
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));


        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NotNull Place place) {
                Toast.makeText(CustomerMapActivity.this, ""+place.getName(), Toast.LENGTH_SHORT).show();
               destination=place.getName();
            }


            @Override
            public void onError(@NotNull Status status) {
                // TODO: Handle the error.
                Toast.makeText(CustomerMapActivity.this, ""+status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundId;
    private GeoQuery geoQuery;

    private void getClosestDrivers() {
        DatabaseReference driversLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driversLocation);
         geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestStatus) {
                    driverFound = true;
                    driverFoundId = key;

                    //child will be added in rider collection
                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users")
                            .child("Riders").child(driverFoundId).child("customerRequests");
                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerRideId", customerId);
                    map.put("destination", destination);
                    driverRef.updateChildren(map);
                    //------------------- after this get assigned customer function will be triggered because we have attached a listner
                    getDriverLocation();
                    getDriverInfo();
                    request.setText("Looking for driver location..........");

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //Toast.makeText(CustomerMapActivity.this, "Please wait we are looking for drivers", Toast.LENGTH_SHORT).show();

                if (!driverFound) {
                    radius++;
                    getClosestDrivers();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriverInfo() {
        driverInfo.setVisibility(View.VISIBLE);
        DatabaseReference databaseReferenceCustomer = FirebaseDatabase.getInstance()
                .getReference().child("Users").child("Riders").child(driverFoundId);
        databaseReferenceCustomer.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map.get("name") != null) {

                        driverName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {

                        driverPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("car") != null) {

                        driverCar.setText(map.get("car").toString());
                    }
                    if (map.get("profileImageUrl") != null) {
                        Glide.with(getApplication())
                                .load(map.get("profileImageUrl")
                                        .toString()).into(driverImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerMapActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private  Marker driverMarker;
    private  DatabaseReference driverLocationRef;
private ValueEventListener driverLocationRefListener;
    private void getDriverLocation() {

        Toast.makeText(this, "Getting driver location", Toast.LENGTH_SHORT).show();

         driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking")
                .child(driverFoundId).child("l");
        driverLocationRefListener= driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && requestStatus) {
                    List<Object> map = (List<Object>) snapshot.getValue();
                    double locationlat = 0;
                    double locationlng = 0;
                    request.setText("Driver found........");
                    if (map.get(0) != null) {
                        locationlat = Double.parseDouble(map.get(0).toString());
                    }

                    if (map.get(1) != null) {
                        locationlng = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng driverlatlng = new LatLng(locationlat, locationlng);
                    if (driverMarker != null) {
                        driverMarker.remove();
                    }

                    Location location1=new Location("");
                    location1.setLatitude(pickupLocation.latitude);
                    location1.setLongitude(pickupLocation.longitude);

                    Location location2=new Location("");
                    location2.setLatitude(driverlatlng.latitude);
                    location2.setLongitude(driverlatlng.longitude);

                    float distance=location1.distanceTo(location2);
                    if (distance<100){
                        request.setText("Driver 's  Here ");
                        Toast.makeText(CustomerMapActivity.this, "Driver arrived", Toast.LENGTH_SHORT).show();
                    }else{
                        request.setText("Driver Found: " + String.valueOf(distance));
                    }


                    driverMarker = mMap.addMarker(new MarkerOptions().position(driverlatlng).title("Your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car_marker_foreground)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerMapActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
//        if (ContextCompat.checkSelfPermission(CustomerMapActivity.this,
//                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
//            if (ActivityCompat.shouldShowRequestPermissionRationale(CustomerMapActivity.this,
//                    Manifest.permission.ACCESS_FINE_LOCATION)){
//                ActivityCompat.requestPermissions(CustomerMapActivity.this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//                try {
//                    TimeUnit.SECONDS.sleep(10);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                mMap = googleMap;

                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
//            }else{
//                ActivityCompat.requestPermissions(CustomerMapActivity.this,
//                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//            }
//        }


    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        lastlocation = location;
//        System.out.println("i am updating");
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                                           int[] grantResults){
//        switch (requestCode){
//            case 1: {
//                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                    if (ContextCompat.checkSelfPermission(CustomerMapActivity.this,
//                            Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
//                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
//                    }
//                }else{
//                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
//                }
//                return;
//            }
//        }
//    }
}
