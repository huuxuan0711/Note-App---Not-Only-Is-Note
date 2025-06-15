package com.xmobile.project0.Fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.xmobile.project0.Activity.NoteActivity;
import com.xmobile.project0.Activity.SearchActivity;
import com.xmobile.project0.Adapter.FolderAdapter;
import com.xmobile.project0.DAO.FolderDao;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Folder;
import com.xmobile.project0.Helper.FinishRequestListener;
import com.xmobile.project0.R;
import com.xmobile.project0.Helper.MyItemTouchHelper;
import com.xmobile.project0.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class HomeFragment extends BaseFragment implements FinishRequestListener {

    private FragmentHomeBinding binding;
    private AlertDialog dialogAddFolder;
    private FolderAdapter adapter;
    private List<Folder> folders = new ArrayList<>();
    private int state = 1;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public static final int REQUEST_CODE_ADD_NOTE = 1;

    public HomeFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        initControl();

        return binding.getRoot();
    }

    private void initControl(){
        Locale locale = getCurrentLocale(requireContext());
        String language = locale.getLanguage();
        //Khởi tạo folder mặc định
        initDefaultFolder(language);

        //Tạo note
        binding.createNote.setOnClickListener(v -> createNote());

        //Tạo folder
        binding.addFolder.setOnClickListener(v -> addFolder());

        //Sửa folder
        binding.txtModify.setOnClickListener(v -> modifyFolder());

        //Tìm kiếm
        binding.searchLayout.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), SearchActivity.class));
        });
    }

    @SuppressLint("CheckResult")
    private void initDefaultFolder(String language) {
        FolderDao folderDao = NoteDatabase.getDatabase(requireContext()).folderDao();

        compositeDisposable.add(
                Single.zip(
                                folderDao.checkFolder("Tất cả ghi chú"),
                                folderDao.checkFolderID(0),
                                (countByName, countById) -> countByName == 0 && countById == 0
                        )
                        .flatMapCompletable(shouldInsert -> {
                            if (shouldInsert) {
                                Folder folder = new Folder();
                                folder.setName("Tất cả ghi chú");
                                folder.setSize(0);
                                return folderDao.insertFolder(folder);
                            } else {
                                String name = language.equals("en") ? "All notes" : "Tất cả ghi chú";
                                return folderDao.updateFolder(0, name);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                this::listFolder,
                                throwable -> Log.e("initDefaultFolder", "Error: ", throwable)
                        )
        );
    }


    @SuppressLint("ClickableViewAccessibility")
    private void modifyFolder() {
        binding.txtModify.setVisibility(View.INVISIBLE);
        binding.txtDone.setVisibility(View.VISIBLE);
        binding.addFolder.setVisibility(View.INVISIBLE);
        binding.createNote.setVisibility(View.INVISIBLE);
        state = 0;
        adapter.setState(state);
        binding.txtDone.setOnClickListener(v -> {
            binding.txtModify.setVisibility(View.VISIBLE);
            binding.txtDone.setVisibility(View.GONE);
            binding.addFolder.setVisibility(View.VISIBLE);
            binding.createNote.setVisibility(View.VISIBLE);
            state = 1;
            adapter.setState(state);
        });
    }

    private void createNote(){
        int idFolder = folders.get(0).getId();
        String nameFolder = folders.get(0).getName();
        Intent intent = new Intent(requireContext(), NoteActivity.class);
        intent.putExtra("folderId", idFolder);
        intent.putExtra("folderName", nameFolder);
        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
    }

    private void addFolder() {
        if (dialogAddFolder == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View view = LayoutInflater.from(requireContext()).inflate(
                    R.layout.layout_addfolder,
                    requireView().findViewById(R.id.layout_add_folder)
            );
            builder.setView(view);
            dialogAddFolder = builder.create();

            final TextInputLayout inputName = view.findViewById(R.id.edtNameFolder);
            final TextView checkFolder = view.findViewById(R.id.checkFolder);

            // Thêm thư mục
            view.findViewById(R.id.txtAddFolder).setOnClickListener(v -> {
                String folderName = Objects.requireNonNull(inputName.getEditText()).getText().toString().trim();

                if (folderName.isEmpty()) {
                    showError(checkFolder, getString(R.string.please_enter_folder_name));
                } else if (folderName.length() > 20) {
                    showError(checkFolder, getString(R.string.folder_name_is_so_long));
                } else {
                    saveFolder(folderName, checkFolder);
                }
            });

            // Hủy
            view.findViewById(R.id.txtCancel).setOnClickListener(v -> dialogAddFolder.dismiss());

            // Reset khi đóng dialog
            dialogAddFolder.setOnDismissListener(dialog -> {
                checkFolder.setVisibility(View.INVISIBLE);
                Objects.requireNonNull(inputName.getEditText()).setText(null);
                inputName.clearFocus();
            });
        }

        dialogAddFolder.show();
    }

    private void saveFolder(String nameFolder, TextView checkFolder) {
        FolderDao folderDao = NoteDatabase.getDatabase(requireContext()).folderDao();

        compositeDisposable.add(
                folderDao.checkFolder(nameFolder)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .flatMapCompletable(count -> {
                            if (count == 0) {
                                Folder folder = new Folder();
                                folder.setName(nameFolder);
                                folder.setSize(0);
                                folders.add(folder); // Add to local list
                                return folderDao.insertFolder(folder);
                            } else {
                                return Completable.error(new Throwable("exists"));
                            }
                        })
                        .subscribe(
                                () -> {
                                    dialogAddFolder.dismiss();
                                    adapter.notifyDataSetChanged();
                                },
                                throwable -> {
                                    if ("exists".equals(throwable.getMessage())) {
                                        showError(checkFolder, getString(R.string.folder_name_is_existed));
                                    } else {
                                        Toast.makeText(requireContext(), "Lỗi khi lưu thư mục", Toast.LENGTH_SHORT).show();
                                    }
                                }
                        )
        );
    }

    private void showError(TextView errorView, String message) {
        errorView.setVisibility(View.VISIBLE);
        errorView.setText(message);
    }

    private void listFolder(){
        FolderDao folderDao = NoteDatabase.getDatabase(requireContext()).folderDao();

        compositeDisposable.add(
                folderDao.getAllFolders()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    folders = result;
                                    binding.recyclerViewFolder.setLayoutManager(
                                            new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                                    );
                                    adapter = new FolderAdapter(folders, requireContext(), HomeFragment.this);
                                    adapter.setState(1);
                                    attachMove();
                                    binding.recyclerViewFolder.setAdapter(adapter);
                                },
                                throwable -> {
                                    Toast.makeText(requireContext(), "Lỗi khi tải thư mục", Toast.LENGTH_SHORT).show();
                                    Log.e("loadFolders", "Error", throwable);
                                }
                        )
        );
    }

    private void attachMove(){
        ItemTouchHelper.Callback callback = new MyItemTouchHelper(requireContext() ,adapter, 0);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        adapter.setTouchHelper(itemTouchHelper);
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewFolder);
    }

    public Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            return context.getResources().getConfiguration().locale;
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        binding.txtModify.setVisibility(View.VISIBLE);
        binding.txtDone.setVisibility(View.GONE);
        binding.addFolder.setVisibility(View.VISIBLE);
        binding.createNote.setVisibility(View.VISIBLE);
        state = 1;
        if (adapter != null) adapter.setState(state);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            listFolder();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
        binding = null; // nếu dùng ViewBinding
    }



    @Override
    public void onFinishRequested() {
        requireActivity().finish();
    }
}

//Done
//ấn Tạo -> gọi đến Insert trong database, đóng dialog, notice change cho adapter để update
//ấn Hủy bỏ -> đóng dialog
//list cung cấp cho recycler view lấy từ getAllFolder trong FolderDAO

//Done
//khi sửa tên folder -> gọi đến updateFolder trong FolderDAO, notice change cho adapter để update
//khi xóa folder -> gọi đến deleteFolder trong FolderDAO, notice change cho adapter để update
//lấy số lượng các note trong folder -> gọi đến sizeFolder trong FolderDAO

//Tìm kiếm -> tìm trên toàn bộ nội dung note
//Kiểm tra khi chuỗi tìm kiếm rỗng -> gọi lại getAllFolder để hiển thị lại list
