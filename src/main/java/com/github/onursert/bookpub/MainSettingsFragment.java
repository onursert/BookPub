package com.github.onursert.bookpub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

public class MainSettingsFragment extends PreferenceFragmentCompat {

    public MainSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        setPreferencesFromResource(R.xml.preferences, s);

        Preference preferenceGithub = findPreference("github");
        preferenceGithub.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/onursert/bookpub"));
                startActivity(browserIntent);
                return true;
            }
        });

        Preference preferencePicasso = findPreference("picasso");
        preferencePicasso.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/square/picasso/blob/master/LICENSE.txt"));
                startActivity(browserIntent);
                return true;
            }
        });
    }
}
