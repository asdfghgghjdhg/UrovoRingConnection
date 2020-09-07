package com.google.asdfghgghjdhg.urovoringconnection;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class ServiceManagementPreference extends Preference implements View.OnClickListener {
    private View.OnClickListener startServiceClickListener = null;
    private View.OnClickListener stopServiceClickListener = null;

    public ServiceManagementPreference(Context context) {
        super(context);

        setWidgetLayoutResource(R.layout.service_management_preference);
    }

    public ServiceManagementPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setWidgetLayoutResource(R.layout.service_management_preference);
    }

    public ServiceManagementPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWidgetLayoutResource(R.layout.service_management_preference);
    }

    public ServiceManagementPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setWidgetLayoutResource(R.layout.service_management_preference);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        Button btn = (Button)holder.findViewById(R.id.buttonStartService);
        btn.setOnClickListener(this);
        btn = (Button)holder.findViewById(R.id.buttonStopService);
        btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonStartService) {
            if (startServiceClickListener != null) {
                startServiceClickListener.onClick(v);
            }
        }
        if (v.getId() == R.id.buttonStopService) {
            if (stopServiceClickListener != null) {
                stopServiceClickListener.onClick(v);
            }
        }
    }

    public void setOnStartServiceClick(View.OnClickListener listener) {
        startServiceClickListener = listener;
    }
    public void setOnStopServiceClick(View.OnClickListener listener) {
        stopServiceClickListener = listener;
    }
}
