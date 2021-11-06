package com.tech4bytes.android.gdrivesync;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import javax.annotation.Nullable;

public class FolderPicker extends Activity {
    public String pickFolder(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(Intent.createChooser(i, "Choose directory"), 9999);
        }
        return "prasun";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case 9999:
                Log.i("Test", "Result URI " + data.getData());
                break;
        }
    }
}