package ro.atm.corden.view.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivityUserTimelineBinding;
import ro.atm.corden.model.transport_model.Action;
import ro.atm.corden.util.adapter.TimelineAdapter;
import ro.atm.corden.util.constant.ExtraConstant;
import ro.atm.corden.viewmodel.UserTimelineViewModel;

public class UserTimelineActivity extends AppCompatActivity {
    private ActivityUserTimelineBinding binding;
    private UserTimelineViewModel viewModel;

    private String username;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.timeline_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.selectDate:
                final Calendar cldr = Calendar.getInstance();
                int day = cldr.get(Calendar.DAY_OF_MONTH);
                int month = cldr.get(Calendar.MONTH);
                int year = cldr.get(Calendar.YEAR);
                DatePickerDialog picker = new DatePickerDialog(UserTimelineActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                String date = String.format("%s-%s-%s", year, monthOfYear + 1, dayOfMonth);
                                viewModel.setActions(username, date);
                            }
                        }, year, month, day);
                picker.show();
                break;
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_timeline);
        binding.setLifecycleOwner(this);

        viewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(this.getApplication())
                .create(UserTimelineViewModel.class);

        binding.setViewModel(viewModel);

        binding.timeline.setLayoutManager(new LinearLayoutManager(this));
        binding.timeline.setHasFixedSize(true);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        username = getIntent().getStringExtra(ExtraConstant.GET_USERNAME);

        if(username != null){
            viewModel.setActions(username, new SimpleDateFormat("YYYY-MM-dd").format(new Date()));
        }

        final TimelineAdapter timelineAdapter = new TimelineAdapter();
        binding.timeline.setAdapter(timelineAdapter);

        viewModel.getActions().observe(this, new Observer<List<Action>>() {
            @Override
            public void onChanged(List<Action> actions) {
                timelineAdapter.setActions(actions);
            }
        });
    }
}
