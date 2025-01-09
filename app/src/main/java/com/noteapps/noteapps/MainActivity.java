package com.noteapps.noteapps;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.noteapps.noteapps.adapter.NoteAdapter;
import com.noteapps.noteapps.data.Note;
import com.noteapps.noteapps.databinding.ActivityMainBinding;
import com.noteapps.noteapps.db.NoteHelper;
import com.noteapps.noteapps.helper.MappingHelper;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NoteAdapter adapter;

    private final ActivityResultLauncher<Intent> resultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null) {
                    switch (result.getResultCode()) {
                        case NoteAddUpdateActivity.RESULT_ADD:
                            Note noteAdd = result.getData().getParcelableExtra(NoteAddUpdateActivity.EXTRA_NOTE);
                            adapter.addItem(noteAdd);
                            binding.rvNotes.smoothScrollToPosition(adapter.getItemCount() - 1);
                            showSnackbarMessage("Satu item berhasil ditambahkan");
                            break;
                        case NoteAddUpdateActivity.RESULT_UPDATE:
                            Note noteUpdate = result.getData().getParcelableExtra(NoteAddUpdateActivity.EXTRA_NOTE);
                            int positionUpdate = result.getData().getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0);
                            adapter.updateItem(positionUpdate, noteUpdate);
                            binding.rvNotes.smoothScrollToPosition(positionUpdate);
                            showSnackbarMessage("Satu item berhasil diubah");
                            break;
                        case NoteAddUpdateActivity.RESULT_DELETE:
                            int positionDelete = result.getData().getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0);
                            adapter.removeItem(positionDelete);
                            showSnackbarMessage("Satu item berhasil dihapus");
                            break;
                    }
                }
            });

    private static final String EXTRA_STATE = "EXTRA_STATE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notes");
        }

        binding.rvNotes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotes.setHasFixedSize(true);

        adapter = new NoteAdapter((selectedNote, position) -> {
            Intent intent = new Intent(MainActivity.this, NoteAddUpdateActivity.class);
            intent.putExtra(NoteAddUpdateActivity.EXTRA_NOTE, selectedNote);
            intent.putExtra(NoteAddUpdateActivity.EXTRA_POSITION, position);
            resultLauncher.launch(intent);
        });
        binding.rvNotes.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteAddUpdateActivity.class);
            resultLauncher.launch(intent);
        });

        if (savedInstanceState == null) {
            loadNotesAsync();
        } else {
            ArrayList<Note> list = savedInstanceState.getParcelableArrayList(EXTRA_STATE);
            if (list != null) {
                adapter.setListNotes(list);
            }
        }
    }

    private void loadNotesAsync() {
        new LoadNotesAsync(this, binding.progressbar, adapter).execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(EXTRA_STATE, adapter.getListNotes());
    }

    private void showSnackbarMessage(String message) {
        Snackbar.make(binding.rvNotes, message, Snackbar.LENGTH_SHORT).show();
    }

    private static class LoadNotesAsync extends AsyncTask<Void, Void, ArrayList<Note>> {
        private final WeakReference<MainActivity> activityReference;
        private final WeakReference<View> progressBar;
        private final WeakReference<NoteAdapter> adapterReference;

        LoadNotesAsync(MainActivity activity, View progressBar, NoteAdapter adapter) {
            this.activityReference = new WeakReference<>(activity);
            this.progressBar = new WeakReference<>(progressBar);
            this.adapterReference = new WeakReference<>(adapter);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            View progress = progressBar.get();
            if (progress != null) {
                progress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected ArrayList<Note> doInBackground(Void... voids) {
            MainActivity activity = activityReference.get();
            if (activity != null) {
                NoteHelper noteHelper = NoteHelper.getInstance(activity.getApplicationContext());
                try {
                    noteHelper.open();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                ArrayList<Note> notes = MappingHelper.mapCursorToArrayList(noteHelper.queryAll());
                noteHelper.close();
                return notes;
            }
            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(ArrayList<Note> notes) {
            super.onPostExecute(notes);
            MainActivity activity = activityReference.get();
            View progress = progressBar.get();
            NoteAdapter adapter = adapterReference.get();

            if (activity != null && progress != null && adapter != null) {
                progress.setVisibility(View.INVISIBLE);
                if (notes.size() > 0) {
                    adapter.setListNotes(notes);
                } else {
                    adapter.setListNotes(new ArrayList<>());
                    activity.showSnackbarMessage("Tidak ada data saat ini");
                }
            }
        }
    }
}
