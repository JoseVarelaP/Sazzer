package player.sazzer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

// Clase personalizada que se utilizara en Recycler view.
class ListadoMusica extends BaseAdapter {
    private LayoutInflater InfoCan;
    private List<Song> canciones;

    ListadoMusica (Context context, List<Song> listaCanciones) {
        this.canciones = listaCanciones;
        this.InfoCan = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return canciones.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Incluye la vista para el objeto de canción, pero no lo adjuntes al pariente, porque puede
        // resultar en situaciones raras.
        LinearLayout vista = (LinearLayout) InfoCan.inflate(R.layout.list_item, parent, false);
        // Obten objetos
        TextView nombreCancion = (TextView) vista.findViewById(R.id.nombreCancion);
        TextView nombreArtista = (TextView) vista.findViewById(R.id.nombreArtista);
        TextView nombreAlbum = (TextView) vista.findViewById(R.id.nombreAlbum);

        // Agarra la canción para el objeto actual...
        Song canc = canciones.get(position);

        // Asigna los nombres y TAMBIEN un elemento que pueda representar la canción de manera
        // unica.
        nombreCancion.setText( canc.getTitle() );
        nombreArtista.setText( canc.getArtist() );
        nombreAlbum.setText( canc.getAlbum() );
        vista.setTag(position);

        // Todo listo, regresa el objeto.
        return vista;
    }
}
