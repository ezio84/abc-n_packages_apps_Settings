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
        customAppCheck(mActiveEdgeShortSqueezeAppSelection, mActiveEdgeLongSqueezeAppSelection);
    }

    @Override
    public void onResume() {
        if (mVideoPreference != null) {
            mVideoPreference.onViewVisible(mVideoPaused);
        }
        customAppCheck(mActiveEdgeShortSqueezeAppSelection, mActiveEdgeLongSqueezeAppSelection);
    }

    private boolean isAssistGestureEnabled() {
        return (Settings.Secure.getInt(mContext.getContentResolver(),
                SHORT_SQUEEZE_SELECTION, OFF) != 0)
                || (Settings.Secure.getInt(mContext.getContentResolver(),
                LONG_SQUEEZE_SELECTION, OFF) != 0);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (preference == null) return;

        if (TextUtils.equals(preference.getKey(), "gesture_assist_wake")) {
            SwitchPreference pref = (SwitchPreference) preference;
            int value = Settings.Secure.getInt(
                    mContext.getContentResolver(), ASSIST_GESTURE_WAKE_ENABLED, ON);
            pref.setChecked(value == ON);
        } else if (TextUtils.equals(preference.getKey(), "gesture_assist_sensitivity")) {
            CustomSeekBarPreference pref = (CustomSeekBarPreference) preference;
            int value = Settings.Secure.getInt(
                    mContext.getContentResolver(), ASSIST_GESTURE_SENSITIVITY, 2);
            pref.setValue(value);
        } else if (TextUtils.equals(preference.getKey(), "short_squeeze_selection")) {
            ListPreference pref = (ListPreference) preference;
            int value = Settings.Secure.getInt(
                    mContext.getContentResolver(), SHORT_SQUEEZE_SELECTION, OFF);
            pref.setValue(String.valueOf(value));
            pref.setSummary(pref.getEntry());
        } else if (TextUtils.equals(preference.getKey(), "short_squeeze_app_selection")) {
            Preference pref = (Preference) preference;
            boolean isAppSelection = Settings.Secure.getInt(mContext.getContentResolver(),
                    SHORT_SQUEEZE_SELECTION, OFF) == 11/*action_app_action*/;
            pref.setEnabled(isAppSelection);
            customAppCheck(pref, mActiveEdgeLongSqueezeAppSelection);
        } else if (TextUtils.equals(preference.getKey(), "long_squeeze_selection")) {
            ListPreference pref = (ListPreference) preference;
            int value = Settings.Secure.getInt(
                    mContext.getContentResolver(), LONG_SQUEEZE_SELECTION, OFF);
            pref.setValue(String.valueOf(value));
            pref.setSummary(pref.getEntry());
        } else if (TextUtils.equals(preference.getKey(), "long_squeeze_app_selection")) {
            Preference pref = (Preference) preference;
            boolean isAppSelection = Settings.Secure.getInt(mContext.getContentResolver(),
                    LONG_SQUEEZE_SELECTION, OFF) == 11/*action_app_action*/;
            pref.setEnabled(isAppSelection);
            customAppCheck(mActiveEdgeShortSqueezeAppSelection, pref);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (TextUtils.equals(preference.getKey(), "short_squeeze_selection")) {
            int value = Integer.parseInt((String) newValue);
            Settings.Secure.putInt(mContext.getContentResolver(),
                    SHORT_SQUEEZE_SELECTION, value);
            int index = mActiveEdgeShortSqueezeActions.findIndexOfValue((String) newValue);
            mActiveEdgeShortSqueezeActions.setSummary(
                    mActiveEdgeShortSqueezeActions.getEntries()[index]);
            if (mActiveEdgeShortSqueezeAppSelection != null) {
                mActiveEdgeShortSqueezeAppSelection.setEnabled(value == 11);
            }
            customAppCheck(mActiveEdgeShortSqueezeAppSelection, mActiveEdgeLongSqueezeAppSelection);
            return true;
        } else if (TextUtils.equals(preference.getKey(), "long_squeeze_selection")) {
            int value = Integer.parseInt((String) newValue);
            Settings.Secure.putInt(mContext.getContentResolver(),
                    LONG_SQUEEZE_SELECTION, value);
            int index = mActiveEdgeLongSqueezeActions.findIndexOfValue((String) newValue);
            mActiveEdgeLongSqueezeActions.setSummary(
                    mActiveEdgeLongSqueezeActions.getEntries()[index]);
            if (mActiveEdgeLongSqueezeAppSelection != null) {
                mActiveEdgeLongSqueezeAppSelection.setEnabled(value == 11);
            }
            customAppCheck(mActiveEdgeShortSqueezeAppSelection, mActiveEdgeLongSqueezeAppSelection);
            return true;
        } else if (TextUtils.equals(preference.getKey(), "gesture_assist_sensitivity")) {
            int val = (Integer) newValue;
            Settings.Secure.putInt(mContext.getContentResolver(),
                    ASSIST_GESTURE_SENSITIVITY, val);
            return true;
        } else if (TextUtils.equals(preference.getKey(), "gesture_assist_wake")) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.Secure.putInt(mContext.getContentResolver(),
                    ASSIST_GESTURE_WAKE_ENABLED,
                    enabled ? 1 : 0);
            return true;
        }
        return false;
    }

    private void customAppCheck(Preference appShortSqueezeSelection, Preference appLongSqueezeSelection) {
        if (appShortSqueezeSelection != null) {
            appShortSqueezeSelection.setSummary(Settings.Secure.getString(mContext.getContentResolver(),
                    String.valueOf(SHORT_SQUEEZE_CUSTOM_APP_FR_NAME)));
        }
        if (appLongSqueezeSelection != null) {
            appLongSqueezeSelection.setSummary(Settings.Secure.getString(mContext.getContentResolver(),
                    String.valueOf(LONG_SQUEEZE_CUSTOM_APP_FR_NAME)));
        }
    }
}
