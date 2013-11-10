/*
 * Performance Control - An Android CPU Control application Copyright (C) 2012
 * James Roberts
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.brewcrewfoo.performance.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.GovSetActivity;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

public class GPUSettings extends Fragment implements SeekBar.OnSeekBarChangeListener, Constants {

    private SeekBar mMaxSlider;
    private TextView mMaxSpeedText;
    private String[] mAvailableFrequencies;
    private String mMaxFreqSetting;
    SharedPreferences mPreferences;
    private Context context;
    private CpuInfoListAdapter mCpuInfoListAdapter;
    private ListView mCpuInfoList;
    private List<String> mCpuInfoListData;
    private LayoutInflater mInflater;
    private static final int NEW_MENU_ID=Menu.FIRST+1;

    public class CpuInfoListAdapter extends ArrayAdapter<String> {

        public CpuInfoListAdapter(Context context, int resource, List<String> values) {
            super(context, R.layout.gpu_info_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = mInflater.inflate(R.layout.gpu_info_item, parent, false);
            TextView cpuInfoFreq = (TextView)rowView.findViewById(R.id.gpu_info_freq);
            cpuInfoFreq.setText(mCpuInfoListData.get(position));
            return rowView;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=getActivity();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        mInflater = inflater;
        View view = mInflater.inflate(R.layout.gpu_settings, root, false);

        mAvailableFrequencies = new String[0];

        String availableFrequenciesLine = Helpers.readOneLine(STEPS_GPU_PATH);
        if (availableFrequenciesLine != null) {
            mAvailableFrequencies = availableFrequenciesLine.split(" ");
            Arrays.sort(mAvailableFrequencies, new Comparator<String>() {
                @Override
                public int compare(String object1, String object2) {
                    return Integer.valueOf(object1).compareTo(Integer.valueOf(object2));
                }
            });
        }

        int mFrequenciesNum = mAvailableFrequencies.length - 1;

        String mCurMaxSpeed;

        mCurMaxSpeed = Helpers.readOneLine(MAX_GPU_FREQ_PATH);

        mMaxSlider = (SeekBar) view.findViewById(R.id.max_gpu_slider);
        mMaxSlider.setMax(mFrequenciesNum);
        mMaxSpeedText = (TextView) view.findViewById(R.id.max_gpu_speed_text);
        mMaxSpeedText.setText(Helpers.toGpuMHz(mCurMaxSpeed));
        mMaxSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(mCurMaxSpeed));
        mMaxFreqSetting=mCurMaxSpeed;
        mMaxSlider.setOnSeekBarChangeListener(this);

        Switch mSetOnBoot = (Switch) view.findViewById(R.id.gpu_sob);
        mSetOnBoot.setChecked(mPreferences.getBoolean(GPU_SOB, false));
        mSetOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(GPU_SOB, checked);
                if (checked) {
                    editor.putString(PREF_MAX_GPU, Helpers.readOneLine(MAX_GPU_FREQ_PATH));
                }
                editor.commit();
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.gpu_settings_menu, menu);
        Helpers.addItems2Menu(menu,NEW_MENU_ID,getString(R.string.menu_tab),(ViewPager) getView().getParent());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Helpers.removeCurItem(item,NEW_MENU_ID,(ViewPager) getView().getParent());
        switch(item.getItemId()){
            case R.id.app_settings:
                Intent intent = new Intent(context, PCSettings.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar.getId() == R.id.max_gpu_slider) {
                setMaxSpeed(seekBar, progress);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // we have a break now, write the values..
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
            sb.append("busybox echo ").append(mMaxFreqSetting).append(" > ").append(MAX_GPU_FREQ_PATH).append(";\n");
        Helpers.shExec(sb,context,true);
        }
    }

    public void setMaxSpeed(SeekBar seekBar, int progress) {
        String current = "";
        current = mAvailableFrequencies[progress];
        mMaxSpeedText.setText(Helpers.toGpuMHz(current));
        mMaxFreqSetting = current;
        updateSharedPrefs(PREF_MAX_GPU, current);
    }


    private void updateSharedPrefs(String var, String value) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(var, value).commit();
    }
}

