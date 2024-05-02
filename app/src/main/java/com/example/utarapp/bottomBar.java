package com.example.utarapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class bottomBar extends BaseActivity{
    BottomNavigationView bottomNavigationView;
    private DrawerLayout drawerLayout;
    private NavigationView moreNavigationView;

    private static Fragment mCurrentFragment = null;

    public static class ViewPagerAdapter extends FragmentStateAdapter {
        private String userType;
        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, String userType) {
            super(fragmentActivity);
            this.userType = userType;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Determine the appropriate fragment for the specified position
            if (userType.equals("lecturer")) {
                switch (position) {
                    case 0:
                        return new HomePage();
                    case 1:
                        return new RealTimeNotification();
                    // Skip ScanAttendance for lecturers
                    default:
                        return null; // Fallback or consider another fragment
                }
            } else {
                // Original fragment loading logic for students
                switch (position) {
                    case 0:
                        return new HomePage();
                    case 1:
                        return new RealTimeNotification();
                    case 2:
                        return new ScanAttendance();
                    default:
                        return null;
                }
            }
        }


        @Override
        public int getItemCount() {
            return userType.equals("lecturer") ? 2 : 3; // You have three fragments, so return 3
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_bar);

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userType = sharedPreferences.getString("UserType", null);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        drawerLayout = findViewById(R.id.drawer_layout);
        moreNavigationView = findViewById(R.id.nav_view);
        Menu navMenu = moreNavigationView.getMenu();

        if ("lecturer".equals(userType)) {
            bottomNavigationView.getMenu().removeItem(R.id.attendance_page);
        }

// Conditionally show or hide menu items
        navMenu.findItem(R.id.nav_ninth_fragment).setVisible(userType.equals("lecturer"));
// Repeat for other items as necessary

        // Set the title of the "nav_forth_fragment" based on the user type
        MenuItem fourthFragment = navMenu.findItem(R.id.nav_forth_fragment);
        if (userType.equals("lecturer")) {
            fourthFragment.setTitle("Timetable Management");
        } else {
            fourthFragment.setTitle("Course Registration");
        }

        MenuItem sixthFragment = navMenu.findItem(R.id.nav_sixth_fragment);
        if (userType.equals("lecturer")) {
            sixthFragment.setTitle("Attendance Management");
        } else {
            sixthFragment.setTitle("Exam Result");
        }


        ViewPager2 viewPager = findViewById(R.id.viewPager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this, userType);
        viewPager.setAdapter(adapter);

        moreNavigationView.setNavigationItemSelectedListener(item -> {
            // Handle drawer menu item clicks here.
            int id = item.getItemId();
                if (id==R.id.nav_first_fragment) {
                    // Start another activity or perform some action
                    Intent intentMenu = new Intent(bottomBar.this, AssignmentTask.class);
                    startActivity(intentMenu);
                } else if (id==R.id.nav_second_fragment) {
                // Start another activity or perform some action
                Intent intentMenu = new Intent(bottomBar.this, reviewTimetable.class);
                startActivity(intentMenu);
            } else if (id==R.id.nav_third_fragment) {
                    // Start another activity or perform some action
                    Intent intentMenu = new Intent(bottomBar.this, UtarPortalWebActivity.class);
                    startActivity(intentMenu);
                }else if (id==R.id.nav_forth_fragment) {
                    if(userType.equals("student")) {
                        // Start another activity or perform some action
                        Intent intentMenu = new Intent(bottomBar.this, CourseRegistration.class);
                        startActivity(intentMenu);
                    }else if (userType.equals("lecturer")) {
                        Intent intentMenu = new Intent(bottomBar.this, TimetableManagementWebViewActivity.class);
                        startActivity(intentMenu);
                    }
                }else if (id==R.id.nav_fifth_fragment) {
                    // Start another activity or perform some action
                    Intent intentMenu = new Intent(bottomBar.this, UTARMapActivity.class);
                    startActivity(intentMenu);
                }else if (id==R.id.nav_sixth_fragment) {
                    if(userType.equals("student")) {
                        // Start another activity or perform some action
                        Intent intentMenu = new Intent(bottomBar.this, examResultWeb.class);
                        startActivity(intentMenu);
                    }else if (userType.equals("lecturer")) {
                        Intent intentMenu = new Intent(bottomBar.this, AttendanceManagementWebViewActivity.class);
                        startActivity(intentMenu);
                    }
                }else if (id==R.id.nav_seventh_fragment) {
                    // Start another activity or perform some action
                    Intent intentMenu = new Intent(bottomBar.this, EmailWebViewActivity.class);
                    startActivity(intentMenu);
                }else if (id==R.id.nav_eighth_fragment) {
                    // Start another activity or perform some action
                    Intent intentMenu = new Intent(bottomBar.this, UTAROfficialWebsite.class);
                    startActivity(intentMenu);
                }else if (id==R.id.nav_ninth_fragment) {
                    // Start another activity or perform some action
                    Intent intentMenu = new Intent(bottomBar.this, AssignmentManagementWebViewActivity.class);
                    startActivity(intentMenu);
                }

                // Handle other drawer items similarly
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Linking ViewPager2 with BottomNavigationView
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNavigationView.setSelectedItemId(R.id.home_page);
                        break;
                    case 1:
                        bottomNavigationView.setSelectedItemId(R.id.notification_page);
                        break;
                    case 2:
                        bottomNavigationView.setSelectedItemId(R.id.attendance_page);
                        break;
                    case 3:
                        bottomNavigationView.setSelectedItemId(R.id.more_page);
                        if (drawerLayout != null && !drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            drawerLayout.openDrawer(GravityCompat.START);
                        }
                        break;
                }
            }
        });


        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home_page) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (id == R.id.notification_page) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (id == R.id.attendance_page) {
                viewPager.setCurrentItem(2);
                return true;
            } else if (id == R.id.more_page){
                // Open the hamburger menu when the more_page is selected
                viewPager.setCurrentItem(3);
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            }else {
                return false;
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.home_page);

        //Log Out function
        Button btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear(); // This clears all data. Use remove("key") for specific values.
                editor.apply();
                // Here, you can clear user's session or any other logout operations.
                UserData.getInstance().clearSession();

                // Then, navigate back to the login screen or any other screen.
                Intent intent = new Intent(bottomBar.this, LoginPage.class);
                startActivity(intent);
                finish();

                // Close the navigation drawer
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        });
    }
    HomePage firstFragment = new HomePage();
    RealTimeNotification secondFragment = new RealTimeNotification();
    digitalStudentId thirdFragment = new digitalStudentId();

    //back press for hamburger menu
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}