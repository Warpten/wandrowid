package org.warpten.wandrowid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;

import org.warpten.wandrowid.fragments.ChatMessageType;

public class WhisperDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_whisper, null))
                .setPositiveButton(R.string.button_send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String targetName = ((EditText)WhisperDialog.this.getDialog().findViewById(R.id.dialog_whispertarget)).getText().toString();
                        String message = ((EditText)WhisperDialog.this.getDialog().findViewById(R.id.whisper_message)).getText().toString();
                        G.WorldSocket().opcodeHandlers.SendMessageChat(ChatMessageType.Whisper, targetName, message);;
                    }
                })
                .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        WhisperDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}
