package com.example.emergencyapp2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder> {

    private List<EmergencyContact> contacts;
    private OnContactDeleteListener deleteListener;
    private boolean isEditMode = false;

    public void setEditMode(boolean isEditing) {
        this.isEditMode = isEditing;
        notifyDataSetChanged(); // Refresh the entire list to show/hide buttons
    }
    public interface OnContactDeleteListener {
        void onContactDelete(int position);
    }
    public EmergencyContactsAdapter(List<EmergencyContact> contacts, OnContactDeleteListener listener) {
        this.contacts = contacts;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);
        holder.nameTextView.setText(contact.getName());
        holder.numberTextView.setText(contact.getNumber());

        // 3. Set visibility based on the edit mode
        holder.deleteButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public List<EmergencyContact> getContacts() {
        return contacts;
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView numberTextView;
        ImageButton deleteButton;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contactNameTextView);
            numberTextView = itemView.findViewById(R.id.contactNumberTextView);
            deleteButton = itemView.findViewById(R.id.deleteContactButton);

            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        deleteListener.onContactDelete(position);
                    }
                }
            });
        }
    }
}