/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures;

import static android.provider.Settings.Secure.ASSIST_GESTURE_SENSITIVITY;
import static android.provider.Settings.Secure.ASSIST_GESTURE_WAKE_ENABLED;
import static android.provider.Settings.Secure.SHORT_SQUEEZE_SELECTION;
import static android.provider.Settings.Secure.SHORT_SQUEEZE_CUSTOM_APP_FR_NAME;
import static android.provider.Settings.Secure.LONG_SQUEEZE_SELECTION;
import static android.provider.Settings.Secure.LONG_SQUEEZE_CUSTOM_APP_FR_NAME;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

import com.abc.support.preferences.CustomSeekBarPreference;

public class AssistGestureSettingsPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause, OnCreate, OnSaveInstanceState {

    private static final String PREF_KEY_VIDEO = "gesture_assist_video";
    static final String KEY_VIDEO_PAUSED = "key_video_paused";

    private static final int ON = 1;
    private static final int OFF = 0;

    private final AssistGestureFeatureProvider mFeatureProvider;

    private VideoPreference mVideoPreference;
    boolean mVideoPaused;

    private CustomSeekBarPreference mActiveEdgeSensitivity;
    private SwitchPreference mActiveEdgeWake;
    private ListPreference mActiveEdgeShortSqueezeActions;
    private Preference mActiveEdgeShortSqueezeAppSelection;
    private ListPreference mActiveEdgeLongSqueezeActions;
    private Preference mActiveEdgeLongSqueezeAppSelection;

    public AssistGestureSettingsPreferenceController(Context context,
            String key) {
        super(context, key);
        mFeatureProvider = FeatureFactory.getFactory(context).getAssistGestureFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return mFeatureProvider.isSensorAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mVideoPreference = (VideoPreference) screen.findPreference(PREF_KEY_VIDEO);

            mActiveEdgeSensitivity = (CustomSeekBarPreference) screen.findPreference("gesture_assist_sensitivity");
            mActiveEdgeWake = (SwitchPreference) screen.findPreference("gesture_assist_wake");
            mActiveEdgeShortSqueezeActions = (ListPreference) screen.findPreference("short_squeeze_selection");
            mActiveEdgeShortSqueezeAppSelection = (Preference) screen.findPreference("short_squeeze_app_selection");
            mActiveEdgeLongSqueezeActions = (ListPreference) screen.findPreference("long_squeeze_selection");
            mActiveEdgeLongSqueezeAppSelection = (Preference) screen.findPreference("long_squeeze_app_selection");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mVideoPaused = savedInstanceState.getBoolean(KEY_VIDEO_PAUSED, false);
        }
    }

     @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_VIDEO_PAUSED, mVideoPaused);
    }

    @Override
    public void onPause() {
        if (mVideoPreference != null) {
            mVideoPaused = mVideoPreference.isVideoPaused();
            mVideoPreference.onViewInvisible();
        }
        actionPreferenceReload();
        customAppCheck();
    }

    @Override
    public void onResume() {
        if (mVideoPreference != null) {
            mVideoPreference.onViewVisible(mVideoPaused);
        }
        actionPreferenceReload();
        customAppCheck();
    }

    /* Helper for reloading both short and long gesture as they might change on
       package uninstallation */
    private void actionPreferenceReload() {
        int shortSqueezeActions = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SHORT_SQUEEZE_SELECTION, 0,
                UserHandle.USER_CURRENT);
        int longSqueezeActions = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.LONG_SQUEEZE_SELECTION, 0,
                UserHandle.USER_CURRENT);

        // Reload the action preferences
        if (mActiveEdgeShortSqueezeActions != null) {
            mActiveEdgeShortSqueezeActions.setValue(Integer.toString(shortSqueezeActions));
            mActiveEdgeShortSqueezeActions.setSummary(mActiveEdgeShortSqueezeActions.getEntry());
        }
        if (mActiveEdgeLongSqueezeActions != null) {
            mActiveEdgeLongSqueezeActions.setValue(Integer.toString(longSqueezeActions));
            mActiveEdgeLongSqueezeActions.setSummary(mActiveEdgeLongSqueezeActions.getEntry());
        }
        // Also ensure that the application chooser gets disabled when needed
        if (mActiveEdgeShortSqueezeAppSelection != null && mActiveEdgeShortSqueezeActions != null) {
            mActiveEdgeShortSqueezeAppSelection.setEnabled(mActiveEdgeShortSqueezeActions.getEntryValues()
            [shortSqueezeActions].equals("11"));
        }
        if (mActiveEdgeLongSqueezeAppSelection != null && mActiveEdgeLongSqueezeActions != null) {
            mActiveEdgeLongSqueezeAppSelection.setEnabled(mActiveEdgeLongSqueezeActions.getEntryValues()
            [longSqueezeActions].equals("11"));
        }
    }

    private boolean isAssistGestureEnabled() {
        return (Settings.Secure.getIntForUser(mContext.getContentResolver(),
                SHORT_SQUEEZE_SELECTION, OFF, UserHandle.USER_CURRENT) != 0)
                || (Settings.Secure.getIntForUser(mContext.getContentResolver(),
                LONG_SQUEEZE_SELECTION, OFF, UserHandle.USER_CURRENT) != 0);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (preference == null) return;

        if (TextUtils.equals(preference.getKey(), "gesture_assist_wake")) {
            SwitchPreference pref = (SwitchPreference) preference;
            int value = Settings.Secure.getIntForUser(
                    mContext.getContentResolver(), ASSIST_GESTURE_WAKE_ENABLED, ON,
                    UserHandle.USER_CURRENT);
            pref.setChecked(value == ON);
        } else if (TextUtils.equals(preference.getKey(), "gesture_assist_sensitivity")) {
            CustomSeekBarPreference pref = (CustomSeekBarPreference) preference;
            int value = Settings.Secure.getIntForUser(
                    mContext.getContentResolver(), ASSIST_GESTURE_SENSITIVITY, 2,
                    UserHandle.USER_CURRENT);
            pref.setValue(value);
        } else if (TextUtils.equals(preference.getKey(), "short_squeeze_selection")) {
            ListPreference pref = (ListPreference) preference;
            int value = Settings.Secure.getIntForUser(
                    mContext.getContentResolver(), SHORT_SQUEEZE_SELECTION, OFF,
                    UserHandle.USER_CURRENT);
            pref.setValue(String.valueOf(value));
            pref.setSummary(pref.getEntry());
        } else if (TextUtils.equals(preference.getKey(), "short_squeeze_app_selection")) {
            Preference pref = (Preference) preference;
            boolean isAppSelection = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                    SHORT_SQUEEZE_SELECTION, OFF, UserHandle.USER_CURRENT) == 11/*action_app_action*/;
            pref.setEnabled(isAppSelection);
            customAppCheck();
        } else if (TextUtils.equals(preference.getKey(), "long_squeeze_selection")) {
            ListPreference pref = (ListPreference) preference;
            int value = Settings.Secure.getIntForUser(
                    mContext.getContentResolver(), LONG_SQUEEZE_SELECTION, OFF,
                    UserHandle.USER_CURRENT);
            pref.setValue(String.valueOf(value));
            pref.setSummary(pref.getEntry());
        } else if (TextUtils.equals(preference.getKey(), "long_squeeze_app_selection")) {
            Preference pref = (Preference) preference;
            boolean isAppSelection = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                    LONG_SQUEEZE_SELECTION, OFF, UserHandle.USER_CURRENT) == 11/*action_app_action*/;
            pref.setEnabled(isAppSelection);
            customAppCheck();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (TextUtils.equals(preference.getKey(), "short_squeeze_selection")) {
            int value = Integer.parseInt((String) newValue);
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    SHORT_SQUEEZE_SELECTION, value, UserHandle.USER_CURRENT);
            int index = mActiveEdgeShortSqueezeActions.findIndexOfValue((String) newValue);
            mActiveEdgeShortSqueezeActions.setSummary(
                    mActiveEdgeShortSqueezeActions.getEntries()[index]);
            if (mActiveEdgeShortSqueezeAppSelection != null) {
                mActiveEdgeShortSqueezeAppSelection.setEnabled(value == 11);
            }
            customAppCheck();
            return true;
        } else if (TextUtils.equals(preference.getKey(), "long_squeeze_selection")) {
            int value = Integer.parseInt((String) newValue);
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    LONG_SQUEEZE_SELECTION, value, UserHandle.USER_CURRENT);
            int index = mActiveEdgeLongSqueezeActions.findIndexOfValue((String) newValue);
            mActiveEdgeLongSqueezeActions.setSummary(
                    mActiveEdgeLongSqueezeActions.getEntries()[index]);
            if (mActiveEdgeLongSqueezeAppSelection != null) {
                mActiveEdgeLongSqueezeAppSelection.setEnabled(value == 11);
            }
            customAppCheck();
            return true;
        } else if (TextUtils.equals(preference.getKey(), "gesture_assist_sensitivity")) {
            int val = (Integer) newValue;
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    ASSIST_GESTURE_SENSITIVITY, val, UserHandle.USER_CURRENT);
            return true;
        } else if (TextUtils.equals(preference.getKey(), "gesture_assist_wake")) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    ASSIST_GESTURE_WAKE_ENABLED,
                    enabled ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    private void customAppCheck() {
        if (mActiveEdgeShortSqueezeAppSelection != null) {
            mActiveEdgeShortSqueezeAppSelection.setSummary(Settings.Secure.getStringForUser(mContext.getContentResolver(),
                    String.valueOf(SHORT_SQUEEZE_CUSTOM_APP_FR_NAME), UserHandle.USER_CURRENT));
        }
        if (mActiveEdgeLongSqueezeAppSelection != null) {
            mActiveEdgeLongSqueezeAppSelection.setSummary(Settings.Secure.getStringForUser(mContext.getContentResolver(),
                    String.valueOf(LONG_SQUEEZE_CUSTOM_APP_FR_NAME), UserHandle.USER_CURRENT));
        }
    }
}
