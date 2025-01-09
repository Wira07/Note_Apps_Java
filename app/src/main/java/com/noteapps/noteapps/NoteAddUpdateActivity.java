package com.noteapps.noteapps;

import static com.noteapps.noteapps.db.DatabaseContract.NoteColumns.DATE;
import static com.noteapps.noteapps.db.DatabaseContract.NoteColumns.DESCRIPTION;
import static com.noteapps.noteapps.db.DatabaseContract.NoteColumns.TITLE;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.ContentValues;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.noteapps.noteapps.data.Note;
import com.noteapps.noteapps.databinding.ActivityNoteAddUpdateBinding;
import com.noteapps.noteapps.db.NoteHelper;

public class NoteAddUpdateActivity extends AppCompatActivity {

    private ActivityNoteAddUpdateBinding binding;

    private boolean isEdit = false;
    private Note note;
    private int position;
    private NoteHelper noteHelper;

    public static final String EXTRA_NOTE = "extra_note";
    public static final String EXTRA_POSITION = "extra_position";
    public static final int RESULT_ADD = 101;
    public static final int RESULT_UPDATE = 201;
    public static final int RESULT_DELETE = 301;
    private final int ALERT_DIALOG_CLOSE = 10;
    private final int ALERT_DIALOG_DELETE = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inisialisasi View Binding
        binding = ActivityNoteAddUpdateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        noteHelper = NoteHelper.getInstance(getApplicationContext());
        try {
            noteHelper.open();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        note = getIntent().getParcelableExtra(EXTRA_NOTE);
        if (note != null) {
            position = getIntent().getIntExtra(EXTRA_POSITION, 0);
            isEdit = true;
        } else {
            note = new Note();
        }

        String actionBarTitle;
        String btnTitle;

        if (isEdit) {
            actionBarTitle = "Ubah";
            btnTitle = "Update";

            if (note != null) {
                binding.edtTitle.setText(note.getTitle());
                binding.edtDescription.setText(note.getDescription());
            }
        } else {
            actionBarTitle = "Tambah";
            btnTitle = "Simpan";
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(actionBarTitle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.btnSubmit.setText(btnTitle);

        binding.btnSubmit.setOnClickListener(view -> {
            String title = binding.edtTitle.getText().toString().trim();
            String description = binding.edtDescription.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                binding.edtTitle.setError("Field can not be blank");
                return;
            }

            note.setTitle(title);
            note.setDescription(description);

            Intent intent = new Intent();
            intent.putExtra(EXTRA_NOTE, note);
            intent.putExtra(EXTRA_POSITION, position);

            ContentValues values = new ContentValues();
            values.put(TITLE, title);
            values.put(DESCRIPTION, description);

            if (isEdit) {
                long result = noteHelper.update(String.valueOf(note.getId()), values);
                if (result > 0) {
                    setResult(RESULT_UPDATE, intent);
                    finish();
                } else {
                    Toast.makeText(NoteAddUpdateActivity.this, "Gagal mengupdate data", Toast.LENGTH_SHORT).show();
                }
            } else {
                note.setDate(getCurrentDate());
                values.put(DATE, getCurrentDate());
                long result = noteHelper.insert(values);

                if (result > 0) {
                    note.setId((int) result);
                    setResult(RESULT_ADD, intent);
                    finish();
                } else {
                    Toast.makeText(NoteAddUpdateActivity.this, "Gagal menambah data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isEdit) {
            getMenuInflater().inflate(R.menu.menu_form, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            showAlertDialog(ALERT_DIALOG_DELETE);
        } else if (id == android.R.id.home) {
            showAlertDialog(ALERT_DIALOG_CLOSE);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showAlertDialog(ALERT_DIALOG_CLOSE);
    }

    private void showAlertDialog(int type) {
        final boolean isDialogClose = type == ALERT_DIALOG_CLOSE;
        String dialogTitle = isDialogClose ? "Batal" : "Hapus Note";
        String dialogMessage = isDialogClose ? "Apakah anda ingin membatalkan perubahan pada form?" : "Apakah anda yakin ingin menghapus item ini?";

        new AlertDialog.Builder(this).setTitle(dialogTitle).setMessage(dialogMessage).setCancelable(false).setPositiveButton("Ya", (dialog, id) -> {
            if (isDialogClose) {
                finish();
            } else {
                long result = noteHelper.deleteById(String.valueOf(note.getId()));
                if (result > 0) {
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_POSITION, position);
                    setResult(RESULT_DELETE, intent);
                    finish();
                } else {
                    Toast.makeText(NoteAddUpdateActivity.this, "Gagal menghapus data", Toast.LENGTH_SHORT).show();
                }
            }
        }).setNegativeButton("Tidak", (dialog, id) -> dialog.cancel()).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}