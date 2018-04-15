package com.washinson.yaradio2;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    static final String TAG = "Stations";
    SharedPreferences sharedPreferences;

    static ArrayList<Station> stations = new ArrayList<>();
    static public Integer[] targets = {
            R.id.type_users,
            R.id.type_genres,
            R.id.type_moods,
            R.id.type_activities,
            R.id.type_epoches,
            R.id.type_authors,
            R.id.type_recommendations
    };
    static public ArrayList<Station.Subtype> recommend = new ArrayList<>();
    int curChoice = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Manager.browser = new WebView(this).getSettings().getUserAgentString();
        Manager.init(this);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.colorPrimary));

        sharedPreferences = getSharedPreferences(TAG, Context.MODE_PRIVATE);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();
        fragmentTransaction.replace(R.id.list_targets,
                ListFragment.instantiate(this, ListFragment.class.getName()), "loading").commit();

        try {
            createGUI();
            generateStations();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void generateStations() throws Exception {
        MainActivity.stations.clear();

        if(sharedPreferences.getString("library.jsx", "").equals("")){
            updateStations();
        }

        String[] targets = {
                "user", "genre", "mood", "activity", "epoch", "author"
        };

        String response = sharedPreferences.getString("library.jsx", "");

        JSONObject object = new JSONObject(response);

        JSONObject types = object.getJSONObject("types");
        JSONObject stations = object.getJSONObject("stations");

        int i = 0;
        for(String target : targets){
            if(types.getJSONObject(target).has("children")) {
                MainActivity.stations.add(Utils.makeStation(target,
                        types.getJSONObject(target).getJSONArray("children"),
                        stations));
            }
            else
                MainActivity.stations.add(Utils.makeStation(target, null, stations));
            i++;
        }

        loadType(curChoice);
    }

    boolean hasInternetConnection(){
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork == null)
            return false;
        return activeNetwork.isConnectedOrConnecting();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Browser.getLogin() != null){
            NavigationView navigationView = findViewById(R.id.nav_view);
            TextView textView = navigationView.getHeaderView(0).findViewById(R.id.fakeTextView);
            textView.setText(Browser.getLogin());
            TextView textView2 = navigationView.getHeaderView(0).findViewById(R.id.textView3);
            textView2.setText("");
        }
        try {
            if(hasInternetConnection())
                generateStations();
            else {
                Snackbar.make(findViewById(android.R.id.content), R.string.no_internet, Snackbar.LENGTH_SHORT).show();
                //Toast.makeText(this, "No internet", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadType(int i) {
        curChoice = i;

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(targets[i]);

        TargetFragment fragment = new TargetFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("StationID", i);
        fragment.setArguments(bundle);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        if(fragmentManager.findFragmentByTag("setting") != null){
            fragmentManager.popBackStack();
        }

        fragmentTransaction.replace(R.id.list_targets, fragment);
        fragmentTransaction.commit();
    }

    private void updateStations() throws Exception {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String response = Manager.getInstance().get("https://radio.yandex.ru/handlers/library.jsx?lang=ru",
                null, null);
        
        editor.putString("library.jsx", response);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //Toast.makeText(this, "Ha-ha! Fake button!", Toast.LENGTH_SHORT).show();
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager
                    .beginTransaction();
            if(fragmentManager.findFragmentByTag("setting") == null)
                fragmentTransaction.replace(R.id.list_targets, new SettingFragment(), "setting")
                        .addToBackStack("setting").commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        for(int i = 0; i < targets.length; i++){
            if(id == targets[i]) {
                if(id == R.id.type_users && Browser.getLogin() == null){
                    Intent intent = new Intent(this, Browser.class);
                    startActivity(intent);
                }
                loadType(i);
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    public void login(View view) {
        if(Browser.getLogin() != null){
            Toast.makeText(this, "Ha-ha! Fake button!", Toast.LENGTH_SHORT).show();
        }else {
            Intent intent = new Intent(this, Browser.class);
            startActivity(intent);
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        }
    }
}
