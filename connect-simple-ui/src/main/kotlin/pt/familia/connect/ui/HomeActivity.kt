package pt.familia.connect.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import pt.familia.connect.core.FamilyContact
import pt.familia.connect.core.FamilyContacts
import java.util.concurrent.Executors

/**
 * Ecrã principal: as fotografias dos familiares (favoritos da agenda), grandes, com o nome por
 * baixo. Tocar numa fotografia abre as duas ações: Chamada e Videochamada.
 *
 * Adapta-se de telemóveis a tablets de 10–15″: o número de colunas depende da largura do ecrã.
 * Não há menus nem ícones pequenos. O cuidador entra na configuração com um toque longo de
 * 3 segundos na engrenagem.
 */
class HomeActivity : Activity() {

    private lateinit var grid: GridView
    private lateinit var empty: TextView
    private lateinit var banner: Button
    private val adapter = FamilyAdapter()
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connect_ui_home)
        grid = findViewById(R.id.connect_ui_home_grid)
        empty = findViewById(R.id.connect_ui_home_empty)
        banner = findViewById(R.id.connect_ui_home_banner)
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, position, _ ->
            startActivity(ContactActivity.intent(this, adapter.items[position].lookupKey))
        }
        findViewById<Button>(R.id.connect_ui_home_phone).setOnClickListener { startActivity(fullPhoneIntent(this)) }
        setupCaregiverGesture(findViewById(R.id.connect_ui_home_settings))
    }

    override fun onResume() {
        super.onResume()
        grid.numColumns = columnsFor(resources.configuration.screenWidthDp)
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQ_CONTACTS)
        }
        updateDialerBanner()
        reload()
    }

    private fun reload() {
        io.execute {
            val items = FamilyContacts.favorites(this)
            main.post {
                adapter.items = items
                adapter.notifyDataSetChanged()
                empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    /** Sem ser o telefone predefinido, a app não recebe chamadas. O aviso é um botão grande. */
    private fun updateDialerBanner() {
        val telecom = getSystemService(TelecomManager::class.java)
        val isDefault = telecom?.defaultDialerPackage == packageName
        banner.visibility = if (isDefault) View.GONE else View.VISIBLE
        banner.setOnClickListener {
            val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else {
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
            startActivityForResult(request, REQ_DIALER)
        }
    }

    /** Toque longo de 3 segundos: evita que o idoso entre na configuração por engano. */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupCaregiverGesture(view: View) {
        val open = Runnable { startActivity(Intent(this, CaregiverActivity::class.java)) }
        view.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.postDelayed(open, CAREGIVER_HOLD_MS)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.removeCallbacks(open)
            }
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CONTACTS) reload()
    }

    @Deprecated("Activity.onBackPressed")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Este é o ecrã principal: "voltar" não faz sair para um sítio desconhecido.
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }

    private inner class FamilyAdapter : BaseAdapter() {
        var items: List<FamilyContact> = emptyList()

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].contactId

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.connect_ui_tile, parent, false)
            val contact = items[position]
            // Largura do ecrã e não do pai: na primeira medição, o pai ainda pode ter largura 0.
            val size = resources.displayMetrics.widthPixels / maxOf(grid.numColumns, 1)
            val photo = view.findViewById<ImageView>(R.id.connect_ui_tile_photo)
            view.findViewById<View>(R.id.connect_ui_tile_frame).layoutParams.height = (size * 0.8).toInt()
            view.findViewById<TextView>(R.id.connect_ui_tile_name).text = contact.name
            bindAvatar(this@HomeActivity, contact, photo, view.findViewById(R.id.connect_ui_tile_initials), size)
            view.contentDescription = getString(R.string.connect_ui_tile_description, contact.name)
            return view
        }
    }

    companion object {
        private const val REQ_CONTACTS = 61
        private const val REQ_DIALER = 62
        private const val CAREGIVER_HOLD_MS = 3000L

        /** Uma coluna por cada ~260 dp: 2 num telemóvel, 3 a 5 em tablets de 10–15″. */
        internal fun columnsFor(widthDp: Int) = (widthDp / 260).coerceIn(2, 5)

        /** Fossify Phone completa (histórico, marcador, definições), através do filtro que a MainActivity já tem. */
        internal fun fullPhoneIntent(activity: Activity): Intent =
            Intent(Intent.ACTION_VIEW)
                .setType(CallLog.Calls.CONTENT_TYPE) // "vnd.android.cursor.dir/calls"
                .setPackage(activity.packageName)
    }
}
