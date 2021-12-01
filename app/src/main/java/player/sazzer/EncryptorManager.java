package player.sazzer;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;
import androidx.security.crypto.MasterKeys;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.KeyGeneratorSpi;
import javax.crypto.NoSuchPaddingException;

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
            KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
            //String mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);
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
            File fileEncr = new File(folder, fileName + "-enc");
            EncryptedFile encryptedFile = new EncryptedFile.Builder(
                    context,
                    fileEncr,
                    mainKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            if( fileEncr.exists() ) {
                fileEncr.delete();
            }

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

        } catch (GeneralSecurityException | IOException e){
            e.printStackTrace();
        }
    }
    public void ReadEncryptedFile( String fileName )
    {
        Log.d("ReadEncryptedFile", "Starting Operation.");
        try{
            KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
            String mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);

            // Check first if the folder where the audios will be stored in exists.
            if( !VerifyAndCreateAppFolder() )
            {
                Log.e("CreateEncryptedFile","Folder for audios could not be created.");
                return;
            }

            Log.d("ReadEncryptedFile", "Creating basis for encrypted file.");
            File folder = new File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name));
            EncryptedFile encryptedFile = new EncryptedFile.Builder(
                    new File( folder , fileName + "-enc" ),
                    context,
                    mainKeyAlias,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            InputStream inputStream = encryptedFile.openFileInput();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int nextByte = inputStream.read();
            while( nextByte != -1 ) {
                byteArrayOutputStream.write(nextByte);
                nextByte = inputStream.read();
            }

            byte[] plaintext = byteArrayOutputStream.toByteArray();
            /*
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            Key k = keygen.generateKey();

            // Create the cypher which the will be converted to.
            Cipher AES = Cipher.getInstance("AES/ECB/PKCS5Padding");
            AES.init(Cipher.ENCRYPT_MODE, k);

            // Load the file to encrypt.
            Log.d("CreateEncryptedFile", "Opening File "+ fileNamePath);
            FileOutputStream fs = new FileOutputStream(fileNamePath);
            CipherOutputStream out = new CipherOutputStream(fs, AES);
            out.write(fs.toString().getBytes());
            out.flush();
            out.close();

            Log.d("CreateEncryptedFile", "File "+ fileNamePath + " Written.");

            Cipher aes2 = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aes2.init(Cipher.DECRYPT_MODE, k);

            FileInputStream fis = new FileInputStream(fileNamePath);
            CipherInputStream in = new CipherInputStream(fis, aes2);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] b = new byte[1024];
            int bitsLeidos;
            while( (bitsLeidos = in.read(b)) >= 0 )
            {
                baos.write(b, 0, bitsLeidos);
            }
            System.out.println(new String(baos.toByteArray()));

             */
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }
}
