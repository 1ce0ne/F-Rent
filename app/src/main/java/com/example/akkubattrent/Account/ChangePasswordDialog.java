package com.example.akkubattrent.Account;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.akkubattrent.R;

public class ChangePasswordDialog {
    private Dialog dialog;
    private EditText oldPasswordEditText;
    private EditText newPasswordEditText;
    private Button confirmButton;

    public ChangePasswordDialog(Context context, ChangePasswordListener listener, String currentPassword) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_change_password);

        oldPasswordEditText = dialog.findViewById(R.id.oldPasswordEditText);
        newPasswordEditText = dialog.findViewById(R.id.newPasswordEditText);
        confirmButton = dialog.findViewById(R.id.confirmButton);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldPassword = oldPasswordEditText.getText().toString();
                String newPassword = newPasswordEditText.getText().toString();

                if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(context, "Поля не могут быть пустыми", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!oldPassword.equals(currentPassword)) {
                    Toast.makeText(context, "Старый пароль неверен", Toast.LENGTH_SHORT).show();
                    return;
                }

                listener.onPasswordChanged(newPassword);
                dialog.dismiss();
            }
        });
    }

    public void show() {
        dialog.show();
    }

    public interface ChangePasswordListener {
        void onPasswordChanged(String newPassword);
    }
}