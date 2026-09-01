package com.grimforsaken.sparkofferfilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileNotFoundException;

public class SetupActivity extends Activity {
    private static final int PICK_INSTALLER_APK = 4101;

    private SharedPreferences prefs;
    private TextView heading;
    private TextView body;
    private TextView smallText;
    private TextView alreadyLabel;
    private TextView status;
    private Button deleteButton;
    private Button alreadyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
        LanguageText.ensureDefault(prefs);

        if (prefs.getBoolean(Prefs.INSTALLER_CLEANUP_COMPLETED, false)) {
            openHome();
            return;
        }

        setContentView(R.layout.activity_setup);
        heading = findViewById(R.id.setupHeading);
        body = findViewById(R.id.setupBody);
        smallText = findViewById(R.id.setupSmallText);
        alreadyLabel = findViewById(R.id.alreadyDeletedLabel);
        status = findViewById(R.id.setupStatus);
        deleteButton = findViewById(R.id.deleteInstallerButton);
        alreadyButton = findViewById(R.id.alreadyDeletedButton);

        findViewById(R.id.setupEnglish).setOnClickListener(v -> {
            LanguageText.setSpanish(prefs, false);
            applyLanguage();
        });
        findViewById(R.id.setupSpanish).setOnClickListener(v -> {
            LanguageText.setSpanish(prefs, true);
            applyLanguage();
        });

        deleteButton.setOnClickListener(v -> chooseInstallerApk());
        alreadyButton.setOnClickListener(v -> verifyAlreadyDeleted());
        applyLanguage();
    }

    private void applyLanguage() {
        boolean es = LanguageText.isSpanish(prefs);
        heading.setText(es ? "Finalizar configuración" : "Finish Setup");
        body.setText(es
                ? "Por seguridad y para mantener limpio tu dispositivo, elimina el APK de instalación antes de usar Safe Driver.\n\nSafe Driver te ayudará a localizar el archivo de instalación para que puedas eliminarlo lo más rápido posible."
                : "For security and to keep your device clean, delete the installer APK before using Safe Driver.\n\nSafe Driver will help you locate the installer file so you can remove it as quickly as possible.");
        deleteButton.setText(es ? "Eliminar APK de instalación y continuar" : "Delete Installer APK and Continue");
        smallText.setText(es
                ? "Después de eliminar el APK de instalación, esta pantalla de configuración no volverá a aparecer."
                : "After the installer APK is deleted, this setup screen will never appear again.");
        alreadyLabel.setText(es
                ? "Si el APK de instalación ya fue eliminado:"
                : "If the installer APK has already been deleted:");
        alreadyButton.setText(es ? "Ya eliminé el APK" : "I Already Deleted the APK");
    }

    private void chooseInstallerApk() {
        boolean es = LanguageText.isSpanish(prefs);
        status.setText(es
                ? "Selecciona el APK de Safe Driver. Se eliminará automáticamente después de seleccionarlo."
                : "Select the Safe Driver APK. It will be deleted automatically after you select it.");

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/vnd.android.package-archive", "application/octet-stream"
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_INSTALLER_APK);
        } catch (Exception first) {
            intent.setType("*/*");
            startActivityForResult(intent, PICK_INSTALLER_APK);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_INSTALLER_APK || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (Exception ignored) {}

        prefs.edit().putString(Prefs.INSTALLER_URI, uri.toString()).apply();

        String name = displayName(uri);
        if (name != null && !name.toLowerCase().endsWith(".apk")) {
            boolean es = LanguageText.isSpanish(prefs);
            status.setText(es
                    ? "Ese archivo no parece ser un APK. Selecciona el APK de Safe Driver."
                    : "That file does not appear to be an APK. Select the Safe Driver APK.");
            return;
        }

        if (deleteSelectedDocument(uri) || !documentExists(uri)) {
            completeCleanup();
        } else {
            boolean es = LanguageText.isSpanish(prefs);
            status.setText(es
                    ? "Android no permitió eliminar ese archivo automáticamente. Elimínalo en Archivos y luego toca «Ya eliminé el APK»."
                    : "Android did not allow Safe Driver to delete that file automatically. Delete it in Files, then tap “I Already Deleted the APK.”");
        }
    }

    private boolean deleteSelectedDocument(Uri uri) {
        try {
            if (DocumentsContract.isDocumentUri(this, uri)) {
                return DocumentsContract.deleteDocument(getContentResolver(), uri);
            }
            return getContentResolver().delete(uri, null, null) > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean documentExists(Uri uri) {
        try (android.content.res.AssetFileDescriptor ignored = getContentResolver().openAssetFileDescriptor(uri, "r")) {
            return true;
        } catch (FileNotFoundException missing) {
            return false;
        } catch (Exception uncertain) {
            return true;
        }
    }

    private void verifyAlreadyDeleted() {
        String stored = prefs.getString(Prefs.INSTALLER_URI, "");
        if (!stored.isEmpty()) {
            try {
                Uri uri = Uri.parse(stored);
                if (!documentExists(uri)) {
                    completeCleanup();
                    return;
                }
            } catch (Exception ignored) {}
        }
        showUnverifiedDialog();
    }

    private void showUnverifiedDialog() {
        boolean es = LanguageText.isSpanish(prefs);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(es ? "No se encontró el APK de instalación" : "Installer APK not found")
                .setMessage(es
                        ? "Si ya eliminaste el instalador, toca Continuar. De lo contrario, selecciona el APK de instalación para que Safe Driver pueda eliminarlo."
                        : "If you already deleted the installer, tap Continue. Otherwise, choose the installer APK so Safe Driver can remove it.")
                .setPositiveButton(es ? "Seleccionar APK de instalación" : "Choose Installer APK", (d, which) -> chooseInstallerApk())
                .setNegativeButton(es ? "Continuar" : "Continue", (d, which) -> completeCleanup())
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(d -> {});
        dialog.show();
    }

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {}
        return null;
    }

    private void completeCleanup() {
        prefs.edit()
                .putBoolean(Prefs.INSTALLER_CLEANUP_COMPLETED, true)
                .putInt(Prefs.INSTALLER_CLEANUP_VERSION, 1)
                .apply();
        openHome();
    }

    private void openHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (prefs != null && prefs.getBoolean(Prefs.INSTALLER_CLEANUP_COMPLETED, false)) {
            super.onBackPressed();
        } else {
            moveTaskToBack(true);
        }
    }
}
