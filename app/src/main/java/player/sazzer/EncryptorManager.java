package player.sazzer;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import org.apache.commons.io.FileUtils;

public class EncryptorManager {
    private Context context;
    public EncryptorManager( Context ctx )
    {
        this.context = ctx;
    }

    public boolean VerifyAndCreateAppFolder()
    {
        File folder = new File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name));

        if (!folder.exists()) {
            boolean re = folder.mkdirs();
            Log.i("FOLDER:", String.valueOf(re));
            return re;
        }

        return true;
    }
    public void CreateEncryptedFile( String fileName )
    {
        Log.d("CreateEncryptedFile", "Starting Operation.");
        try{
            MasterKey mainKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Check first if the folder where the audios will be stored in exists.
            if( !VerifyAndCreateAppFolder() )
            {
                Log.e("CreateEncryptedFile","Folder for audios could not be created.");
                return;
            }

            Log.d("CreateEncryptedFile", "Creating basis for encrypted file.");
            File folder = new File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name));
            File fileEncr = new File(folder, "enc-" + fileName);
            EncryptedFile encryptedFile = new EncryptedFile.Builder(
                    context,
                    fileEncr,
                    mainKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            File audioData = new File(folder, fileName);
            int size = (int) audioData.length();
            byte[] bytes = new byte[size];
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(audioData));
            DataInputStream dis = new DataInputStream(buf);
            dis.readFully(bytes);
            //buf.read(bytes, 0, bytes.length);
            buf.close();

            OutputStream outputStream = encryptedFile.openFileOutput();
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();

            File myAudioFile = new File(folder, fileName);

            if( myAudioFile.exists() ) {
                boolean b = myAudioFile.delete();
                Log.i("DELETE_OLD:", String.valueOf(b));
            }

        } catch (GeneralSecurityException | IOException e){
            e.printStackTrace();
        }
    }
    public String ReadEncryptedFile( String fileName )
    {
        Log.d("ReadEncryptedFile", "Starting Operation.");
        try{
            MasterKey mainKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Check first if the folder where the audios will be stored in exists.
            if( !VerifyAndCreateAppFolder() )
            {
                Log.e("CreateEncryptedFile","Folder for audios could not be created.");
                return null;
            }

            Log.d("ReadEncryptedFile", "Creating basis for encrypted file.");
            File folder = new File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name));
            File EncFile = new File( folder , fileName );
            Log.d("ReadEncryptedFile", "Loading " + folder + "/" + fileName);
            EncryptedFile encryptedFile = new EncryptedFile.Builder(
                    context,
                    EncFile,
                    mainKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            InputStream inputStream = encryptedFile.openFileInput();
            //ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // create temp file that will hold byte array
            File tempMp3 = new File( Environment.getExternalStorageDirectory(), "Sazzer/Sazzer-temp" );
            tempMp3.deleteOnExit();

            FileUtils.copyInputStreamToFile( inputStream, tempMp3 );

            return tempMp3.getPath();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
