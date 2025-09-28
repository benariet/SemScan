package org.example.semscan.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.example.semscan.R;
import org.example.semscan.data.model.Attendance;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {
    
    private List<Attendance> attendanceList;
    private SimpleDateFormat dateFormat;
    
    public AttendanceAdapter(List<Attendance> attendanceList) {
        this.attendanceList = attendanceList;
        this.dateFormat = new SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        Attendance attendance = attendanceList.get(position);
        holder.bind(attendance);
    }
    
    @Override
    public int getItemCount() {
        return attendanceList.size();
    }
    
    class AttendanceViewHolder extends RecyclerView.ViewHolder {
        private TextView textStudentId;
        private TextView textTimestamp;
        
        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            textStudentId = itemView.findViewById(R.id.text_student_id);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
        }
        
        public void bind(Attendance attendance) {
            textStudentId.setText(attendance.getUserId());
            textTimestamp.setText(dateFormat.format(new Date(attendance.getTimestamp())));
        }
    }
}


