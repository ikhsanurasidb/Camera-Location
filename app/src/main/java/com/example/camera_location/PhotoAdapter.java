package com.example.camera_location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaScannerConnection;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private final Context context;
    private RecyclerView recyclerView;
    private final List<String> photoPaths;
    private final List<String> latitudeValue;
    private final List<String> longitudeValue;
    private Geocoder geocoder;
    private List<Address> addresses;

    public PhotoAdapter(Context context, List<String> photoPaths, List<String> latitudeValue, List<String> longitudeValue) {
        this.context = context;
        this.photoPaths = photoPaths;
        this.latitudeValue = latitudeValue;
        this.longitudeValue = longitudeValue;
    }

    public static void updateUI(MediaScannerConnection.OnScanCompletedListener onScanCompletedListener, PhotoAdapter adapter, RecyclerView recyclerView) {
        DBHandler.DBHelper mHelper = new DBHandler.DBHelper((Context) onScanCompletedListener);

        List<String> imagePaths = mHelper.getAllImagePaths();
        List<String> latitudeValue = mHelper.getAllImageLatitude();
        List<String> longitudeValue = mHelper.getAllImageLongitude();

        if (adapter == null) {
            adapter = new PhotoAdapter((Context) onScanCompletedListener, imagePaths, latitudeValue, longitudeValue);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateData(imagePaths);
        }
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String photoPath = photoPaths.get(position);
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
        holder.photoImageView.setImageBitmap(bitmap);

        holder.photoImageView.setOnClickListener(v -> showPopupWithDetails(context, position));
        holder.photoImageView.setOnLongClickListener(v -> {
            showFullImagePopup(holder, context, position);
            return true;
        });

        holder.deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(context, photoPath, position));
    }

    @Override
    public int getItemCount() {
        return photoPaths.size();
    }

    public static void updateUI(Context context, PhotoAdapter adapter, RecyclerView recyclerView) {
        DBHandler.DBHelper mHelper = new DBHandler.DBHelper(context);

        List<String> imagePaths = mHelper.getAllImagePaths();
        List<String> latitudeValue = mHelper.getAllImageLatitude();
        List<String> longitudeValue = mHelper.getAllImageLongitude();

        if (adapter == null) {
            adapter = new PhotoAdapter(context, imagePaths, latitudeValue, longitudeValue);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateData(imagePaths);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<String> newPhotoPaths) {
        this.photoPaths.clear();
        this.photoPaths.addAll(newPhotoPaths);
        notifyDataSetChanged();
    }

    public void deletePhoto(Context context, String photoPath, int position) {
        DBHandler.DBHelper mHelper = new DBHandler.DBHelper(context);
        SQLiteDatabase db = mHelper.getWritableDatabase();

        db.delete(DBHandler.Contract.TaskEntry.TABLE,
                DBHandler.Contract.TaskEntry.COL_IMAGE_PATH + " = ?",
                new String[]{photoPath});
        db.close();

        photoPaths.remove(position);
        latitudeValue.remove(position);
        longitudeValue.remove(position);
        notifyItemRemoved(position);

         File file = new File(photoPath);
         if (file.exists()) {
             file.delete();
         }

         updateUI(context, this, recyclerView);
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImageView;
        Button deleteButton;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.imageView);
            deleteButton = itemView.findViewById(R.id.deletePhoto);
        }
    }

    private void showPopupWithDetails(Context context, int position) {
        String latitude = latitudeValue.get(position);
        String longitude = longitudeValue.get(position);

        try {
            geocoder = new Geocoder(context, Locale.getDefault());
            addresses = geocoder.getFromLocation(Double.parseDouble(latitude), Double.parseDouble(longitude), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String address = addresses.get(0).getAddressLine(0);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Location Details");
        builder.setMessage("Latitude: " + latitude + "\n\nLongitude: " + longitude + "\n\nAddress: " + address);
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteConfirmationDialog(Context context, String photoPath, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Photo");
        builder.setMessage("Are you sure you want to delete this photo?");
        builder.setPositiveButton("Yes", (dialog, which) -> deletePhoto(context, photoPath, position));
        builder.setNegativeButton("No", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showFullImagePopup(@NonNull PhotoViewHolder holder, Context context, int position) {
        String photoPath = photoPaths.get(position);
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);

        LayoutInflater inflater = LayoutInflater.from(context);
        View popupView = inflater.inflate(R.layout.popup_image_full_size, null);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ImageView fullSizeImageView = popupView.findViewById(R.id.fullSizeImageView);
        fullSizeImageView.setImageBitmap(bitmap);

        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.argb(200, 0, 0, 0)));
        popupWindow.showAtLocation(holder.photoImageView, Gravity.CENTER, 0, 0);

        popupView.setOnClickListener(v -> popupWindow.dismiss());
    }

}
