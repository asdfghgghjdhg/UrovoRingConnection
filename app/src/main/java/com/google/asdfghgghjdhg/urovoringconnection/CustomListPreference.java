package com.google.asdfghgghjdhg.urovoringconnection;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

public class CustomListPreference extends ListPreference {
    public CustomListPreference(Context context) {
        super(context);

        setWidgetLayoutResource(R.layout.custom_list_preference);
    }

    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setWidgetLayoutResource(R.layout.custom_list_preference);
    }

    public CustomListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWidgetLayoutResource(R.layout.custom_list_preference);
    }

    public CustomListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setWidgetLayoutResource(R.layout.custom_list_preference);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView title = (TextView)holder.findViewById(R.id.selectedTitle);
        TextView value = (TextView)holder.findViewById(R.id.selectedValue);
        if (getEntry() == null) {
            if (title != null) { title.setVisibility(View.INVISIBLE); }
            if (value != null) { value.setVisibility(View.INVISIBLE); }
        } else {
            if (title != null) {
                title.setVisibility(View.VISIBLE);
                title.setText(getEntry());
            }
            if (value != null) {
                value.setVisibility(View.VISIBLE);
                value.setText(getValue());
            }
        }
    }
}
