/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.network;

import static android.provider.Settings.System.NETWORK_TRAFFIC_STATE_SB;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.android.internal.util.custom.Utils;
import com.android.settings.core.TogglePreferenceController;

public class NetworkMonitorSbPrefController extends
        TogglePreferenceController implements Preference.OnPreferenceChangeListener {

    @VisibleForTesting
    static final String KEY_NETWORK_MONITOR_SB = "network_traffic_state_sb";
    private final int ON = 1;
    private final int OFF = 0;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public NetworkMonitorSbPrefController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return !Utils.hasNotch(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_NETWORK_MONITOR_SB.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, SettingsEnums.MOBILE_NETWORK);
        }
        return false;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                NETWORK_TRAFFIC_STATE_SB, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.System.putInt(mContext.getContentResolver(),
                NETWORK_TRAFFIC_STATE_SB, isChecked ? ON : OFF);
        return true;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }
}
