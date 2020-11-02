package com.example.rideshare.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.rideshare.MainActivity;
import com.example.rideshare.R;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.api.GoogleApiClient;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;


public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private Location lastlocation;
    private LocationRequest locationRequest;
    private Button dlogout, settings;
    private FirebaseAuth firebaseAuth;
    private String customerID = "";
    private String uid = "";
    private boolean isLoggingOut = false;
    private LinearLayout customerInfo;
    private ImageView customerImage;
    private TextView customerName, customerPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        dlogout = findViewById(R.id.dlogout);

        customerInfo = findViewById(R.id.customerInfo);
        customerImage = findViewById(R.id.customerProfileImage);
        customerName = findViewById(R.id.customerName);
        customerPhone = findViewById(R.id.customerPhone);
        settings = findViewById(R.id.rSettings);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this, DriverSettingActivity.class);
                startActivity(intent);
                return;
            }
        });

        dlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent i = new Intent(DriverMapActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        });

        //if there is a customer request or suddenly if a customer id exist in rider collection this function will be triggred
        getAssignedCustomer();

    }

    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Riders").child(driverId).child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    customerID = snapshot.getValue().toString();
                    //here we will get the pick up location of the rider
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerInfo();
                } else {
                    customerID = "";
                    if (pickupMarker != null) {
                        pickupMarker.remove();
                    }
                    if (assignedCustomerValueEventListener != null) {
                        getAssignedCustomerPickupLocationRef.removeEventListener(assignedCustomerValueEventListener);
                    }
                    customerInfo.setVisibility(View.GONE);
                    customerName.setText("");
                    customerPhone.setText("");
                    customerImage.setImageResource(R.mipmap.ic_guest_user_foreground);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverMapActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void getAssignedCustomerInfo() {
        customerInfo.setVisibility(View.VISIBLE);
        DatabaseReference databaseReferenceCustomer = FirebaseDatabase.getInstance()
                .getReference().child("Users").child("Customers").child(customerID);
        databaseReferenceCustomer.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map.get("name") != null) {

                        customerName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {

                        customerPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("profileImageUrl") != null) {
                        Glide.with(getApplication())
                                .load(map.get("profileImageUrl")
                                        .toString()).into(customerImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverMapActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Marker pickupMarker;
    private DatabaseReference getAssignedCustomerPickupLocationRef;
    private ValueEventListener assignedCustomerValueEventListener;

    private void getAssignedCustomerPickupLocation() {
        getAssignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequests")
                .child(customerID).child("l");
        assignedCustomerValueEventListener = getAssignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && !customerID.equals("")) {
                    List<Object> map = (List<Object>) snapshot.getValue();
                    double locationlat = 0;
                    double locationlng = 0;

                    if (map.get(0) != null) {
                        locationlat = Double.parseDouble(map.get(0).toString());
                    }

                    if (map.get(1) != null) {
                        locationlng = Double.parseDouble(map.get(1).toString());
                    }

                    LatLng driverlatlng = new LatLng(locationlat, locationlng);

                    pickupMarker = mMap.addMarker(new MarkerOptions().position(driverlatlng).title("customer pickup location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup_marker_foreground)));
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverMapActivity.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);

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
        if (getApplicationContext() != null) {
            lastlocation = location;
            //        System.out.println("i am updating");
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");

            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);
            switch (customerID) {
                case "":
                    geoFireWorking.removeLocation(uid);
                    geoFireAvailable.setLocation(uid, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailable.removeLocation(uid);
                    geoFireWorking.setLocation(uid, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }


        }

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

    private void disconnectDriver() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(uid);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLoggingOut) {
            disconnectDriver();

        }
    }
}
