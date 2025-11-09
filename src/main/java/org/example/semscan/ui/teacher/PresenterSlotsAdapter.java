package org.example.semscan.ui.teacher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.utils.Logger;

import java.util.ArrayList;
import java.util.List;

class PresenterSlotsAdapter extends RecyclerView.Adapter<PresenterSlotsAdapter.SlotViewHolder> {

    interface SlotActionListener {
        void onRegisterClicked(ApiService.SlotCard slot);
    }

    private final List<ApiService.SlotCard> items = new ArrayList<>();
    private final SlotActionListener listener;

    PresenterSlotsAdapter(@NonNull SlotActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_presenter_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void submitList(List<ApiService.SlotCard> slots) {
        items.clear();
        if (slots != null) {
            items.addAll(slots);
        }
        notifyDataSetChanged();
    }

    private String formatPresenters(List<ApiService.PresenterCoPresenter> presenters) {
        if (presenters == null || presenters.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (ApiService.PresenterCoPresenter presenter : presenters) {
            StringBuilder builder = new StringBuilder();
            if (presenter.name != null) {
                builder.append(presenter.name);
            }
            if (presenter.topic != null && presenter.topic.trim().length() > 0) {
                if (builder.length() > 0) {
                    builder.append(" — ");
                }
                builder.append(presenter.topic.trim());
            }
            if (builder.length() > 0) {
                names.add(builder.toString());
            }
        }
        if (names.isEmpty()) {
            return null;
        }
        return android.text.TextUtils.join("\n", names);
    }

    class SlotViewHolder extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView schedule;
        private final TextView location;
        private final TextView capacity;
        private final TextView presenters;
        private final TextView badge;
        private final MaterialButton registerButton;

        SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_slot_title);
            schedule = itemView.findViewById(R.id.text_slot_schedule);
            location = itemView.findViewById(R.id.text_slot_location);
            capacity = itemView.findViewById(R.id.text_slot_capacity);
            presenters = itemView.findViewById(R.id.text_slot_presenters);
            badge = itemView.findViewById(R.id.text_slot_badge);
            registerButton = itemView.findViewById(R.id.btn_register_slot);
        }

        void bind(final ApiService.SlotCard slot) {
            Context context = itemView.getContext();
            String titleText = context.getString(R.string.presenter_home_slot_title_format,
                    safe(slot.dayOfWeek), safe(slot.date));
            title.setText(titleText);

            schedule.setText(safe(slot.timeRange));
            
            // Make card clickable - clicking card triggers register action
            // Allow registration attempt even if canRegister is false (server will validate)
            itemView.setOnClickListener(v -> {
                // Allow registration attempt if:
                // 1. Not already registered in this slot
                // 2. Slot is not full
                // 3. User is not already registered (checked by server)
                if (!slot.alreadyRegistered && slot.state != ApiService.SlotState.FULL) {
                    if (listener != null) {
                        listener.onRegisterClicked(slot);
                    }
                }
            });

            StringBuilder venue = new StringBuilder();
            if (slot.room != null && !slot.room.isEmpty()) {
                venue.append(context.getString(R.string.room_with_label, slot.room));
            }
            if (slot.building != null && !slot.building.isEmpty()) {
                if (venue.length() > 0) {
                    venue.append(" • ");
                }
                venue.append(context.getString(R.string.building_with_label, slot.building));
            }
            location.setText(venue.toString());
            location.setVisibility(venue.length() > 0 ? View.VISIBLE : View.GONE);

            String capacityText = context.getString(R.string.presenter_home_slot_registered_count,
                    slot.enrolledCount, slot.capacity)
                    + context.getString(R.string.presenter_home_slot_available_count, slot.availableCount);
            capacity.setText(capacityText);

            String presentersText = formatPresenters(slot.registered);
            if (presentersText != null) {
                presenters.setText(presentersText);
                presenters.setVisibility(View.VISIBLE);
            } else {
                presenters.setVisibility(View.GONE);
            }

            badge.setVisibility(View.GONE);
            if (slot.alreadyRegistered) {
                badge.setText(R.string.presenter_slot_registered_badge);
                badge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_slot_status));
                badge.setVisibility(View.VISIBLE);
            } else if (slot.state == ApiService.SlotState.FULL) {
                badge.setText(R.string.presenter_slot_full_badge);
                badge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_slot_status_red));
                badge.setVisibility(View.VISIBLE);
            } else if (slot.state == ApiService.SlotState.SEMI) {
                badge.setText(R.string.presenter_home_slot_state_partial);
                badge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_slot_status_yellow));
                badge.setVisibility(View.VISIBLE);
            } else if (slot.state == ApiService.SlotState.FREE) {
                badge.setText(R.string.presenter_home_slot_state_available);
                badge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_slot_status_green));
                badge.setVisibility(View.VISIBLE);
            }

            if (!slot.canRegister || slot.alreadyRegistered || slot.state == ApiService.SlotState.FULL) {
                registerButton.setVisibility(View.GONE);
            } else {
                registerButton.setVisibility(View.VISIBLE);
                registerButton.setOnClickListener(v -> {
                    Logger.userAction("Register Slot", "Attempting to register for slot=" + slot.slotId);
                    if (listener != null) {
                        listener.onRegisterClicked(slot);
                    }
                });
            }

            // Show disable reason or default message if can't register
            if (!slot.canRegister) {
                if (slot.disableReason != null && slot.disableReason.trim().length() > 0) {
                    location.setText(slot.disableReason);
                } else {
                    // Default message when can't register but no specific reason provided
                    location.setText(context.getString(R.string.presenter_slot_registered_in_another));
                }
                location.setVisibility(View.VISIBLE);
            }
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
