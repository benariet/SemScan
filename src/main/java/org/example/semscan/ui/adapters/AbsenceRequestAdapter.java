package org.example.semscan.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.example.semscan.R;
import org.example.semscan.data.model.AbsenceRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AbsenceRequestAdapter extends RecyclerView.Adapter<AbsenceRequestAdapter.AbsenceRequestViewHolder> {
    
    private List<AbsenceRequest> absenceRequests;
    private OnActionClickListener actionClickListener;
    private SimpleDateFormat dateFormat;
    
    public interface OnActionClickListener {
        void onApprove(AbsenceRequest absenceRequest);
        void onReject(AbsenceRequest absenceRequest);
    }
    
    public AbsenceRequestAdapter(List<AbsenceRequest> absenceRequests, OnActionClickListener actionClickListener) {
        this.absenceRequests = absenceRequests;
        this.actionClickListener = actionClickListener;
        this.dateFormat = new SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public AbsenceRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_absence_request, parent, false);
        return new AbsenceRequestViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AbsenceRequestViewHolder holder, int position) {
        AbsenceRequest absenceRequest = absenceRequests.get(position);
        holder.bind(absenceRequest);
    }
    
    @Override
    public int getItemCount() {
        return absenceRequests.size();
    }
    
    class AbsenceRequestViewHolder extends RecyclerView.ViewHolder {
        private TextView textStudentId;
        private TextView textTimestamp;
        private TextView textReason;
        private TextView textNote;
        private Button btnApprove;
        private Button btnReject;
        
        public AbsenceRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            textStudentId = itemView.findViewById(R.id.text_student_id);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            textReason = itemView.findViewById(R.id.text_reason);
            textNote = itemView.findViewById(R.id.text_note);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
        
        public void bind(AbsenceRequest absenceRequest) {
            textStudentId.setText(absenceRequest.getUserId());
            textTimestamp.setText(dateFormat.format(new Date(absenceRequest.getTimestamp())));
            textReason.setText(absenceRequest.getReason());
            
            if (absenceRequest.getNote() != null && !absenceRequest.getNote().isEmpty()) {
                textNote.setText(absenceRequest.getNote());
                textNote.setVisibility(View.VISIBLE);
            } else {
                textNote.setVisibility(View.GONE);
            }
            
            btnApprove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionClickListener != null) {
                        actionClickListener.onApprove(absenceRequest);
                    }
                }
            });
            
            btnReject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionClickListener != null) {
                        actionClickListener.onReject(absenceRequest);
                    }
                }
            });
        }
    }
}

