package com.koenidv.gmbplanner.ui.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.koenidv.gmbplanner.MainActivity;
import com.koenidv.gmbplanner.R;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import static android.content.Context.MODE_PRIVATE;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChangesFragment extends Fragment {

    public ChangesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_changes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences prefs = Objects.requireNonNull(getActivity()).getSharedPreferences("sharedPrefs", MODE_PRIVATE);

        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.changesToggleGroup);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getActivity(), Objects.requireNonNull(getActivity()).getSupportFragmentManager());
        ViewPager viewPager = view.findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        // Disable SwipeRefreshLayout while swiping
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float v, int i1) {
            }

            @Override
            public void onPageSelected(int position) {
                toggleGroup.check(position == 0 ? R.id.mineButton : R.id.allButton);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (MainActivity.swiperefresh != null)
                    MainActivity.swiperefresh.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
            }
        });

        // Switch to all changes if no courses are specified
        if (prefs.getString("myCourses", "").isEmpty())
            viewPager.setCurrentItem(1);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked)
                viewPager.setCurrentItem(checkedId == R.id.mineButton ? 0 : 1);
        });
    }
}
