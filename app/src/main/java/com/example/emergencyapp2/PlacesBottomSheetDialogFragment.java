package com.example.emergencyapp2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class PlacesBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private OnPlaceTypeSelectedListener mListener;
    private RecyclerView recyclerView;

    // Interface to communicate back to the Activity
    public interface OnPlaceTypeSelectedListener {
        void onPlaceTypeSelected(String placeType);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure the host activity implements the callback interface
        try {
            mListener = (OnPlaceTypeSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnPlaceTypeSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_places_bottom_sheet, container, false);
        recyclerView = view.findViewById(R.id.places_recycler_view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Define the list of place types
        List<PlaceType> placeTypes = new ArrayList<>();
        placeTypes.add(new PlaceType("Hospitals", "hospital"));
        placeTypes.add(new PlaceType("Police Stations", "police"));
        placeTypes.add(new PlaceType("Pharmacies", "pharmacy"));
        placeTypes.add(new PlaceType("ATMs", "atm"));

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        PlaceTypeAdapter adapter = new PlaceTypeAdapter(placeTypes, placeType -> {
            mListener.onPlaceTypeSelected(placeType.getApiValue());
            dismiss(); // Close the bottom sheet after selection
        });
        recyclerView.setAdapter(adapter);
    }

    // --- Helper Data Class and Adapter ---

    private static class PlaceType {
        private final String displayName;
        private final String apiValue;

        PlaceType(String displayName, String apiValue) {
            this.displayName = displayName;
            this.apiValue = apiValue;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getApiValue() {
            return apiValue;
        }
    }

    private static class PlaceTypeAdapter extends RecyclerView.Adapter<PlaceTypeAdapter.ViewHolder> {
        private final List<PlaceType> placeTypes;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(PlaceType placeType);
        }

        PlaceTypeAdapter(List<PlaceType> placeTypes, OnItemClickListener listener) {
            this.placeTypes = placeTypes;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place_type, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PlaceType placeType = placeTypes.get(position);
            holder.bind(placeType, listener);
        }

        @Override
        public int getItemCount() {
            return placeTypes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView nameTextView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.place_type_name);
            }

            void bind(final PlaceType placeType, final OnItemClickListener listener) {
                nameTextView.setText(placeType.getDisplayName());
                itemView.setOnClickListener(v -> listener.onItemClick(placeType));
            }
        }
    }
}