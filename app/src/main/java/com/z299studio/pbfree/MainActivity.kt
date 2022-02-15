package com.z299studio.pbfree

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdRequest
import com.google.android.material.navigation.NavigationView
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.data.DataProcessor
import com.z299studio.pbfree.databinding.ActivityMainBinding
import com.z299studio.pbfree.tool.DriveSyncService
import com.z299studio.pbfree.tool.SyncStatus
import com.z299studio.pbfree.viewmodels.AuthSource
import com.z299studio.pbfree.viewmodels.AuthStatus
import com.z299studio.pbfree.viewmodels.AuthenticateViewModel
import com.z299studio.pbfree.viewmodels.BiometricViewModel
import com.z299studio.pbfree.viewmodels.HomeViewModel
import com.z299studio.pbfree.viewmodels.ItemViewModel
import com.z299studio.pbfree.viewmodels.SyncViewModel
import java.io.File
import java.text.DateFormat
import java.util.*
import javax.crypto.Cipher

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private var pausingTime = System.currentTimeMillis()
    private val itemViewModel: ItemViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val syncViewModel: SyncViewModel by viewModels()
    private val authViewModel: AuthenticateViewModel by viewModels()
    private val biometricViewModel: BiometricViewModel by viewModels()
    private val categoryNameIdMap = HashMap<String, Int>()
    private val signInLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        DriveSyncService.get().handleActivityResult(this, result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upgradeSettings()
        syncViewModel.filePath = File(this.filesDir, AccountRepository.FILE_NAME)
        if (BiometricViewModel.canBiometricAuth(this)) {
            biometricViewModel.init(this)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        binding.appBarMain.contentMain.fab.setOnClickListener {
            itemViewModel.reset2CategoryTemplate(homeViewModel.category.value)
            findNavController(R.id.nav_host).navigate(R.id.action_add_new)
        }
        // TODO: Move the AppBar to HomeFragment.
        drawerLayout = binding.drawerLayout
        navView = binding.navView
        navController = findNavController(R.id.nav_host)
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            hideSoftInput()
            when (destination.id) {
                R.id.nav_home -> {
                    if (arguments?.getBoolean("refresh_menu", false) == true) {
                        var id = 0
                        while (navView.menu.findItem(id) != null) {
                            navView.menu.removeItem(id++)
                        }
                        var validSelection = false
                        AccountRepository.get().getAllCategories().forEachIndexed { index, category ->
                            val menuItem = navView.menu.add(R.id.menu_main, index, index, category.name)
                            categoryNameIdMap[category.name] = index
                            menuItem.setIcon(getIcon(category.icon))
                            menuItem.isCheckable = true
                            if (category.name == homeViewModel.category.value) {
                                validSelection = true
                            }
                        }
                        if (!validSelection) {
                            homeViewModel.category.value = null
                        }
                    }
                    binding.appBarMain.toolbar.visibility = View.GONE
                    binding.appBarMain.contentMain.fab.show()
                    binding.appBarMain.contentMain.ad.visibility = View.VISIBLE
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                }
                in LOCK_DRAWER_FRAGMENTS -> {
                    binding.appBarMain.toolbar.visibility = View.VISIBLE
                    binding.appBarMain.contentMain.fab.hide()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    binding.appBarMain.contentMain.ad.visibility =
                        if (destination.id == R.id.nav_detail) { View.VISIBLE } else { View.GONE }
                }
                else -> {
                    binding.appBarMain.toolbar.visibility = View.GONE
                    binding.appBarMain.contentMain.fab.visibility = View.GONE
                    binding.appBarMain.contentMain.ad.visibility = View.GONE
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
            }
            if (destination.id == R.id.nav_detail) {
                binding.appBarMain.toolbar.title = arguments?.getString("title","")
            } else {
                binding.appBarMain.toolbar.title = null
            }
        }
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)
        val adRequest = AdRequest.Builder().build()
        binding.appBarMain.contentMain.ad.loadAd(adRequest)

        subscribeUi()
    }

    override fun onNavigationItemSelected(menu: MenuItem): Boolean {
        if (menu.itemId in setOf(R.id.nav_settings, R.id.nav_category)) {
            return NavigationUI.onNavDestinationSelected(menu, navController)
        }
        navView.setCheckedItem(menu.itemId)
        drawerLayout.closeDrawer(GravityCompat.START)
        homeViewModel.category.value = if (menu.itemId == R.id.nav_home) { null } else {menu.title.toString()}
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onPause() {
        super.onPause()
        pausingTime = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        if (navController.currentDestination?.id != R.id.nav_login) {
            if (ignoreNextPause) {
                ignoreNextPause = false
                return
            }
            val lockTime = Integer.parseInt(
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(getString(R.string.pref_lock_time), null) ?: "300000"
            )
            if (lockTime > 0 && System.currentTimeMillis() - pausingTime > lockTime) {
                startActivity(Intent(this, MainActivity::class.java))
                this.finish()
//                val navOptionsBuilder = NavOptions.Builder()
//                navOptionsBuilder.setPopUpTo(R.id.nav_home, true).setLaunchSingleTop(true)
//                val args = Bundle()
//                args.putBoolean("auth_only", true)
//                navController.navigate(R.id.nav_login, args, navOptionsBuilder.build())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.appBarMain.contentMain.ad.destroy()
    }

    fun openDrawer() {
        hideSoftInput()
        drawerLayout.openDrawer(GravityCompat.START)
    }

    fun checkSelectedDrawer() {
        navView.setCheckedItem(categoryNameIdMap[homeViewModel.category.value] ?: R.id.nav_home)
    }

    fun startSync() {
        val defaultPref = PreferenceManager.getDefaultSharedPreferences(this)
        if (defaultPref.getBoolean(getString(R.string.key_sync), false)) {
            DriveSyncService.get().create(this, getString(R.string.app_name),
                syncViewModel)?.let {
                signInLauncher.launch(it)
            }
        }
    }

    private fun upgradeSettings() {
        val defaultPref = PreferenceManager.getDefaultSharedPreferences(this)
        val fpPref = getSharedPreferences(BIO_PREF, Context.MODE_PRIVATE)
        if (defaultPref.contains(getString(R.string.lock_time_v2))) {
            val lockTime = defaultPref.getInt(getString(R.string.lock_time_v2), 300000)
            defaultPref.edit().putString(getString(R.string.pref_lock_time), lockTime.toString())
                .remove(getString(R.string.lock_time_v2)).apply()
        }
        if (defaultPref.contains(getString(R.string.sync_v2))) {
            val syncEnabled = defaultPref.getInt(getString(R.string.sync_v2), 0) != 0 //==2
            defaultPref.edit().putBoolean(getString(R.string.key_sync), syncEnabled)
                .remove(getString(R.string.sync_v2)).apply()
        }
        if (fpPref.contains(getString(R.string.biometric_v2))) {
            val bioStatus = fpPref.getInt(getString(R.string.biometric_v2), 0)
            fpPref.edit().putBoolean(getString(R.string.pref_biometric), bioStatus == 3)
                .remove(getString(R.string.biometric_v2)).apply()
        }
    }

    private fun hideSoftInput() {
        val currentFocusedView = this.currentFocus
        currentFocusedView?.let {
            (this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(currentFocusedView.windowToken, 0)
        }
    }

    private fun subscribeUi() {
        authViewModel.status.observe(this) {
            when (it) {
                AuthStatus.Canceled -> {
                    syncViewModel.send()
                }
                AuthStatus.Confirmed -> {
                    syncViewModel.content?.let { content ->
                        try {
                            decryptAndSave(authViewModel.password, content)
                        } catch (error: Exception) {
                            authViewModel.status.value = AuthStatus.Rejected
                        }
                    }
                }
                else -> {}
            }
        }
        syncViewModel.status.observe(this) { status ->
            when(status) {
                SyncStatus.Preparing -> {
                    signInLauncher.launch(syncViewModel.intent)
                }
                SyncStatus.Resolving -> {
                    AlertDialog.Builder(this).setTitle(R.string.sync_title)
                        .setMessage(R.string.sync_resolve)
                        .setPositiveButton(R.string.keep_local) { _, _ ->
                            syncViewModel.send()
                        }
                        .setNegativeButton(R.string.keep_cloud) { _, _ ->
                            resolveAcceptedRemoteData()
                        }
                        .show()
                }
                SyncStatus.Done -> {
                    resolveAcceptedRemoteData()
                }
                else -> {}
            }
        }

        biometricViewModel.status.observe(this) { status ->
            if (biometricViewModel.mode != Cipher.ENCRYPT_MODE) {
                return@observe
            }
            if (status == AuthStatus.Accepted) {
                val pref = getSharedPreferences(BIO_PREF, MODE_PRIVATE)
                biometricViewModel.cryptoObject?.apply {
                    val encrypted = this.cipher?.doFinal(AccountRepository.get().password.toByteArray())
                    pref.edit()
                        .putBoolean(getString(R.string.pref_biometric), true)
                        .putString(getString(R.string.bio_iv), Base64.encodeToString(this.cipher?.iv, Base64.DEFAULT))
                        .putString(getString(R.string.bio_data), Base64.encodeToString(encrypted, Base64.DEFAULT))
                        .apply()
                }
                biometricViewModel.finish()
            } else if (status == AuthStatus.Canceled) {
                val pref = getSharedPreferences(BIO_PREF, MODE_PRIVATE)
                pref.edit().putBoolean(getString(R.string.pref_biometric), false).apply()
                biometricViewModel.finish()
            }
        }
    }

    private fun decryptAndSave(password: String, data: ByteArray) {
        val result = DataProcessor.getDataParser(data).parse(password, data)
        AccountRepository.get().setData(result)
        if (AccountRepository.get().password.isEmpty()) {
            AccountRepository.get().password = password
        }
        AccountRepository.get().saveData(this)
        authViewModel.status.value = AuthStatus.Accepted
    }

    private fun resolveAcceptedRemoteData() {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.edit().putString(getString(R.string.key_sync_time),
            DateFormat.getDateTimeInstance().format(Date())).apply()
        if (syncViewModel.content?.isNotEmpty() == true) {
            if (!syncViewModel.hasLocalData) {
                AccountRepository.get().saveData(this, syncViewModel.content!!)
                return
            }
            try {
                decryptAndSave(AccountRepository.get().password, syncViewModel.content!!)
            } catch (error: Exception) {
                authViewModel.source = AuthSource.Sync
                authViewModel.password = ""
                authViewModel.status.value = AuthStatus.Ready
                navController.navigate(R.id.authenticateDialog)
            }
        }
    }

    companion object {
        const val BIO_PREF = "pbfp"
        const val DEFAULT_ICON = -1
        var ignoreNextPause = false
        val LOCK_DRAWER_FRAGMENTS = setOf(R.id.nav_detail, R.id.nav_edit, R.id.nav_category, R.id.nav_settings)

        val ICONS = arrayOf(R.drawable.bank,
            R.drawable.creditcard, R.drawable.desktop, R.drawable.shop,
            R.drawable.email, R.drawable.web, R.drawable.wallet,
            R.drawable.atm, R.drawable.bag, R.drawable.gift,
            R.drawable.school, R.drawable.folder,R.drawable.work,
            R.drawable.chat, R.drawable.lock, R.drawable.account)

        @DrawableRes
        fun getIcon(index: Int): Int {
            return if (index < 0 || index >= ICONS.size) {
                return R.drawable.label
            } else {
                ICONS[index]
            }
        }
    }
}