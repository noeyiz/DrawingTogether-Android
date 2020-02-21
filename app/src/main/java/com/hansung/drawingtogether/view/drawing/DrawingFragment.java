package com.hansung.drawingtogether.view.drawing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;

import com.hansung.drawingtogether.R;
import com.hansung.drawingtogether.databinding.FragmentDrawingBinding;
import com.hansung.drawingtogether.view.NavigationCommand;
import com.hansung.drawingtogether.view.main.MainActivity;

import java.io.File;
import java.io.IOException;

public class DrawingFragment extends Fragment {

    private final int PICK_FROM_GALLERY = 0;
    private final int PICK_FROM_CAMERA = 1;

    Point size;

    FragmentDrawingBinding binding;
    private DrawingViewModel drawingViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDrawingBinding.inflate(inflater, container, false);
        drawingViewModel = ViewModelProviders.of(this).get(DrawingViewModel.class);

        // 디바이스 화면 size 구하기
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);

        // 디바이스 화면 넓이의 3배 = 드로잉뷰 넓이
        ViewGroup.LayoutParams layoutParams = binding.drawingView.getLayoutParams();
        layoutParams.width = size.x*3;
        binding.drawingView.setLayoutParams(layoutParams);

        drawingViewModel.drawingCommands.observe(this, new Observer<DrawingCommand>() {
            @Override
            public void onChanged(DrawingCommand drawingCommand) {
                if (drawingCommand instanceof DrawingCommand.PenMode) {
                    showPopup(((DrawingCommand.PenMode) drawingCommand).getView(), R.layout.popup_pen_mode);
                }
                if (drawingCommand instanceof DrawingCommand.EraserMode) {
                    showPopup(((DrawingCommand.EraserMode) drawingCommand).getView(), R.layout.popup_eraser_mode);
                }
                if (drawingCommand instanceof DrawingCommand.TextMode) {
                    showPopup(((DrawingCommand.TextMode) drawingCommand).getView(), R.layout.popup_text_mode);
                }
                if (drawingCommand instanceof DrawingCommand.ShapeMode) {
                    showPopup(((DrawingCommand.ShapeMode) drawingCommand).getView(), R.layout.popup_shape_mode);
                }
            }
        });

        drawingViewModel.navigationCommands.observe(this, new Observer<NavigationCommand>() {
            @Override
            public void onChanged(NavigationCommand navigationCommand) {
                if (navigationCommand instanceof NavigationCommand.To) {
                    NavHostFragment.findNavController(DrawingFragment.this)
                            .navigate(((NavigationCommand.To) navigationCommand).getDestinationId());
                }
                if (navigationCommand instanceof  NavigationCommand.Back) {
                    NavHostFragment.findNavController(DrawingFragment.this).popBackStack();
                }
            }
        });

        binding.setVm(drawingViewModel);

        // 현재 사용자 수. 지금은 그냥 2로 해놓음
        binding.setUserNum(Integer.toString(2));

        setHasOptionsMenu(true);

        return binding.getRoot();
    }

    public void showPopup(View view, int layout) {
        View penSettingPopup = getLayoutInflater().inflate(layout, null);
        penSettingPopup.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        PopupWindow popupWindow = new PopupWindow(penSettingPopup, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        popupWindow.showAtLocation(penSettingPopup, Gravity.NO_GRAVITY, location[0], location[1] - penSettingPopup.getMeasuredHeight());
        popupWindow.setElevation(20);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case PICK_FROM_GALLERY:
                if (data == null) {
                    return;
                }
                Uri uri = data.getData();
                ImageView imageView = new ImageView(getContext());
                imageView.setLayoutParams(new LinearLayout.LayoutParams(size.x, ViewGroup.LayoutParams.MATCH_PARENT));
                imageView.setImageURI(uri);
                binding.backgroundView.addView(imageView);
                break;
            case PICK_FROM_CAMERA:
                try {
                    File file = new File(drawingViewModel.getPhotoPath());
                    Bitmap bitmap = rotateBitmap(MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.fromFile(file)));

                    if (bitmap != null) {
                        imageView = new ImageView(getContext());
                        imageView.setLayoutParams(new LinearLayout.LayoutParams(size.x, ViewGroup.LayoutParams.MATCH_PARENT));
                        imageView.setImageBitmap(bitmap);
                        binding.backgroundView.addView(imageView);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).setToolbarTitle("Drawing");
        ((MainActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.application_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.drawing_search:
                drawingViewModel.clickSearch(getView());
                break;
            case R.id.gallery:
                drawingViewModel.getImageFromGallery(DrawingFragment.this);
                break;
            case R.id.camera:
                drawingViewModel.getImageFromCamera(DrawingFragment.this);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public Bitmap rotateBitmap(Bitmap bitmap) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(drawingViewModel.getPhotoPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }
}
