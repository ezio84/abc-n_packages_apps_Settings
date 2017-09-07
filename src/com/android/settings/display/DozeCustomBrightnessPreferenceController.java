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
package com.android.settings.display;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import com.abc.settings.preferences.CustomSeekBarPreference;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

import static android.provider.Settings.Secure.DOZE_ENABLED;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_AMBIENT_DISPLAY;

public class DozeCustomBrightnessPreferenceController extends PreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_AMBIENT_DOZE_CUSTOM_BRIGHTNESS = "ambient_doze_custom_brightness";

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public DozeCustomBrightnessPreferenceController(Context context) {
        super(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AMBIENT_DOZE_CUSTOM_BRIGHTNESS;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_AMBIENT_DOZE_CUSTOM_BRIGHTNESS.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, ACTION_AMBIENT_DISPLAY);
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        CustomSeekBarPreference mAmbientDozeCustomBrightness =
                (CustomSeekBarPreference) preference;
        int defaultValue = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_screenBrightnessDoze);
        int brightness = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.AMBIENT_DOZE_CUSTOM_BRIGHTNESS, defaultValue,
                UserHandle.USER_CURRENT);
        mAmbientDozeCustomBrightness.setMin(defaultValue);
        mAmbientDozeCustomBrightness.setValue(brightness);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int brightness = (Integer) newValue;
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.AMBIENT_DOZE_CUSTOM_BRIGHTNESS, brightness,
                UserHandle.USER_CURRENT);
        return true;
    }

    @Override
    public boolean isAvailable() {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = mContext.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }
}
