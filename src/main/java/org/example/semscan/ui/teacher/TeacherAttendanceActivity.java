package org.example.semscan.ui.teacher;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.example.semscan.R;
import org.example.semscan.ui.fragments.AbsenceRequestsFragment;
import org.example.semscan.ui.fragments.PresentAttendanceFragment;

public class TeacherAttendanceActivity extends AppCompatActivity {
    
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_attendance);
        
        setupToolbar();
        setupViewPager();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupViewPager() {
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        
        AttendancePagerAdapter adapter = new AttendancePagerAdapter(this);
        viewPager.setAdapter(adapter);
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(getString(R.string.present_tab));
                    break;
                case 1:
                    tab.setText(getString(R.string.absence_requests_tab));
                    break;
            }
        }).attach();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    private static class AttendancePagerAdapter extends FragmentStateAdapter {
        
        public AttendancePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new PresentAttendanceFragment();
                case 1:
                    return new AbsenceRequestsFragment();
                default:
                    return new PresentAttendanceFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
