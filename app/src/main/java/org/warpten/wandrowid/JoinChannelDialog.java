package org.warpten.wandrowid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;

/**
 * Created by perquet on 29/07/14.
 */
public class JoinChannelDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_joinchannel, null))
                .setPositiveButton(R.string.button_join_channel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String channelName = ((EditText)JoinChannelDialog.this.getDialog().findViewById(R.id.channelname)).getText().toString();
                        String channelPassword = ((EditText)JoinChannelDialog.this.getDialog().findViewById(R.id.channelpassword)).getText().toString();
                        G.WorldSocket().opcodeHandlers.SendChannelJoin(0, channelName, channelPassword);
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        JoinChannelDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}
