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
import android.text.format.DateFormat
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
 * Ecrã inicial (Figma: "1 · Início").
 *
 * - Relógio e data grandes no topo. Um toque longo de 3 segundos no relógio abre a configuração
 *   do cuidador; não há nenhuma engrenagem à vista.
 * - Aviso de chamada perdida de um familiar, com "Ligar de volta".
 * - Um cartão por familiar (favoritos da agenda), com Ligar e Vídeo diretos: um toque e a
 *   chamada começa. Não há ecrã intermédio.
 * - De 2 a 5 colunas, conforme a largura (telemóvel até tablet de 15″).
 */
class HomeActivity : Activity() {

    private lateinit var grid: GridView
    private lateinit var empty: TextView
    private lateinit var banner: Button
    private lateinit var missedBox: View
    private val adapter = FamilyAdapter()
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connect_ui_home)
        grid = findViewById(R.id.connect_ui_home_grid)
        empty = findViewById(R.id.connect_ui_home_empty)
        banner = findViewById(R.id.connect_ui_home_banner)
        missedBox = findViewById(R.id.connect_ui_home_missed)
        grid.adapter = adapter
        setupCaregiverGesture(findViewById(R.id.connect_ui_home_clock))
    }

    override fun onResume() {
        super.onResume()
        grid.numColumns = columnsFor(resources.configuration.screenWidthDp)
        val missing = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), REQ_PERMISSIONS)
        updateDialerBanner()
        reload()
    }

    private fun reload() {
        io.execute {
            val family = FamilyContacts.favorites(this)
            val missed = MissedCalls.latest(this, family)
            main.post {
                if (isDestroyed) return@post
                adapter.items = family
                adapter.notifyDataSetChanged()
                empty.visibility = if (family.isEmpty()) View.VISIBLE else View.GONE
                showMissed(missed)
            }
        }
    }

    private fun showMissed(missed: MissedCalls.Missed?) {
        if (missed == null) {
            missedBox.visibility = View.GONE
            return
        }
        missedBox.visibility = View.VISIBLE
        findViewById<TextView>(R.id.connect_ui_home_missed_title).text =
            getString(R.string.connect_ui_missed_title, missed.contact.name)
        val time = DateFormat.getTimeFormat(this).format(missed.timeMillis)
        findViewById<TextView>(R.id.connect_ui_home_missed_when).text = getString(
            if (MissedCalls.isToday(missed.timeMillis)) R.string.connect_ui_missed_today else R.string.connect_ui_missed_yesterday,
            time,
        )
        findViewById<Button>(R.id.connect_ui_home_missed_call).apply {
            contentDescription = getString(R.string.connect_ui_call_description, missed.contact.name)
            setOnClickListener { CallActions.phoneCall(this@HomeActivity, missed.contact) }
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

    /** Toque longo de 3 segundos no relógio: evita que o idoso entre na configuração por engano. */
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
        CallActions.onPermissionResult(this, requestCode, grantResults)
        if (requestCode == REQ_PERMISSIONS) reload()
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
        override fun isEnabled(position: Int) = false // os toques vão para os botões, não para o cartão

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.connect_ui_tile, parent, false)
            val contact = items[position]
            // Largura do ecrã e não do pai: na primeira medição, o pai ainda pode ter largura 0.
            val size = resources.displayMetrics.widthPixels / maxOf(grid.numColumns, 1)
            view.findViewById<View>(R.id.connect_ui_tile_frame).layoutParams.height = (size * 0.75).toInt()
            view.findViewById<TextView>(R.id.connect_ui_tile_name).text = contact.name
            bindAvatar(
                this@HomeActivity, contact,
                view.findViewById<ImageView>(R.id.connect_ui_tile_photo),
                view.findViewById(R.id.connect_ui_tile_initials), size,
            )
            view.findViewById<Button>(R.id.connect_ui_tile_call).apply {
                val hasNumber = !contact.phoneNumber.isNullOrBlank()
                visibility = if (hasNumber) View.VISIBLE else View.GONE
                contentDescription = getString(R.string.connect_ui_call_description, contact.name)
                setOnClickListener { CallActions.phoneCall(this@HomeActivity, contact) }
            }
            view.findViewById<Button>(R.id.connect_ui_tile_video).apply {
                // Sem vídeo configurado, o botão fica visível mas esbatido: o cuidador sabe o que falta.
                alpha = if (CallActions.hasVideo(this@HomeActivity, contact)) 1f else 0.35f
                contentDescription = getString(R.string.connect_ui_video_description, contact.name)
                setOnClickListener { CallActions.videoCall(this@HomeActivity, contact) }
            }
            return view
        }
    }

    companion object {
        private const val REQ_PERMISSIONS = 61
        private const val REQ_DIALER = 62
        private const val CAREGIVER_HOLD_MS = 3000L

        /** Uma coluna por cada ~300 dp: 2 num telemóvel, 4 num tablet de 10″ deitado, até 5 em 15″. */
        internal fun columnsFor(widthDp: Int) = (widthDp / 300).coerceIn(2, 5)

        /** Fossify Phone completa (histórico, marcador, definições), através do filtro que a MainActivity já tem. */
        internal fun fullPhoneIntent(activity: Activity): Intent =
            Intent(Intent.ACTION_VIEW)
                .setType(CallLog.Calls.CONTENT_TYPE) // "vnd.android.cursor.dir/calls"
                .setPackage(activity.packageName)
    }
}
