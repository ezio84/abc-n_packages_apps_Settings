package com.android.settings.display;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.security.OwnerInfoPreferenceController.OwnerInfoCallback;

public class DummyFragment extends InstrumentedDialogFragment implements OnClickListener {

    private static final String TAG_DUMMY = "dummy_fragment";

    private View mView;
    private int mUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = UserHandle.myUserId();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mView = LayoutInflater.from(getActivity()).inflate(R.layout.dummy_file, null);
        initView();
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.the_dummy_guys)
                .setView(mView)
                .setPositiveButton(R.string.save, this)
                .setNegativeButton(R.string.cancel, this)
                .show();
    }

    private void initView() {
        //
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            //
        }
    }

    public static void show(Fragment parent) {
        if (!parent.isAdded()) return;

        final DummyFragment dialog = new DummyFragment();
        dialog.setTargetFragment(parent, 0);
        dialog.show(parent.getFragmentManager(), TAG_DUMMY);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ABC;
    }
}
